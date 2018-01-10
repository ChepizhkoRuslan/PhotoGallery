package com.chepizhko.photogallery;

import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FlickrFetchr {
    private static final String TAG = "FlickrFetcher";
    private static final String API_KEY = "d52b7e32b139061b2443eb37531062fb";
    // Метод getUrlBytes(String) получает низкоуровневые данные по URL и возвращает их в виде массива байтов
    public byte[] getUrlBytes(String urlSpec) throws IOException {
        // Этот код создает объект URL на базе строки адреса например www. google.com
        URL url = new URL(urlSpec);
        // вызов метода openConnection() создает объект подключения к заданному URL-адресу
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                        ": with " +
                        urlSpec);
            }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            // После создания объекта URL и открытия подключения программа многократно вызывает read(),
            // пока в подключении не кончатся данные.
            // Объект InputStream(in) предоставляет байты по мере их доступности
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            // Когда чтение будет завершено, программа закрывает его и выдает массив байтов из ByteArrayOutputStream
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }
    // Метод getUrlString(String) преобразует результат из getUrlBytes(String) в String
    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }
    // Метод, который строит соответствующий URL-адрес запроса и загружает его содержимое
    public void fetchItems() {
        try {
            // класс Uri.Builder для построения полного URL-адреса для API-запроса к Flickr.
            // Uri.Builder — вспомогательный класс для создания параметризованных URL-адресов
            // с правильным кодированием символов
            // https://api.flickr.com/services/rest/?method=flickr.photos.getRecent&api_key=xxx&format=json&nojsoncallback=1&extras=url_s
            String url = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    // Метод Uri.Builder.appendQueryParameter(String,String) автоматически кодирует строки запросов
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    // Значение url_s приказывает Flickr включить URL-адрес для уменьшенной версии изображения, если оно доступно
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        }
    }
}
