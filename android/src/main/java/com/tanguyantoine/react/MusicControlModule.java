package com.tanguyantoine.react;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

public class MusicControlModule extends ReactContextBaseJavaModule {

    static MusicControlModule INSTANCE;

    private boolean init = false;
    protected MediaSessionCompat session;

    private MediaMetadataCompat.Builder md;
    private PlaybackStateCompat.Builder pb;
    private NotificationCompat.Builder nb;

    private MusicControlNotification notification;
    private MusicControlListener.VolumeListener volume;
    private MusicControlReceiver receiver;

    private Thread artworkThread;

    private boolean isPlaying = false;
    private long controls = 0;

    public MusicControlModule(ReactApplicationContext context) {
        super(context);
    }

    @Override
    public String getName() {
        return "MusicControlManager";
    }

    @Override
    public Map<String, Object> getConstants() {
        Map<String, Object> map = new HashMap<>();
        map.put("STATE_ERROR", PlaybackStateCompat.STATE_ERROR);
        map.put("STATE_STOPPED", PlaybackStateCompat.STATE_STOPPED);
        map.put("STATE_PLAYING", PlaybackStateCompat.STATE_PLAYING);
        map.put("STATE_PAUSED", PlaybackStateCompat.STATE_PAUSED);
        map.put("STATE_BUFFERING", PlaybackStateCompat.STATE_BUFFERING);
        return map;
    }

    public void init() {
        INSTANCE = this;
        ReactApplicationContext context = getReactApplicationContext();

        ComponentName compName = new ComponentName(context, MusicControlReceiver.class);

        session = new MediaSessionCompat(context, "MusicControl", compName, null);
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(new MusicControlListener(context));

        volume = new MusicControlListener.VolumeListener(context, true, 100);
        session.setPlaybackToRemote(volume);

        md = new MediaMetadataCompat.Builder();
        pb = new PlaybackStateCompat.Builder();
        pb.setActions(controls);
        nb = new NotificationCompat.Builder(context);
        nb.setStyle(new NotificationCompat.MediaStyle().setMediaSession(session.getSessionToken()));

        notification = new MusicControlNotification(context);
        notification.updateActions(controls);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicControlNotification.REMOVE_NOTIFICATION);
        filter.addAction(Intent.ACTION_MEDIA_BUTTON);
        receiver = new MusicControlReceiver(notification, session);
        context.registerReceiver(receiver, filter);

        context.startService(new Intent(context, MusicControlNotification.NotificationService.class));

