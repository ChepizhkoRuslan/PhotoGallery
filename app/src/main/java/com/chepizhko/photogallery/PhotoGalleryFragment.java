package com.chepizhko.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {
    private RecyclerView mPhotoRecyclerView;
    private static final String TAG = "PhotoGalleryFragment";
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // удержание фрагмента, чтобы поворот не приводил к многократному порождению новых объектов AsyncTask
        setRetainInstance(true);
        // вызов execute() для нового экземпляра FetchItemsTask
        new FetchItemsTask().execute();

        // передаём классу ThumbnailDownloader объект Handler, присоединенный к главному потоку
        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        // ThumbnailDownloadListener для обработки загруженного изображения после завершения загрузки
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {

                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        photoHolder.bindDrawable(drawable);
                    }
                }
        );
        mThumbnailDownloader.start();
        // Цикл сообщений состоит из потока и объекта Looper, управляющего очередью сообщений потока
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.photo_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        setupAdapter();
        return v;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // очистите загрузчик при уничтожении представления
        mThumbnailDownloader.clearQueue();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        // закрыть поток
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }
    // Метод setupAdapter() вызывается в onCreateView(…), чтобы каждый раз при создании
    // нового объекта RecyclerView он связывался с подходящим адаптером.
    // Метод также должен вызываться при каждом изменении набора объектов модели.
    private void setupAdapter() {
        // Проверка isAdded() подтверждает, что фрагмент был присоединен к активности, а следовательно,
        // что результат getActivity() будет отличен от null
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        // private TextView mTitleTextView;
        ImageView mItemImageView;
        public PhotoHolder(View itemView) {
            super(itemView);
           // mTitleTextView = (TextView) itemView;
            mItemImageView = (ImageView) itemView.findViewById(R.id.item_image_view);
        }
//        public void bindGalleryItem(GalleryItem item) {
//            mTitleTextView.setText(item.toString());
//        }
        // метод, назначающий объект Drawable виджету ImageView
        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }
    }

    // класс RecyclerView.Adapter, который будет предоставлять необходимые объекты PhotoHolder на основании списка GalleryItem
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }
        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
//            TextView textView = new TextView(getActivity());
//            return new PhotoHolder(textView);
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, viewGroup, false);
            return new PhotoHolder(view);
        }
        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            //photoHolder.bindGalleryItem(galleryItem);
            Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
            photoHolder.bindDrawable(placeholder);
            // вызовите метод queueThumbnail() потока и передайте ему объект PhotoHolder,
            // в котором в конечном итоге будет размещено изображение, и URL-адрес объекта GalleryItem для загрузки
            mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());

        }
        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>> {
        // метод doInBackground(…) для получения данных с сайта
        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            return new FlickrFetchr().fetchItems();
        }
        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            setupAdapter();
        }
    }
}