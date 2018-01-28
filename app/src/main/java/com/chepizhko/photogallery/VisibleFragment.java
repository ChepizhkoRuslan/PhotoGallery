package com.chepizhko.photogallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;

// * Этот класс будет представлять обобщенный фрагмент, скрывающий оповещения переднего плана

public abstract class VisibleFragment extends Fragment {
    private static final String TAG = "VisibleFragment";

    // создаём динамический BroadcastReceiver
    @Override
    public void onStart() {
        super.onStart();
        // Любой объект IntentFilter, который можно выразить в XML, также может быть представлен в коде подобным образом.
        // Просто вызывайте addCategory(String), addAction(String), addDataPath(String) и так далее для настройки фильтра
        IntentFilter filter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
        // регистрация приемника
        getActivity().registerReceiver(mOnShowNotification, filter, PollService.PERM_PRIVATE, null);
    }
    @Override
    public void onStop() {
        super.onStop();
        // отмена приемника
        getActivity().unregisterReceiver(mOnShowNotification);
    }
    private BroadcastReceiver mOnShowNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Получение означает, что пользователь видит приложение,поэтому оповещение отменяется
            // Так как в нашем примере необходимо лишь подать сигнал «да/нет», нам достаточно кода результата
            // Если потребуется вернуть более сложные данные, используйте setResultData(String) или setResultExtras(Bundle).
            // А если вы захотите задать все три значения, вызовите setResult(int,String,Bundle).
            Log.i(TAG, "canceling notification");
            setResultCode(Activity.RESULT_CANCELED);
        }
    };
}