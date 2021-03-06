package com.luseen.yandexsummerschool.data.db;


import com.luseen.yandexsummerschool.model.History;
import com.luseen.yandexsummerschool.model.LanguagePair;
import com.luseen.yandexsummerschool.model.dictionary.Definition;
import com.luseen.yandexsummerschool.model.dictionary.Dictionary;
import com.luseen.yandexsummerschool.model.dictionary.DictionaryTranslation;
import com.luseen.yandexsummerschool.model.dictionary.Example;
import com.luseen.yandexsummerschool.model.dictionary.Synonym;
import com.luseen.yandexsummerschool.model.dictionary.TranslatedString;
import com.luseen.yandexsummerschool.utils.ExceptionTracker;
import com.luseen.yandexsummerschool.utils.RealmUtils;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import rx.Completable;
import rx.Emitter;
import rx.Observable;

public class HistoryDao {

    private static HistoryDao instance = null;

    public static HistoryDao getInstance() {
        if (instance == null) {
            instance = new HistoryDao();
        }
        return instance;
    }

    /**
     * Saving given history
     *
     * @param history given history
     * @return return result ok, or error result
     */
    public Completable saveHistory(History history) {
        return Completable.fromEmitter(emitter -> {
            Realm realm = Realm.getDefaultInstance();
            realm.executeTransaction(r -> {
                int uniqueId = RealmUtils.generateId(r, Definition.class);
                Dictionary dictionary = history.getDictionary();
                dictionary.setIdentifier(dictionary.getOriginalText() + dictionary.getTranslatedText());
                LanguagePair pair = history.getLanguagePair();
                String pairId = pair.getSourceLanguage().getLangCode() + pair.getTargetLanguage().getLangCode();
                LanguagePair historyLanguagePair = createLanguagePair(pair);
                String historyIdentifier = dictionary.getOriginalText() + pairId;
                history.setLanguagePair(historyLanguagePair);
                history.setIdentifier(historyIdentifier);
                int orderId = RealmUtils.generateOrderId(r, History.class);
                history.setOrderId(orderId);
                for (Definition definition : dictionary.getDefinition()) {
                    definition.setId(++uniqueId);
                }
                r.copyToRealmOrUpdate(history);
                emitter.onCompleted();
            });
            realm.close();
        });
    }

    /**
     * Setting favourite to given history
     *
     * @param history given history
     * @return history or error
     */
    public Observable<History> setFavourite(History history) {
        return Observable.fromEmitter(historyEmitter -> {
            Realm realm = Realm.getDefaultInstance();
            realm.executeTransaction(realm1 -> {
                try {
                    history.setFavourite(!history.isFavourite());
                    historyEmitter.onNext(history);
                    historyEmitter.onCompleted();
                } catch (Exception e) {
                    historyEmitter.onError(e);
                }
            });
            realm.close();
        }, Emitter.BackpressureMode.BUFFER);
    }

    /**
     * Create new language pair, from given pair, for making it mutable
     *
     * @param languagePair given pair
     * @return mutable pair
     */
    private LanguagePair createLanguagePair(LanguagePair languagePair) {
        String pairId = languagePair.getSourceLanguage().getLangCode()
                + languagePair.getTargetLanguage().getLangCode();
        LanguagePair historyLanguagePair = new LanguagePair();
        historyLanguagePair.setId(pairId);
        historyLanguagePair.setSourceLanguage(languagePair.getSourceLanguage());
        historyLanguagePair.setTargetLanguage(languagePair.getTargetLanguage());
        return historyLanguagePair;
    }

    /**
     * This method give us matched key word result from favourite
     *
     * @param keyWord given key word
     * @return Found results
     */
    public RealmResults<History> getFavouritesByKeyWord(String keyWord) {
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<History> realmQuery = getBaseQuery(keyWord)
                .equalTo(History.IS_FAVOURITE, true);
        RealmResults<History> favourites = realmQuery.findAllSorted(History.ORDER_ID, Sort.DESCENDING);
        realm.close();
        return favourites;
    }

    /**
     * This method give us matched key word result from history
     *
     * @param keyWord given key word
     * @return Found results
     */
    public RealmResults<History> getHistoriesByKeyWord(String keyWord) {
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<History> realmQuery = getBaseQuery(keyWord);
        RealmResults<History> histories = realmQuery.findAllSorted(History.ORDER_ID, Sort.DESCENDING);
        realm.close();
        return histories;
    }

    /**
     * This method give us history result by given id
     *
     * @param identifier given id
     * @return found history
     */
    public Observable<History> getHistoryByIdentifier(String identifier) {
        Realm realm = Realm.getDefaultInstance();
        Observable<History> history = realm
                .where(History.class)
                .equalTo(History.IDENTIFIER, identifier)
                .findFirstAsync()
                .asObservable()
                .filter(realmObject -> realmObject.isLoaded())
                .first()
                .map(realmObject -> ((History) realmObject));
        realm.close();
        return history;
    }

    /**
     * Getting all history list
     * @return history list
     */
    public Observable<RealmResults<History>> getHistoryList() {
        Realm realm = Realm.getDefaultInstance();
        Observable<RealmResults<History>> histories = realm
                .where(History.class)
                .findAllSortedAsync(History.ORDER_ID, Sort.DESCENDING)
                .asObservable()
                .filter(RealmResults::isLoaded)// isLoaded is true when query is completed
                .first(); // Only get the first result and then complete
        realm.close();
        return histories;
    }

    /**
     * Getting all favourite list
     * @return favourite list
     */
    public Observable<RealmResults<History>> getFavouriteList() {
        Realm realm = Realm.getDefaultInstance();
        Observable<RealmResults<History>> favourites = realm
                .where(History.class)
                .equalTo(History.IS_FAVOURITE, true)
                .findAllSortedAsync(History.ORDER_ID, Sort.DESCENDING)
                .asObservable()
                .filter(RealmResults::isLoaded)// isLoaded is true when query is completed
                .first(); // Only get the first result and then complete
        realm.close();
        return favourites;
    }

    /**
     * Getting saved history list size
     * @return integer count
     */
    public int getHistoryListSize() {
        Realm realm = Realm.getDefaultInstance();
        long count = realm
                .where(History.class)
                .count();
        realm.close();
        return (int) count;
    }

    /**
     * Delete all saved objects from DB
     */
    public void clearHistoryAndFavouriteData() {
        try {
            Realm realm = Realm.getDefaultInstance();
            deleteRealmResult(realm, History.class);
            deleteRealmResult(realm, Definition.class);
            deleteRealmResult(realm, DictionaryTranslation.class);
            deleteRealmResult(realm, Synonym.class);
            deleteRealmResult(realm, TranslatedString.class);
            deleteRealmResult(realm, Example.class);
            realm.close();
        } catch (Exception exception) {
            ExceptionTracker.trackException(exception);
        }
    }

    private <R extends RealmObject> void deleteRealmResult(Realm realm, Class<R> clazz) {
        RealmResults<R> realmResults = realm.where(clazz).findAll();
        realm.executeTransaction(realm1 -> realmResults.deleteAllFromRealm());
    }

    private RealmQuery<History> getBaseQuery(String keyWord) {
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<History> realmQuery = realm.where(History.class)
                .beginGroup()
                .contains(History.ORIGINAL_TEXT, keyWord, Case.INSENSITIVE)
                .or()
                .contains(History.TRANSLATED_TEXT, keyWord, Case.INSENSITIVE)
                .endGroup();
        realm.close();
        return realmQuery;
    }
}
