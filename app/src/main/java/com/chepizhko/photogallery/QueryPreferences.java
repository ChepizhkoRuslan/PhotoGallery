package com.chepizhko.photogallery;

import android.content.Context;
import android.preference.PreferenceManager;

public class QueryPreferences {
    // Значение PREF_SEARCH_QUERY используется в качестве ключа для хранения запроса.
    private static final String PREF_SEARCH_QUERY = "searchQuery";
    // Константа для хранения идентификатора последней загруженной фотографии
    private static final String PREF_LAST_RESULT_ID = "lastResultId";
    // сигнал находиться во включенном или в отключенном состоянии
    private static final String PREF_IS_ALARM_ON = "isAlarmOn";
    // Метод getStoredQuery(Context) возвращает значение запроса
    // класс QueryPreferences не имеет собственного контекста,
    // вызывающий компонент должен передать свой контекст как входной параметр
    public static String getStoredQuery(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_SEARCH_QUERY, null);
    }
    // Метод setStoredQuery(Context) записывает запрос в хранилище общих настроек для заданного контекста
    public static void setStoredQuery(Context context, String query) {
        PreferenceManager.getDefaultSharedPreferences(context)
                // вызов SharedPreferences.edit() используется для получения экземпляра SharedPreferences.Editor.
                // Этот класс используется для сохранения значений в SharedPreferences.
                // Множественные изменения могут быть сгруппированы в одну операцию записи в хранилище
                .edit()
                .putString(PREF_SEARCH_QUERY, query)
                // Метод apply() вносит изменения в память немедленно,
                // а непосредственная запись в файл осуществляется в фоновом потоке
                .apply();
    }

    public static String getLastResultId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_LAST_RESULT_ID, null);
    }
    public static void setLastResultId(Context context, String lastResultId) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_LAST_RESULT_ID, lastResultId)
                .apply();
    }

    public static boolean isAlarmOn(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_IS_ALARM_ON, false);
    }
    public static void setAlarmOn(Context context, boolean isOn) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_IS_ALARM_ON, isOn)
                .apply();
    }
}