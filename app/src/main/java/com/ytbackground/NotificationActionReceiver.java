package com.ytbackground;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import static com.ytbackground.MainActivity.pausePlay;
import static com.ytbackground.MainActivity.runJs;

public class NotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String actionName = intent.getStringExtra("actionName");
        //Toast.makeText(context, actionName, Toast.LENGTH_SHORT).show();

        if(actionName==null) actionName="";
        if(actionName.equals("next"))
            runJs();
        else if(actionName.equals("pausePlay"))
            pausePlay();
        Log.d("debug-ac","action "+actionName);
    }
}