package com.ytbackground;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import static com.ytbackground.MainActivity.CHANNEL_ID;

//Create a Notification channel for the foreground service
public class NotificationChannelCreation extends android.app.Application{
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel serviceChannel = new android.app.NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_ID,
                    NotificationManager.IMPORTANCE_MIN
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}