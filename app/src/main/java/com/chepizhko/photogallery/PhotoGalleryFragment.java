package com.chepizhko.photogallery;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends VisibleFragment {
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
        setHasOptionsMenu(true);
        // вызов execute() для нового экземпляра FetchItemsTask
        updateItems();


////////////////////////// Если не использовать Picasso
//        // передаём классу ThumbnailDownloader объект Handler, присоединенный к главному потоку
//        Handler responseHandler = new Handler();
//        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
//        // ThumbnailDownloadListener для обработки загруженного изображения после завершения загрузки
//        mThumbnailDownloader.setThumbnailDownloadListener(
//                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
//                    @Override
//                    public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
//
//                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
//                        photoHolder.bindDrawable(drawable);
//                    }
//                }
//        );
//        mThumbnailDownloader.start();
//        // Цикл сообщений состоит из потока и объекта Looper, управляющего очередью сообщений потока
//        mThumbnailDownloader.getLooper();
//        Log.i(TAG, "Background thread started");
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
        // очистите загрузчик при уничтожении представления если не Picasso!!!!!!!!!!!!
//        mThumbnailDownloader.clearQueue();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        // закрыть поток если не Picasso!!!!!!!!!!!!!
//        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery, menu);
        // мы получаем объект MenuItem, представляющий поле поиска, и сохраняем его в searchItem.
        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        // Затем из searchItem извлекается объект SearchView методом getActionView().
        final SearchView searchView = (SearchView) searchItem.getActionView();
        // интерфейс SearchView.OnQueryTextListener предоставляет возможность получения обратных вызовов при отправке запроса
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            // выполняется при отправке запроса пользователем. Отправленный запрос передается во входном параметре
            // Возвращение true сообщает системе, что поисковый запрос был обработан
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "QueryTextSubmit: " + s);
                // обновление сохраненного запроса при отправке нового запроса пользователем
                QueryPreferences.setStoredQuery(getActivity(), s);
                // В этом методе мы будем запускать FetchItemsTask для получения новых результатов
                updateItems();
                return true;
            }
            // выполняется при каждом изменении текста в текстовом поле SearchView.
            // метод будет вызываться каждый раз, когда изменяется хотя бы один символ
            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "QueryTextChange: " + s);
                return false;
            }
        });
        // заполнить текстовое поле поиска сохраненным запросом,
        // когда пользователь нажимает кнопку поиска для открытия SearchView
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                // задайте текст запроса SearchView при раскрытии представления
                searchView.setQuery(query, false);
            }
        });
        // проверьте, что сигнал активен, и измените текст menu_item_toggle_polling,
        // чтобы приложение выводило надпись, соответствующую текущему состоянию
        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }
    // Каждый раз, когда пользователь выбирает элемент Clear Search в дополнительном меню,
    // стирайте сохраненный запрос (присваиванием ему null).
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                // обновить меню на панели инструментов
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void updateItems() {
        // читаем сохраненный запрос из общих настроек и используем его для создания нового экземпляра FetchItemsTask
        String query = QueryPreferences.getStoredQuery(getActivity());
        // вызов execute() для нового экземпляра FetchItemsTask
        new FetchItemsTask(query).execute();
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

    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        // private TextView mTitleTextView;
        ImageView mItemImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView) {
            super(itemView);
           // mTitleTextView = (TextView) itemView;
            mItemImageView = (ImageView) itemView.findViewById(R.id.item_image_view);
            itemView.setOnClickListener(this);
        }
        ////////////////////////// Если не использовать Picasso
        //        public void bindGalleryItem(GalleryItem item) {
//            mTitleTextView.setText(item.toString());
//        }
//        // метод, назначающий объект Drawable виджету ImageView
//        public void bindDrawable(Drawable drawable) {
//            mItemImageView.setImageDrawable(drawable);
//        }
        /////////////////////////////////Picasso
        public void bindGalleryItem(GalleryItem galleryItem) {
            mGalleryItem = galleryItem;
            Picasso.with(getActivity())
                    .load(galleryItem.getUrl())
                    .placeholder(R.drawable.bill_up_close)
                    .into(mItemImageView);
        }
        ///////////////////////////////////////////////

        @Override
        public void onClick(View v) {
            // запуск баузера
            //Intent i = new Intent(Intent.ACTION_VIEW, mGalleryItem.getPhotoPageUri());
            // запуск WebView
            Intent i = PhotoPageActivity.newIntent(getActivity(), mGalleryItem.getPhotoPageUri());
            startActivity(i);
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

//            //photoHolder.bindGalleryItem(galleryItem);
//            Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
//            photoHolder.bindDrawable(placeholder);
//            // вызовите метод queueThumbnail() потока и передайте ему объект PhotoHolder,
//            // в котором в конечном итоге будет размещено изображение, и URL-адрес объекта GalleryItem для загрузки
//            mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
            /////////////////////////////// Picasso
            photoHolder.bindGalleryItem(galleryItem);
            ///////////////////////////////////////

        }
        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>> {
        private String mQuery;
        // конструктор, который получает строку запроса и сохраняет ее в переменной
        public FetchItemsTask(String query) {
            mQuery = query;
        }
        // метод doInBackground(…) для получения данных с сайта
        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            //String query = "robot"; // Для тестирования
            if (mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos();
            } else {
                return new FlickrFetchr().searchPhotos(mQuery);
            }
        }
        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            setupAdapter();
        }
    }
}