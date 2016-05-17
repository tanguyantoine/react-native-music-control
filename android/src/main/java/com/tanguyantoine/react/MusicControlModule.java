package com.tanguyantoine.react;

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
import android.media.AudioManager;
import android.media.session.MediaSession;
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


import java.util.Map;


public class MusicControlModule extends ReactContextBaseJavaModule {
    private ReactContext reactContext;
    private MediaSession mediaSession;
    private Notification.Builder notificationBuilder;
    private ReadableMap infos;
    private WritableNativeMap enabledControls;

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

    private void updateNotification(){
        initNotificationBuilder();

        this.notificationBuilder
                .setSmallIcon(android.R.drawable.arrow_up_float);
        String title = infos.hasKey("title") ? infos.getString("title") : "";
        String content = infos.hasKey("artist") ? infos.getString("artist") : "";
        this.notificationBuilder
                .setContentTitle(title)
                .setContentText(content);

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
        NotificationManager notificationManager = (NotificationManager) reactContext.getSystemService(Context.NOTIFICATION_SERVICE );
        notificationManager.notify(NOTIFICATION_ID, builder.build());
        this.notificationBuilder = null;
    }
}
