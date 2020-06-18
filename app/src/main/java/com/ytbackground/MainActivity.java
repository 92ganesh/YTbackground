package com.ytbackground;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements ComponentCallbacks2 {
    boolean DEBUG = false;     //Make sure to set it false for release version
    public static final String CHANNEL_ID="YTBServiceChannel";
    public static final String ACTION_NAME = "ACTION_NAME";
    public static final String PAUSE_PLAY = "PAUSE_PLAY";
    public static final String TOGGLE_PAUSE_PLAY = "TOGGLE_PAUSE_PLAY";
    public static final String PHONE = "PHONE";
    public static final String USER_DETAILS = "userDetails";
    public static final String TOTAL_USERS = "totalUsers";
    static MyWebView webView;
    static boolean isPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //hide status bar for api level <4.1
        //This also works for higher versions too and provides auto hide of status bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        //set default screen orientation method
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_FULL_USER);

        // Hide the status bar and action
        final View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        if(getSupportActionBar()!=null)
            getSupportActionBar().hide();

        final FrameLayout customViewContainer = findViewById(R.id.frame);  //used to display full screen videos
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        // Used to hide navigation bar in full screen mode
                        if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                            //following method is called with a delay
                            //It auto hides the navigation bar after user clicks on video in full screen mode
                            new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(customViewContainer.getVisibility()==View.VISIBLE && webView.getVisibility()==View.GONE) {
                                            View decorView = getWindow().getDecorView();
                                            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
                                            decorView.setSystemUiVisibility(uiOptions);
                                        }
                                    }
                                }, 1500);
                        }
                    }
                });

        SharedPreferences sharedpreferences = getSharedPreferences("YTB", Context.MODE_PRIVATE);

        // add 'debugging' as model while developing
        if(DEBUG){
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString(PHONE, "debugging");
            editor.commit();
        }


        //store Device model number in the sharedPreferences if using the app for the first time
        //else register current time in firebase database
        String deviceModel = sharedpreferences.getString(PHONE,"not_available");
        if(deviceModel==null || deviceModel.equals("not_available")){
            registerNewDevice(sharedpreferences);
        }else{
            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference userDetails = database.getReference(USER_DETAILS);
            String[] accessedTime = getTime();
            userDetails.child(deviceModel+"").child(accessedTime[0]).setValue(accessedTime[1]);
        }

        String path = "https://www.youtube.com";
        webView = (MyWebView)findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {

            //toggle icon of pause/play in notification when there is output as "switchPausePlay" on js console
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                if(consoleMessage.message().equals(TOGGLE_PAUSE_PLAY)){
                    togglePausePlay();
                }
                return true;
            }

            //on user's request for full screen set custom view as frameLayout
            @Override
            public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
                super.onShowCustomView(view,callback);
                //set view in landscape mode when user requests for full screen
                setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

                // Hide both the navigation bar and the status bar
                //note any user activity will clear flags which needs to set again
                //see decorView.setOnSystemUiVisibilityChangeListener()
                View decorView = getWindow().getDecorView();
                int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN;
                decorView.setSystemUiVisibility(uiOptions);

                webView.setVisibility(View.GONE);
                customViewContainer.setVisibility(View.VISIBLE);
                customViewContainer.addView(view);
            }

            //on user's request for exiting full screen set custom view back to main layout
            @Override
            public void onHideCustomView () {
                super.onHideCustomView();
                //set view in landscape mode when user requests for full screen
                setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_FULL_USER);

                webView.setVisibility(View.VISIBLE);
                customViewContainer.setVisibility(View.GONE);
            }
        });
        webView.loadUrl(path);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if(action==null)
            action="null";
        Log.d("debug-ac",action);

        //****  To avoid onCreate() being called again when activity is brought in front from the background
        if ( Intent.ACTION_MAIN.equals(action) && ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) == Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)) {
            finish();
        }
        //****

        //**** To respond to send intent from other apps
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                path = handleSendText(intent);

                webView = (MyWebView)findViewById(R.id.webView);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.setWebViewClient(new WebViewClient());
                webView.setWebChromeClient(new WebChromeClient());
                webView.loadUrl(path);
                Log.d("debug-ac",path);
            }
        }
        //****

        // start foreground service
        startService();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        String type = intent.getType();

        if(action==null)
            action="null";
        Log.d("debug-ac",action);
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String path = handleSendText(intent);
                webView = (MyWebView)findViewById(R.id.webView);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.setWebViewClient(new WebViewClient());
                webView.setWebChromeClient(new WebChromeClient());
                webView.loadUrl(path);
                Log.d("debug-ac",path);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(webView.canGoBack())
            webView.goBack();
        else
            super.onBackPressed();
    }

    /*  Toggles pause/play icon in notification
    * */
    public void togglePausePlay(){
        Context context = this;
        MediaSessionCompat mediaSession = new MediaSessionCompat(context, "tag");
        // Pending intent for main notification tap action
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, notificationIntent, 0);

        // Pending intent for pausePlay button
        Bitmap artwork = BitmapFactory.decodeResource(getResources(), R.drawable.icon_bmp);
        Intent pausePlayIntent = new Intent(context, NotificationActionReceiver.class);
        pausePlayIntent.putExtra(ACTION_NAME, PAUSE_PLAY);
        PendingIntent pausePlayPendingIntent =
                PendingIntent.getBroadcast(context, 0, pausePlayIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.sound)
                .setLargeIcon(artwork)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setColor(Color.RED)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true);

        if(!isPlaying){
            notificationBuilder.addAction(R.drawable.pause, PAUSE_PLAY, pausePlayPendingIntent);
        }else{
            notificationBuilder.addAction(R.drawable.play, PAUSE_PLAY, pausePlayPendingIntent);
        }
        isPlaying = !isPlaying;

        notificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0)
                        .setMediaSession(mediaSession.getSessionToken()));

        Notification notification = notificationBuilder.build();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, notification);
    }

    /*  To click pause/play button on the video in the webview
    * */
    public static void pausePlay(){
        webView.loadUrl("javascript:(function(){"+
                "    var x = document.getElementsByClassName(\"player-control-play-pause-icon\"); " +
                "    if(x.length!=0){ " +
                "        console.log('"+TOGGLE_PAUSE_PLAY+"');" +
                "        x[0].click();  " +
                "    }  " +
                "})()");
    }

    /*  To start foregroundService
    * */
    public void startService(){
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    /*  For a new device, assign a id number(userIDandModel) and regiter it in the firebase database
    *   Id number is <count_of_users+1>_<model_number> eg 23_Samsung Note 10
    *   after registering store the userIDandModel in the sharedPreferences
    * */
    private static void registerNewDevice(final SharedPreferences sharedpreferences){
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference totalUsers = database.getReference(TOTAL_USERS);
        totalUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long userID = dataSnapshot.getValue(Long.class) ;
                if(userID!=null) {
                    userID++;
                    String userIDandModel =  userID + "_" + getDeviceName();
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString(PHONE, userIDandModel);
                    editor.commit();

                    totalUsers.setValue(userID);
                    DatabaseReference userDetails = database.getReference(USER_DETAILS);
                    String[] accessedTime = getTime();
                    userDetails.child(userIDandModel+"").child(accessedTime[0]).setValue(accessedTime[1]);
                    Log.d("debug-ac","userIDandModel "+userID+" "+accessedTime[1]);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w("debug-ac", "Failed to read value.", error.toException());
            }
        });
    }

    //*******  Utility methods  *****************
    private static String[] getTime(){
        Calendar calendar = Calendar.getInstance();
        String[] time = new String[2];
        time[0] =  calendar.getTime().getTime()+"";
        time[1] =  calendar.getTime().toLocaleString();
        return time;
    }

    private static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;

        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }

        return phrase.toString();
    }

    public String handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            return sharedText;
        }
        return "Invalid url";
    }
    //*****************************************
}

// webView does not run in background by default
// To keep the webView running in the background use the override the onWindowVisibilityChanged()
class MyWebView extends WebView {
    public MyWebView(Context context) {
        super(context);
    }
    public MyWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public MyWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //To keep running the webView in the background
    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility != View.GONE && visibility != View.INVISIBLE)
            super.onWindowVisibilityChanged(visibility);
    }
}