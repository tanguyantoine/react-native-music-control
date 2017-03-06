package com.tanguyantoine.react;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.SystemClock;
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
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.views.imagehelper.ResourceDrawableIdHelper;
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

    private PlaybackStateCompat state;

    protected MusicControlNotification notification;
    private MusicControlListener.VolumeListener volume;
    private MusicControlReceiver receiver;
    private boolean remoteVolume = false;

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

        volume = new MusicControlListener.VolumeListener(context, true, 100, 100);
        if(remoteVolume) {
            session.setPlaybackToRemote(volume);
        } else {
            session.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
        }

        md = new MediaMetadataCompat.Builder();
        pb = new PlaybackStateCompat.Builder();
        pb.setActions(controls);
        nb = new NotificationCompat.Builder(context);
        nb.setStyle(new NotificationCompat.MediaStyle().setMediaSession(session.getSessionToken()));

        state = pb.build();

        notification = new MusicControlNotification(context);
        notification.updateActions(controls);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicControlNotification.REMOVE_NOTIFICATION);
        filter.addAction(MusicControlNotification.MEDIA_BUTTON);
        filter.addAction(Intent.ACTION_MEDIA_BUTTON);
        receiver = new MusicControlReceiver(this, context.getPackageName());
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
        RatingCompat rating = metadata.hasKey("rating") ? RatingCompat.newPercentageRating(metadata.getInt("rating")) : RatingCompat.newUnratedRating(RatingCompat.RATING_PERCENTAGE);
        long duration = metadata.hasKey("duration") ? (long)(metadata.getDouble("duration") * 1000) : 0;
        int notificationColor = metadata.hasKey("color") ? metadata.getInt("color") : NotificationCompat.COLOR_DEFAULT;

        String artwork = null;
        boolean localArtwork = false;
        if(metadata.hasKey("artwork")) {
            if(metadata.getType("artwork") == ReadableType.Map) {
                artwork = metadata.getMap("artwork").getString("uri");
                localArtwork = true;
            } else {
                artwork = metadata.getString("artwork");
            }
        }

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
            final String artworkUrl = artwork;
            final boolean artworkLocal = localArtwork;

            artworkThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = loadArtwork(artworkUrl, artworkLocal);
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
    public void updatePlayback(ReadableMap info) {
        if(!init) init();

        long elapsedTime;
        long bufferedTime = info.hasKey("bufferedTime") ? (long)(info.getDouble("bufferedTime") * 1000) : state.getBufferedPosition();
        float speed = info.hasKey("speed") ? (float)info.getDouble("speed") : state.getPlaybackSpeed();
        int pbState = info.hasKey("state") ? info.getInt("state") : state.getState();
        int maxVol = info.hasKey("maxVolume") ? info.getInt("maxVolume") : volume.getMaxVolume();
        int vol = info.hasKey("volume") ? info.getInt("volume") : volume.getCurrentVolume();

        if(info.hasKey("elapsedTime")) {
            elapsedTime = (long)(info.getDouble("elapsedTime") * 1000);
        } else {
            // Calculates the new elapsed time based on the state and speed
            long deltaTime = SystemClock.elapsedRealtime() - state.getLastPositionUpdateTime();
            if(state.getState() != PlaybackStateCompat.STATE_PLAYING) deltaTime = 0;
            elapsedTime = state.getPosition() + (long)(deltaTime * speed);
        }

        pb.setState(pbState, elapsedTime, speed);
        pb.setBufferedPosition(bufferedTime);

        isPlaying = pbState == PlaybackStateCompat.STATE_PLAYING || pbState == PlaybackStateCompat.STATE_BUFFERING;
        notification.show(nb, isPlaying);

        state = pb.build();
        session.setPlaybackState(state);

        if(remoteVolume) {
            session.setPlaybackToRemote(volume.create(null, maxVol, vol));
        } else {
            session.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
        }
    }

    @ReactMethod
    public void resetNowPlaying() {
        if(!init) return;
        if(artworkThread != null && artworkThread.isAlive()) artworkThread.interrupt();
        artworkThread = null;

        md = new MediaMetadataCompat.Builder();

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
            case "setRating":
                controlValue = PlaybackStateCompat.ACTION_SET_RATING;
                break;
            case "volume":
                volume = volume.create(enable, null, null);
                if(remoteVolume) session.setPlaybackToRemote(volume);
                return;
            case "remoteVolume":
                remoteVolume = enable;
                if(enable) {
                    session.setPlaybackToRemote(volume);
                } else {
                    session.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
                }
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

    private Bitmap loadArtwork(String url, boolean local) {
        Bitmap bitmap = null;

        try {
            if(local) {

                // Gets the drawable from the RN's helper for local resources
                ResourceDrawableIdHelper helper = ResourceDrawableIdHelper.getInstance();
                Drawable image = helper.getResourceDrawable(getReactApplicationContext(), url);

                if(image instanceof BitmapDrawable) {
                    bitmap = ((BitmapDrawable)image).getBitmap();
                } else {
                    bitmap = BitmapFactory.decodeFile(url);
                }

            } else {

                // Open connection to the URL and decodes the image
                URLConnection con = new URL(url).openConnection();
                con.connect();
                InputStream input = con.getInputStream();
                bitmap = BitmapFactory.decodeStream(input);
                input.close();

            }
        } catch(IOException ex) {
            Log.w("MusicControl", "Could not load the artwork", ex);
        }

        return bitmap;
    }

}
