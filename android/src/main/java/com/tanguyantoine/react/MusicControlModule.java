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
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.network.OkHttpClientProvider;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import java.io.IOException;
import java.io.InputStream;
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

    private boolean isPlaying = false;
    private long controls = 0;

    public MusicControlModule(ReactApplicationContext context) {
        super(context);
        init();
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

        notification = new MusicControlNotification(context, compName);
        notification.updateActions(controls);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicControlNotification.REMOVE_NOTIFICATION);
        filter.addAction(Intent.ACTION_MEDIA_BUTTON);
        receiver = new MusicControlReceiver(notification, session);
        context.registerReceiver(receiver, filter);

        context.startService(new Intent(context.getBaseContext(), MusicControlNotification.NotificationService.class));

        isPlaying = false;
        init = true;
    }

    public void destroy() {
        notification.hide();
        session.release();
        getReactApplicationContext().unregisterReceiver(receiver);

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
    public void setNowPlaying(final ReadableMap metadata, final Promise promise) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    setNowPlayingSync(metadata, promise);
                } catch(Exception ex) {
                    promise.reject(ex);
                }
            }
        }).start();
    }

    private void setNowPlayingSync(ReadableMap metadata, Promise promise) {
        if(!init) init();

        String title = metadata.hasKey("title") ? metadata.getString("title") : null;
        String artist = metadata.hasKey("artist") ? metadata.getString("artist") : null;
        String album = metadata.hasKey("album") ? metadata.getString("album") : null;
        String genre = metadata.hasKey("genre") ? metadata.getString("genre") : null;
        String description = metadata.hasKey("description") ? metadata.getString("description") : null;
        String date = metadata.hasKey("date") ? metadata.getString("date") : null;
        RatingCompat rating = metadata.hasKey("rating") ? RatingCompat.newPercentageRating(metadata.getInt("rating")) : null;
        Bitmap artwork = metadata.hasKey("artwork") ? loadArtwork(metadata.getString("artwork")) : null;
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
        md.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artwork);

        nb.setLargeIcon(artwork);
        nb.setContentTitle(title);
        nb.setContentText(artist);
        nb.setContentInfo(album);
        nb.setColor(notificationColor);

        MediaMetadataCompat mediaMetadata = md.build();
        session.setMetadata(mediaMetadata);
        session.setActive(true);
        notification.show(nb, isPlaying);

        promise.resolve(null);
    }

    @ReactMethod
    public void setPlayback(ReadableMap info, Promise promise) {
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

        promise.resolve(null);
    }

    @ReactMethod
    public void resetNowPlaying() {
        if(!init) return;

        md = new MediaMetadataCompat.Builder();
        pb = new PlaybackStateCompat.Builder();

        notification.hide();
        session.setActive(false);
    }

    @ReactMethod
    public void enableControl(String control, boolean enable) {
        if(!init) init();

        long controlValue = 0;
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
                // Use OkHttp to take advantage of configured options (such as caching)
                OkHttpClient client = OkHttpClientProvider.getOkHttpClient();
                Call call = client.newCall(new Request.Builder().url(url).build());
                InputStream input = call.execute().body().byteStream();
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
