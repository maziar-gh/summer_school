package com.luseen.yandexsummerschool.data;

import com.luseen.yandexsummerschool.data.api.Api;
import com.luseen.yandexsummerschool.data.api.ApiInterface;
import com.luseen.yandexsummerschool.model.AvailableLanguages;
import com.luseen.yandexsummerschool.model.Translation;

import rx.Observable;

/**
 * Created by Chatikyan on 19.03.2017.
 */

public class DataManager implements ApiInterface {

    private ApiInterface apiInterface = Api.getInstance().getApiService();

    @Override
    public Observable<Translation> translation(String key, String text, String lang) {
        return apiInterface.translation(key, text, lang);
    }

    @Override
    public Observable<AvailableLanguages> availableLanguages(String key, String lang) {
        return apiInterface.availableLanguages(key, lang);
    }
}
