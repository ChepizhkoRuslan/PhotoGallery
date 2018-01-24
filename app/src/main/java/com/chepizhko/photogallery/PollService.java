package com.chepizhko.photogallery;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import java.util.List;

//   Эта служба будет использоваться нами для опроса результатов поиска

public class PollService extends IntentService {
    private static final String TAG = "PollService";

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    public PollService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Если приложение не воспринимает сеть как доступную или же устройство не полностью подключено к сети,
        // onHandleIntent(…) возвращает управление без выполнения оставшейся части метода
        if (!isNetworkAvailableAndConnected()) {
            return;
        }
//        Необходимо сделать следующее:
//        1. Прочитать текущий запрос и идентификатор последнего результата из SharedPreferences по умолчанию.
//        2. Загрузить последний набор результатов с использованием FlickrFetchr.
//        3. Если набор не пуст, получить первый результат.
//        4. Проверить, отличается ли его идентификатор от идентификатора последнего результата.
//        5. Сохранить первый результат в SharedPreferences
        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getLastResultId(this);

        List<GalleryItem> items;
        if (query == null) {
            items = new FlickrFetchr().fetchRecentPhotos();
        } else {
            items = new FlickrFetchr().searchPhotos(query);
        }
        if (items.size() == 0) {
            return;
        }

        String resultId = items.get(0).getId();
        if (resultId.equals(lastResultId)) {
            Log.i(TAG, "Got an old result: " + resultId);
        } else {
            Log.i(TAG, "Got a new result: " + resultId);
        }

        QueryPreferences.setLastResultId(this, resultId);
    }
    // необходимо при помощи объекта ConnectivityManager убедиться в том, что сеть доступна
    private boolean isNetworkAvailableAndConnected() {

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        // Запрет фоновой загрузки данных полностью блокирует возможность использования сети фоновыми службами
        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        // Если сеть доступна для вашей фоновой службы, она получает экземпляр android.net.NetworkInfo,
        // представляющий текущее сетевое подключение.
        // Затем код проверяет наличие полноценного сетевого подключения, вызывая NetworkInfo.isConnected().
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();

        return isNetworkConnected;
    }
}