        isPlaying = false;
        init = true;
    }

    public void destroy() {
        notification.hide();
        session.release();
        getReactApplicationContext().unregisterReceiver(receiver);

        if(artworkThread != null && artworkThread.isAlive()) artworkThread.interrupt();
        artworkThread = null;

        session = null;
        notification = null;
        volume = null;
        receiver = null;
        md = null;
        pb = null;
        nb = null;

        init = false;
    }

    @ReactMethod
    public void enableBackgroundMode(boolean enable) {
        // Nothing?
    }

    @ReactMethod
    public void setNowPlaying(ReadableMap metadata) {
        if(!init) init();
        if(artworkThread != null && artworkThread.isAlive()) artworkThread.interrupt();

        String title = metadata.hasKey("title") ? metadata.getString("title") : null;
        String artist = metadata.hasKey("artist") ? metadata.getString("artist") : null;
        String album = metadata.hasKey("album") ? metadata.getString("album") : null;
        String genre = metadata.hasKey("genre") ? metadata.getString("genre") : null;
        String description = metadata.hasKey("description") ? metadata.getString("description") : null;
        String date = metadata.hasKey("date") ? metadata.getString("date") : null;
        RatingCompat rating = metadata.hasKey("rating") ? RatingCompat.newPercentageRating(metadata.getInt("rating")) : null;
        final String artwork = metadata.hasKey("artwork") ? metadata.getString("artwork") : null;
        long duration = metadata.hasKey("duration") ? (long)(metadata.getDouble("duration") * 1000) : 0;
        int notificationColor = metadata.hasKey("color") ? metadata.getInt("color") : NotificationCompat.COLOR_DEFAULT;

        md.putText(MediaMetadataCompat.METADATA_KEY_TITLE, title);
        md.putText(MediaMetadataCompat.METADATA_KEY_ARTIST, artist);
        md.putText(MediaMetadataCompat.METADATA_KEY_ALBUM, album);
        md.putText(MediaMetadataCompat.METADATA_KEY_GENRE, genre);
        md.putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, description);
        md.putText(MediaMetadataCompat.METADATA_KEY_DATE, date);
        md.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);
        md.putRating(MediaMetadataCompat.METADATA_KEY_RATING, rating);

        nb.setContentTitle(title);
        nb.setContentText(artist);
        nb.setContentInfo(album);
        nb.setColor(notificationColor);

        if(artwork != null) {
            artworkThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = loadArtwork(artwork);
                    md.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap);
                    nb.setLargeIcon(bitmap);

                    session.setMetadata(md.build());
                    notification.show(nb, isPlaying);
                    artworkThread = null;
                }
            });
            artworkThread.start();
        } else {
            md.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, null);
            nb.setLargeIcon(null);
        }

        session.setMetadata(md.build());
        session.setActive(true);
        notification.show(nb, isPlaying);
    }

    @ReactMethod
    public void setPlayback(ReadableMap info) {
        if(!init) init();

        long elapsedTime = info.hasKey("elapsedTime") ? (long)(info.getDouble("elapsedTime") * 1000) : 0;
        long bufferedTime = info.hasKey("bufferedTime") ? (long)(info.getDouble("bufferedTime") * 1000) : 0;
        float speed = info.hasKey("speed") ? (float)info.getDouble("speed") : 1;
        int state = info.hasKey("state") ? info.getInt("state") : PlaybackStateCompat.STATE_NONE;
        int vol = info.hasKey("volume") ? info.getInt("volume") : 100;

        pb.setState(state, elapsedTime, speed);
        pb.setBufferedPosition(bufferedTime);

        isPlaying = state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_BUFFERING;
        notification.show(nb, isPlaying);

        PlaybackStateCompat playbackState = pb.build();
        session.setPlaybackState(playbackState);
        session.setPlaybackToRemote(volume.create(null, vol));
    }

    @ReactMethod
    public void resetNowPlaying() {
        if(!init) return;
        if(artworkThread != null && artworkThread.isAlive()) artworkThread.interrupt();
        artworkThread = null;

        md = new MediaMetadataCompat.Builder();
        pb = new PlaybackStateCompat.Builder();

        notification.hide();
        session.setActive(false);
    }

    @ReactMethod
    public void enableControl(String control, boolean enable) {
        if(!init) init();

        long controlValue;
        switch(control) {
            case "nextTrack":
                controlValue = PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
                break;
            case "previousTrack":
                controlValue = PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
                break;
            case "play":
                controlValue = PlaybackStateCompat.ACTION_PLAY;
                break;
            case "pause":
                controlValue = PlaybackStateCompat.ACTION_PAUSE;
                break;
            case "togglePlayPause":
                controlValue = PlaybackStateCompat.ACTION_PLAY_PAUSE;
                break;
            case "stop":
                controlValue = PlaybackStateCompat.ACTION_STOP;
                break;
            case "seek":
                controlValue = PlaybackStateCompat.ACTION_SEEK_TO;
                break;
            case "seekForward":
                controlValue = PlaybackStateCompat.ACTION_FAST_FORWARD;
                break;
            case "seekBackward":
                controlValue = PlaybackStateCompat.ACTION_REWIND;
                break;
            case "rate":
                controlValue = PlaybackStateCompat.ACTION_SET_RATING;
                break;
            case "volume":
                session.setPlaybackToRemote(volume.create(enable, null));
                return;
            default:
                // Unknown control type, let's just ignore it
                return;
        }

        if(enable) {
            controls |= controlValue;
        } else {
            controls &= ~controlValue;
        }

        notification.updateActions(controls);
        pb.setActions(controls);
        session.setPlaybackState(pb.build());
    }

    private Bitmap loadArtwork(String url) {
        Bitmap bitmap = null;
        try {
            if(url.matches("^(https?|ftp)://.*$")) { // URL
                URLConnection con = new URL(url).openConnection();
                con.connect();
                InputStream input = con.getInputStream();
                bitmap = BitmapFactory.decodeStream(input);
                input.close();
            } else { // File
                bitmap = BitmapFactory.decodeFile(url);
            }
        } catch(IOException ex) {
            Log.w("MusicControl", "Could not load the artwork", ex);
        }
        return bitmap;
    }

}
