package com.chepizhko.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

//   Эта служба будет использоваться нами для опроса результатов поиска

public class PollService extends IntentService {
    private static final String TAG = "PollService";
    // 60 секунд
    private static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    public static void setServiceAlarm(Context context, boolean isOn) {
        // Метод начинается с создания объекта PendingIntent, который запускает PollService
        // Задача решается вызовом метода PendingIntent.getService(…),
        // в котором упаковывается вызов Context.startService(Intent).
        Intent intent = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, intent, 0);
        // AlarmManager — системная служба, которая может отправлять интенты за вас
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (isOn) {
            // Чтобы установить сигнал, следует вызвать AlarmManager.setRepeating(…).
            // Этот метод тоже получает четыре параметра: константу для описания временной базы сигнала,
            // время запуска сигнала, временной интервал повторения сигнала, и наконец, объект PendingIntent,
            // запускаемый при срабатывании сигнала
            // Использование константы AlarmManager.ELAPSED_REALTIME задает начальное время запуска
            // относительно прошедшего реального времени: SystemClock.elapsedRealtime().
            // В результате сигнал срабатывает по истечении заданного промежутка времени.
            // Если бы вместо этого использовалась константа AlarmManager.RTC,
            // то начальное время определялось бы текущим временем (то есть System.currentTimeMillis()),
            // а сигнал срабатывал в заданный фиксированный момент времени.
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL_MS, pi);
        } else {
            // Отмена сигнала осуществляется вызовом AlarmManager.cancel(PendingIntent).
            alarmManager.cancel(pi);
            // Обычно при этом также следует отменить и PendingIntent
            pi.cancel();
        }
    }
    // метод isServiceAlarmOn(Context), использующий флаг PendingIntent.FLAG_NO_CREATE для проверки сигнала
    public static boolean isServiceAlarmOn(Context context) {
        Intent intent = PollService.newIntent(context);
        // вы можете проверить, существует ли PendingIntent, чтобы узнать, активен сигнал или нет.
        // Эта операция выполняется передачей флага PendingIntent.FLAG_NO_CREATE вызову PendingIntent.getService(…).
        // Флаг говорит, что если объект PendingIntent не существует, то вместо его создания следует вернуть null.
        PendingIntent pi = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
        return pi != null;
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

            // Чтобы служба PollService оповещала пользователя о появлении нового результата, добавьте код
            // Этот код создает объект Notification и вызывает NotificationManager.notify(int, Notification).
            Resources resources = getResources();
            // создаём PendingIntent
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
            // создаём Notification
            Notification notification = new NotificationCompat.Builder(this)
                    // задаем текст бегущей строки
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    // задаем текст бегущей значка
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    // Заголовок
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    // текст
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    // Теперь необходимо указать, что происходит при нажатии на оповещении.
                    // Как и в случае с AlarmManager, для этого используется PendingIntent.
                    // Объект PendingIntent, передаваемый setContentIntent(PendingIntent),
                    // будет запускаться при нажатии пользователем на вашем оповещении на выдвижной панели.
                    .setContentIntent(pi)
                    // Вызов setAutoCancel(true) слегка изменяет это поведение: с этим вызовом
                    // оповещение при нажатии также будет удаляться с выдвижной панели оповещений.
                    .setAutoCancel(true)
                    .build();
            // После того как объект Notification будет создан,
            // его можно отправить вызовом метода notify(int, Notification)
            // для системной службы NotificationManager
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            // Передаваемый целочисленный параметр содержит идентификатор оповещения, уникальный в границах приложения.
            // Если вы отправите второе оповещение с тем же идентификатором, оно заменит последнее оповещение, с тем же id
            // Так реализуются индикаторы прогресса и другие динамические визуальные эффекты
            notificationManager.notify(0, notification);
        }

        QueryPreferences.setLastResultId(this, resultId);
    }
    // необходимо при помощи объекта ConnectivityManager убедиться в том, что сеть доступна
    // и есть разрешение на фоновые операции
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
