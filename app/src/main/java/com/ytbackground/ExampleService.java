package com.ytbackground;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import static com.ytbackground.App.CHANNEL_ID;

public class ExampleService extends Service {
    Notification notification;
    private MediaSessionCompat mediaSession;
    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(this, "tag");
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Pending intent for main notification tap action
        String input = intent.getStringExtra("inputExtra");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);


        // Pending intent for pausePlay button
        Bitmap artwork = BitmapFactory.decodeResource(getResources(), R.drawable.icon_bmp);
        MainActivity.isPlaying=true;
        Intent pausePlayIntent = new Intent(this, NotificationActionReceiver.class);
        pausePlayIntent.putExtra("actionName", "pausePlay");
        PendingIntent pausePlayPendingIntent =
                PendingIntent.getBroadcast(this, 0, pausePlayIntent, PendingIntent.FLAG_UPDATE_CURRENT);

//        // Pending intent for next button
//        Intent nextIntent = new Intent(this, NotificationActionReceiver.class);
//        nextIntent.putExtra("actionName", "next");
//        PendingIntent nextPendingIntent =
//                PendingIntent.getBroadcast(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);



        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon)
                .setLargeIcon(artwork)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setColor(Color.RED)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .addAction(R.drawable.pause, "Pause", pausePlayPendingIntent)
    //            .addAction(R.drawable.next, "Next", nextPendingIntent)
                // Take advantage of MediaStyle features
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0)
                        .setMediaSession(mediaSession.getSessionToken()))
                .build();

        startForeground(1, notification);
        //do heavy work on a background thread
        //stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        this.stopSelf();
    }
}
