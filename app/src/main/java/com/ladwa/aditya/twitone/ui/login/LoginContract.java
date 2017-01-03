package com.ladwa.aditya.twitone.ui.login;

import com.ladwa.aditya.twitone.BasePresenter;
import com.ladwa.aditya.twitone.BaseView;

import rx.Observable;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 *
 * Created by Aditya on 24-Jun-16.
 */
public interface LoginContract {
    interface View extends BaseView<Presenter> {
        void onError(String errorMessage);

        void onSuccess(String message);

        void startOauthIntent(RequestToken requestToken);

        void saveAccessTokenAnd(AccessToken accessToken);

    }

    interface Presenter extends BasePresenter {
        void login();

        void getAccessToken(String verifier);

        Observable<RequestToken> getRequestTokenObservable();

        Observable<AccessToken> getAccessTokenObservable(final String verifier);
    }
}