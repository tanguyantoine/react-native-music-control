package com.tanguyantoine.react;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableNativeMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableMap;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;


public class MusicControlModule extends ReactContextBaseJavaModule {
    private ReactContext reactContext;
    private MediaSession mediaSession;
    private Notification.Builder notificationBuilder;
    private ReadableMap infos;
    private WritableNativeMap enabledControls;
    public NotificationManager notificationManager;

    public static final String MUSIC_CONTROL_EVENT_NAME = "RNMusicControlEvent";
    public static final String ACTION_PLAY = "play";
    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_NEXT = "nextTrack";
    public static final String ACTION_PREVIOUS = "previousTrack";
    public static final String ACTION_STOP = "STOP";

    public static final String CONTROL_pause = "pause";
    public static final String CONTROL_play = "play";
    public static final String CONTROL_stop = "stop";
    public static final String CONTROL_nextTrack = "nextTrack";
    public static final String CONTROL_previousTrack = "previousTrack";
    //public static final String CONTROL_seekForward = "seekForward";
    //public static final String CONTROL_seekBackward = "seekBackward";

    public static final int NOTIFICATION_ID = 7386298;

    public MusicControlModule(ReactApplicationContext reactContext, Activity activity) {
        super(reactContext);
        this.reactContext = reactContext;
        this.mediaSession = new MediaSession(reactContext, reactContext.getPackageName());
        this.enabledControls = new WritableNativeMap();
        this.infos = new ReadableNativeMap();

        IntentFilter intentFilter = new IntentFilter("nextTrack");

        reactContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sendEvent("nextTrack");
            }
        }, intentFilter);
    }

    public void handleIntent(Intent intent) {
        sendEvent("play");
        new AlertDialog.Builder(reactContext)
                .setTitle("Delete entry")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public String getName() {
        return "MusicControlManager";
    }

    @ReactMethod
    public void enableBackgroundMode(Boolean enable) {
        WritableMap params = Arguments.createMap();
        this.mediaSession.setActive(true);
    }

    @ReactMethod
    public void setNowPlaying(ReadableMap newInfos) {
        this.infos = newInfos;
        updateNotification();
    }

    @ReactMethod
    public void resetNowPlaying() {
        infos = new ReadableNativeMap();
        updateNotification();
    }

    @ReactMethod
    public void enableControl(String controlName, Boolean enabled) {
        this.enabledControls.putBoolean(controlName, enabled);
        updateNotification();
    }


    private void sendEvent(String eventName) {
        WritableMap params = Arguments.createMap();
        params.putString("name", eventName);
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(MUSIC_CONTROL_EVENT_NAME, params);
    }

    private Notification.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(reactContext, MusicControlBroadcastReceiver.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(reactContext, 1, intent, 0);
        return new Notification.Action.Builder(icon, title, pendingIntent).build();
    }

    // Get image from url
    private Bitmap getBitmapCover(String coverURL){
        Bitmap mybitmap;
        try{
            if(coverURL.matches("^(https?|ftp)://.*$"))
                // Remote image
                mybitmap = getBitmapFromURL(coverURL);
            else{
                // Local image
                mybitmap = getBitmapFromLocal(coverURL);
            }
            return mybitmap;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // get Local image
    private Bitmap getBitmapFromLocal(String localURL){
        try {
            Uri uri = Uri.parse(localURL);
            File file = new File(uri.getPath());
            FileInputStream fileStream = new FileInputStream(file);
            BufferedInputStream buf = new BufferedInputStream(fileStream);
            Bitmap myBitmap = BitmapFactory.decodeStream(buf);
            buf.close();
            return myBitmap;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // get Remote image
    private Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void updateNotification(){
        initNotificationBuilder();
        //artwork uri : path absolute
        String filePath = infos.getString("artwork");
        Bitmap mybitmap = getBitmapCover(filePath);

        this.notificationBuilder.setSmallIcon(android.R.drawable.ic_media_play);
        //icono largo es bitmap
        this.notificationBuilder.setLargeIcon(mybitmap);
        String title = infos.hasKey("title") ? infos.getString("title") : "";
        String content = infos.hasKey("artist") ? infos.getString("artist") : "";
        this.notificationBuilder
                .setContentTitle(title)
                .setContentText(content);

        //open app if tapped
        Intent resultIntent = new Intent(reactContext, this.getCurrentActivity().getClass());
        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(reactContext, 0, resultIntent, 0);
        this.notificationBuilder.setContentIntent(resultPendingIntent);

        //notification can't be destroyed by swiping
        //this.notificationBuilder.setOngoing(true);

        //If 5.0 >= set the controls to be visible on lockscreen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
            this.notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        if(shouldActivateControl(CONTROL_previousTrack)){
            this.notificationBuilder
                    .addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS));
        }
        //builder.addAction(generateAction(android.R.drawable.ic_media_rew, "Rewind", ACTION_REWIND));
        if(shouldActivateControl(CONTROL_play)) {
            this.notificationBuilder
                    .addAction(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));
        }
        if(shouldActivateControl(CONTROL_pause)) {
            this.notificationBuilder
                    .addAction(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
        }
        //builder.addAction(generateAction(android.R.drawable.ic_media_ff, "Fast Foward", ACTION_FAST_FORWARD));
        if(shouldActivateControl(CONTROL_nextTrack)) {
            this.notificationBuilder
                    .addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT));
        }

        this.notificationBuilder.setPriority(Notification.PRIORITY_MAX);

        notify(this.notificationBuilder);

    }

    private boolean shouldActivateControl(String controlName){
        return enabledControls.hasKey(controlName) && enabledControls.getBoolean(controlName);
    }

    private void initNotificationBuilder(){
        if(notificationBuilder == null) {
            Notification.MediaStyle style = new Notification.MediaStyle();
            Intent intent = new Intent(reactContext, getClass());
            intent.setAction(ACTION_STOP);
            PendingIntent pendingIntent = PendingIntent.getService(reactContext, 1, intent, 0);
            this.notificationBuilder = new Notification.Builder(reactContext);
            this.notificationBuilder
                    .setStyle(style)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setDeleteIntent(pendingIntent);
        }
    }

    private void notify(Notification.Builder builder){
        this.notificationManager = (NotificationManager) reactContext.getSystemService(Context.NOTIFICATION_SERVICE );
        this.notificationManager.notify(NOTIFICATION_ID, builder.build());
        this.notificationBuilder = null;
    }

    public void destroy(){
        this.notificationManager.cancel(NOTIFICATION_ID);
    }
}
