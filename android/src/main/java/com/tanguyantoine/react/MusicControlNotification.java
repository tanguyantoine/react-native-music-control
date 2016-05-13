package com.tanguyantoine.react;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;

import android.app.Notification;
import android.app.NotificationManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableMap;


public class MusicControlNotification {
    private Bitmap bitmapCover;
    public static final String ARTWORK_URL_KEY = "artworkUrl";

    public void update(ReadableMap infos){
        // Check if the cover has changed
        if(!infos.getString(ARTWORK_URL_KEY).isEmpty()){
            this.getBitmapCover(infos.getString(ARTWORK_URL_KEY));
        }

        /*this.infos = newInfos;
        this.createBuilder();
        Notification noti = this.notificationBuilder.build();
        this.notificationManager.notify(this.notificationID, noti);*/
    }

    // Get image from url
    private void getBitmapCover(String coverURL){
        try {
            this.bitmapCover = getBitmapFromURL(coverURL);
        }   catch (Exception ex) {
            ex.printStackTrace();
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
}
