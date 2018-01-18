package com.chepizhko.photogallery;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private ConcurrentMap<T,String> mRequestMap = new ConcurrentHashMap<>();
    // переменная mResponseHandler для хранения экземпляра Handler, переданного из главного потока
    private Handler mResponseHandler;

    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    // интерфейс слушателя для передачи ответов (загруженных изображений) запрашивающей стороне (главному потоку)
    public interface ThumbnailDownloadListener<T> {
        // Метод будет вызван через некоторое время, когда изображение было полностью загружено
        // и готово к добавлению в пользовательский интерфейс
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }
    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    // конструктор, который получает Handler и задает переменную
    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }
    // Метод HandlerThread.onLooperPrepared() вызывается до того, как Looper впервые проверит очередь,
    // поэтому он хорошо подходит для создания реализации Handler
    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            // в handleMessage() мы проверяем тип сообщения, читаем значение obj
            // (которое имеет тип T и служит идентификатором для запроса) и передаем его handleRequest(…).
            // Handler.handleMessage(…) будет вызываться, когда сообщение загрузки извлечено из очереди и готово к обработке
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }
    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }
    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL: " + url);
        // код обновления mRequestMap и постановки нового сообщения в очередь сообщений фонового потока
        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            // в само сообщение URL-адрес не входит. Вместо этого mRequestMap обновляется связью
            // между идентификатором запроса (PhotoHolder) и URL-адресом запроса.
            // Позднее мы получим URL из mRequestMap, чтобы гарантировать, что для заданного экземпляра PhotoHolder
            // всегда загружается последний из запрашивавшихся URL-адресов
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }
    // метод для удаления всех запросов из очереди
    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }
    // Вся загрузка осуществляется в методе handleRequest(). ,
    // после чего передаем его новому экземпляру знакомого класса FlickrFetchr.
    // При этом используется метод FlickrFetchr.getUrlBytes()
    private void handleRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);
            // Мы проверяем существование URL-адреса
            if (url == null) {
                return;
            }
            // передаем URL-адрес новому экземпляру знакомого класса FlickrFetchr.
            // При этом используется метод FlickrFetchr.getUrlBytes()
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            // класс BitmapFactory для построения растрового изображения из массива байтов, возвращенным getUrlBytes()
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");

            mResponseHandler.post(new Runnable() {
                public void run() {
                    // mRequestMap.get(target) != url = Эта проверка гарантирует, что каждый объект PhotoHolder получит
                    // правильное изображение, даже если за прошедшее время был сделан другой запрос
                    // mHasQuit - Если выполнение ThumbnailDownloader уже завершилось, выполнение каких-либо обратных вызовов небезопасно
                    if (mRequestMap.get(target) != url || mHasQuit) {
                        return;
                    }
                    // мы удаляем из requestMap связь «PhotoHolder—URL»
                    mRequestMap.remove(target);
                    // назначаем изображение для PhotoHolder
                    mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                }
            });

        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }
}
