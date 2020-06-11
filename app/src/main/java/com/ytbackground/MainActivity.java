package com.ytbackground;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements ComponentCallbacks2 {
    MyWebView webView;
    boolean debug = true;
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String path;
        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Log.d("debug-ac","onCreate called");
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }

        SharedPreferences sharedpreferences = getSharedPreferences("YTB", Context.MODE_PRIVATE);
        if(debug){
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString("phone", "debugging");
            editor.commit();
        }

        String deviceModel = sharedpreferences.getString("phone","not_available");
        if(deviceModel==null || deviceModel.equals("not_available")){
            registerNewDevice(sharedpreferences);
        }else{
            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference userDetails = database.getReference("userDetails");
            String[] accessedTime = getTime();
            userDetails.child(deviceModel+"").child(accessedTime[0]).setValue(accessedTime[1]);
        }

        path = "https://www.youtube.com";
        webView = (MyWebView)findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl(path);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if(action==null)
            action="null";
        Log.d("debug-ac",action);
        if ( Intent.ACTION_MAIN.equals(action) && ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) == Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)) {
            finish();
        }

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

        startService();
    }

    public void runJs(View view){
        webView.loadUrl("javascript:(function(){"+
                //"setInterval(function(){ " +
                "    var x = document.getElementsByClassName(\"ytp-ad-skip-button\"); " +
            //    "    var x = document.getElementsByClassName(\"ytp-upnext-autoplay-icon\"); " +
                "    if(x.length!=0){ " +
                "        x[0].click();  " +
                "    }  " +
               // "}, 1000); " +
           //     "alert('sdf');"+
                "})()");
    }

    public void startService(){
        Intent serviceIntent = new Intent(this, ExampleService.class);
        serviceIntent.putExtra("inputExtra", "start service");
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void startService(View view){
        startService();
    }
    public void stopService(View view){
        Intent serviceIntent = new Intent(this, ExampleService.class);
        stopService(serviceIntent);
    }

    private static void registerNewDevice(final SharedPreferences sharedpreferences){
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference totalUsers = database.getReference("totalUsers");
        totalUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long userID = dataSnapshot.getValue(Long.class) ;
                if(userID!=null) {
                    userID++;
                    String userIDandModel =  userID + "_" + getDeviceName();
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString("phone", userIDandModel);
                    editor.commit();

                    totalUsers.setValue(userID);
                    DatabaseReference userDetails = database.getReference("userDetails");
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
    protected void onResume() {
        super.onResume();
        Log.d("debug-ac","onResume called");
//        Intent intent = getIntent();
//        String action = intent.getAction();
//        String type = intent.getType();
//
//        if(action==null)
//            action="null";
//        Log.d("debug-ac",action);
//        if (Intent.ACTION_SEND.equals(action) && type != null) {
//            if ("text/plain".equals(type)) {
//                String path = handleSendText(intent);
//
//                webView = (MyWebView)findViewById(R.id.webView);
//                webView.getSettings().setJavaScriptEnabled(true);
//                webView.setWebViewClient(new WebViewClient());
//                webView.setWebChromeClient(new WebChromeClient());
//                webView.loadUrl(path);
//                Log.d("debug-ac",path);
//            }
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("debug-ac","onPause called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("debug-ac","onStop called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("debug-ac","onDestroy called");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("debug-ac","onRestart called");
    }

    @Override
    public void onBackPressed() {
        if(webView.canGoBack())
            webView.goBack();
        else
            super.onBackPressed();
    }

    public String handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            return sharedText;
        }
        return "Invalid url";
    }

    public void onTrimMemory(int level) {
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userDetails = database.getReference("userDetails");
        String[] accessedTime = getTime();
        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
                Log.d("debug-ac", "TRIM_MEMORY_UI_HIDDEN");
                userDetails.child("debugging").child(accessedTime[0]).setValue(accessedTime[1]+" TRIM_MEMORY_UI_HIDDEN");
                break;

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                //Log.d("debug-ac", "TRIM_MEMORY_RUNNING_MODERATE " + ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE);
                //userDetails.child("debugging").child(accessedTime[0]).setValue("TRIM_MEMORY_RUNNING_MODERATE " + ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE);
                break;

            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
               // Log.d("debug-ac", "TRIM_MEMORY_BACKGROUND " + ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);
                //userDetails.child("debugging").child(accessedTime[0]).setValue("TRIM_MEMORY_BACKGROUND " + ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);
                break;

            default:
                Log.d("debug-ac", "default");
                userDetails.child("debugging").child(accessedTime[0]).setValue("default");
                break;
        }
    }

}

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

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility != View.GONE && visibility != View.INVISIBLE)
            super.onWindowVisibilityChanged(visibility);
    }
}