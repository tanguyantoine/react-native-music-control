package com.tanguyantoine.react;


import android.content.Intent;
import android.content.Context;
import android.os.IBinder;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.support.v4.content.LocalBroadcastManager;

public class MusicControlBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context,Intent intent) {
        Intent newIntent = new Intent();
        newIntent.setAction(intent.getAction());
        LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent);
    }

}