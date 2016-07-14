package com.ladwa.aditya.twitone.mainscreen;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;

import com.ladwa.aditya.twitone.R;
import com.ladwa.aditya.twitone.TwitoneApp;
import com.ladwa.aditya.twitone.data.TwitterRepository;
import com.ladwa.aditya.twitone.data.local.TwitterLocalDataStore;
import com.ladwa.aditya.twitone.login.LoginActivity;
import com.marshalchen.ultimaterecyclerview.UltimateRecyclerView;
import com.squareup.leakcanary.RefWatcher;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import twitter4j.Twitter;
import twitter4j.auth.AccessToken;


/**
 * This is the Main Fragment that users will see once they open the app
 * A placeholder fragment containing a simple view.
 */
public class MainScreenFragment extends Fragment implements MainScreenContract.View {

    @Inject
    SharedPreferences preferences;


    @Inject
    Twitter mTwitter;

    @Inject
    TwitterRepository repository;

    @BindView(R.id.recyclerview_timeline)
    UltimateRecyclerView recyclerView;

    private boolean mLogin;
    private Unbinder unbinder;
    private MainScreenContract.Presenter mPresenter;
    private DrawerCallback mDrawerCallback;

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
    public void setPresenter(MainScreenContract.Presenter presenter) {
        mPresenter = presenter;
    }


    public interface DrawerCallback {
        void setProfile(String screenName);

        void updateProfile(com.ladwa.aditya.twitone.data.local.models.User user);
    }
}
