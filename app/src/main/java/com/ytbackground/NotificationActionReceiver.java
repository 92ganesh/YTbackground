package com.ytbackground;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import static com.ytbackground.MainActivity.ACTION_NAME;
import static com.ytbackground.MainActivity.PAUSE_PLAY;
import static com.ytbackground.MainActivity.pausePlay;

public class NotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String actionName = intent.getStringExtra(ACTION_NAME);
        if(actionName==null) actionName="";
        if(actionName.equals(PAUSE_PLAY))
            pausePlay();
        Log.d("debug-ac","action "+actionName);
    }
}