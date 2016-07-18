package com.ladwa.aditya.twitone.mainscreen;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.Toast;

import com.ladwa.aditya.twitone.R;
import com.ladwa.aditya.twitone.TwitoneApp;
import com.ladwa.aditya.twitone.adapter.TimelineAdapter;
import com.ladwa.aditya.twitone.data.TwitterRepository;
import com.ladwa.aditya.twitone.data.local.TwitterLocalDataStore;
import com.ladwa.aditya.twitone.data.local.models.Tweet;
import com.ladwa.aditya.twitone.login.LoginActivity;
import com.squareup.leakcanary.RefWatcher;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;
import twitter4j.Twitter;
import twitter4j.auth.AccessToken;


/**
 * This is the Main Fragment that users will see once they open the app
 * A placeholder fragment containing a simple view.
 */
public class MainScreenFragment extends Fragment implements MainScreenContract.View, SwipeRefreshLayout.OnRefreshListener {

    @Inject
    SharedPreferences preferences;
    @Inject
    Twitter mTwitter;
    @Inject
    TwitterRepository repository;

    @BindView(R.id.recyclerview_timeline)
    RecyclerView recyclerView;

    @BindView(R.id.swipeContainer)
    SwipeRefreshLayout swipeContainer;

    private boolean mLogin;
    private Unbinder unbinder;
    private MainScreenContract.Presenter mPresenter;
    private DrawerCallback mDrawerCallback;
    private LinearLayoutManager linearLayoutManager;
    private TimelineAdapter mTimelineAdapter;
    private SharedPreferences.Editor editor;
    private int finalPos;

    private List<Tweet> tweets;

    public MainScreenFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_screen, container, false);
        unbinder = ButterKnife.bind(this, view);
        TwitoneApp.getTwitterComponent().inject(this);


        mLogin = preferences.getBoolean(getString(R.string.pref_login), false);
        long id = preferences.getLong(getString(R.string.pref_userid), 0);
        String token = preferences.getString(getString(R.string.pref_access_token), "");
        String secret = preferences.getString(getString(R.string.pref_access_secret), "");
        AccessToken accessToken = new AccessToken(token, secret);
        mTwitter.setOAuthAccessToken(accessToken);
        new MainScreenPresenter(this, mLogin, id, mTwitter, repository);

        TwitterLocalDataStore.getInstance(getActivity());

        //Recycler view
        linearLayoutManager = new LinearLayoutManager(getActivity());
        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setAddDuration(1000);
        itemAnimator.setRemoveDuration(1000);
        recyclerView.setItemAnimator(itemAnimator);
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    editor = preferences.edit();
                    editor.putInt("Scroll_pos", linearLayoutManager.findFirstVisibleItemPosition());
                    editor.apply();
                }
            }
        });

        tweets = new ArrayList<>();
        mTimelineAdapter = new TimelineAdapter(tweets, getActivity());
        recyclerView.setAdapter(mTimelineAdapter);
        swipeContainer.setOnRefreshListener(this);


        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mDrawerCallback = (DrawerCallback) context;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //SetupDrawer
        String screenName = preferences.getString(getString(R.string.pref_screen_name), "");
        mDrawerCallback.setProfile(screenName);

    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.subscribe();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPresenter.unsubscribe();
        editor = preferences.edit();
        editor.putInt("Scroll_pos", linearLayoutManager.findFirstVisibleItemPosition());
        editor.apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = TwitoneApp.getRefWatcher();
        refWatcher.watch(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void logout() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(getString(R.string.pref_login), false);
        editor.apply();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //noinspection deprecation
            CookieManager.getInstance().removeAllCookie();
        } else {
            CookieManager.getInstance().removeAllCookies(null);
        }
        mTwitter.setOAuthAccessToken(null);
        startActivity(new Intent(getActivity(), LoginActivity.class));
        getActivity().finish();
    }

    @Override
    public void loadedUser(com.ladwa.aditya.twitone.data.local.models.User user) {
        mDrawerCallback.updateProfile(user);
    }

    @Override
    public void loadTimeline(List<Tweet> tweetList) {
        int oldSize = tweets.size();
        int newSize = tweetList.size();
        int saveScrollPos = preferences.getInt("Scroll_pos", 0);


        if (saveScrollPos > 0) {
            finalPos = saveScrollPos;
        } else {
            finalPos = newSize - oldSize;
        }

        Timber.d("Final pos = " + String.valueOf(finalPos));
        tweets.clear();
        tweets.addAll(tweetList);
        mTimelineAdapter.notifyDataSetChanged();
        setScrollPos();
    }

    @Override
    public void setScrollPos() {
        linearLayoutManager.scrollToPosition(finalPos);
    }

    @Override
    public void stopRefreshing() {
        swipeContainer.setRefreshing(false);
    }

    @Override
    public void showError() {
        Toast.makeText(getActivity(), "An error occurred", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void setPresenter(MainScreenContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void onRefresh() {
        mPresenter.refreshRemoteTimeline();

    }


    public interface DrawerCallback {
        void setProfile(String screenName);

        void updateProfile(com.ladwa.aditya.twitone.data.local.models.User user);
    }
}
