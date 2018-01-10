package com.chepizhko.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FlickrFetchr {
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
}
