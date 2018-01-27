package com.chepizhko.photogallery;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

public class PhotoGalleryActivity extends SingleFragmentActivity {
    // Этот метод возвращает экземпляр Intent, который может использоваться для запуска PhotoGalleryActivity.
    // Вскоре PollService будет вызывать PhotoGalleryActivity.newIntent(…),
    // упаковывать полученный интент в PendingIntent и назначать PendingIntent оповещению
    public static Intent newIntent(Context context) {
        return new Intent(context, PhotoGalleryActivity.class);
    }
    @Override
    protected Fragment createFragment() {
        return PhotoGalleryFragment.newInstance();
    }
}
