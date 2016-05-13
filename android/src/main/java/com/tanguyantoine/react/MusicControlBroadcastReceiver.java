package com.tanguyantoine.react;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MusicControlBroadcastReceiver extends Service {
    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_REWIND = "action_rewind";
    public static final String ACTION_FAST_FORWARD = "action_fast_foward";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("Service", "Created service");
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Service", "onStartCommand called " + intent.getAction());
        sendBroadcast(intent);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}