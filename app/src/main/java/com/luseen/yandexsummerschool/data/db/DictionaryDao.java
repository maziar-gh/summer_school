package com.luseen.yandexsummerschool.data.db;


import com.luseen.yandexsummerschool.model.dictionary.Definition;
import com.luseen.yandexsummerschool.model.dictionary.Dictionary;
import com.luseen.yandexsummerschool.model.dictionary.DictionaryTranslation;
import com.luseen.yandexsummerschool.utils.RealmUtils;

import java.util.List;

import io.realm.RealmResults;
import rx.Observable;

public class DictionaryDao extends AbstractDao {

    private static DictionaryDao instance = null;

    public static DictionaryDao getInstance() {
        if (instance == null) {
            instance = new DictionaryDao();
        }
        return instance;
    }

    public void saveObject(Dictionary dictionary) {
        realm.beginTransaction();
        int definitionId = RealmUtils.generateId(realm, Definition.class);
        int dictionaryTranslationId = RealmUtils.generateId(realm, DictionaryTranslation.class);
        for (Definition definition : dictionary.getDefinition()) {
            definition.setId(++definitionId);
            for (DictionaryTranslation dictionaryTranslation : definition.getTranslations()) {
                dictionaryTranslation.setId(++dictionaryTranslationId);
            }
        }
        realm.copyToRealmOrUpdate(dictionary);
        realm.commitTransaction();
    }

    public void saveList(List<Dictionary> dictionaries) {
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(dictionaries);
        realm.commitTransaction();
    }

    public Dictionary getDictionaryByWord(String word) {
        return realm
                .where(Dictionary.class)
                .equalTo(Dictionary.ORIGINAL_TEXT, word.toLowerCase())
                .findFirst();
    }

    public Observable<RealmResults<Dictionary>> getDictionaryList() {
        return realm
                .where(Dictionary.class)
                .findAllAsync().asObservable();
    }

    public RealmResults<Dictionary> getDictionaryRealmResult() {
        return realm
                .where(Dictionary.class)
                .findAll();
    }

    public Dictionary getDictionary() {
        return restore(Dictionary.class);
    }
}
