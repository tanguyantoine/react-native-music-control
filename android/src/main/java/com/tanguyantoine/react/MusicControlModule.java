package com.tanguyantoine.react;

import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import androidx.core.content.ContextCompat;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.SystemClock;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
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
import java.util.Locale;
import java.util.Map;

public class MusicControlModule extends ReactContextBaseJavaModule implements ComponentCallbacks2 {
    private static final String TAG = MusicControlModule.class.getSimpleName();

    static MusicControlModule INSTANCE;

    private boolean init = false;
    protected MediaSessionCompat session;

    private MediaMetadataCompat.Builder md;
    private PlaybackStateCompat.Builder pb;
    public NotificationCompat.Builder nb;


    private PlaybackStateCompat state;

    public MusicControlNotification notification;
    private MusicControlVolumeListener volume;
    private MusicControlReceiver receiver;
    private MusicControlEventEmitter emitter;
    private MusicControlAudioFocusListener afListener;

    private Thread artworkThread;

    public ReactApplicationContext context;

    private boolean remoteVolume = false;
    private boolean isPlaying = false;
    private long controls = 0;
    private NotificationChannel notificationChannel;
    protected int ratingType = RatingCompat.RATING_PERCENTAGE;

    public NotificationClose notificationClose = NotificationClose.PAUSED;

    public final static String DEFAULT_CHANNEL_ID = "react-native-music-control";
    public final static int DEFAULT_NOTIFICATION_ID = 100;

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

        map.put("RATING_HEART", RatingCompat.RATING_HEART);
        map.put("RATING_THUMBS_UP_DOWN", RatingCompat.RATING_THUMB_UP_DOWN);
        map.put("RATING_3_STARS", RatingCompat.RATING_3_STARS);
        map.put("RATING_4_STARS", RatingCompat.RATING_4_STARS);
        map.put("RATING_5_STARS", RatingCompat.RATING_5_STARS);
        map.put("RATING_PERCENTAGE", RatingCompat.RATING_PERCENTAGE);
        return map;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel(ReactApplicationContext context, String channelId) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationChannel != null) {
             if(notificationChannel.getId().equals(channelId)) {
                 return;
             }
             mNotificationManager.deleteNotificationChannel(notificationChannel.getId());
        }

        NotificationChannel mChannel = new NotificationChannel(channelId, "Media playback", NotificationManager.IMPORTANCE_LOW);
        mChannel.setDescription("Media playback controls");
        mChannel.setShowBadge(false);
        mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        mNotificationManager.createNotificationChannel(mChannel);
        this.notificationChannel = mChannel;
    }

    private boolean hasControl(long control) {
        if((controls & control) == control) {
            return true;
        }
        return false;
    }

    private void updateNotificationMediaStyle() {
        if (!Build.MANUFACTURER.toLowerCase(Locale.getDefault()).contains("huawei") && Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            MediaStyle style = new MediaStyle();
            style.setMediaSession(session.getSessionToken());
            int controlCount = 0;
            if(hasControl(PlaybackStateCompat.ACTION_PLAY) || hasControl(PlaybackStateCompat.ACTION_PAUSE) || hasControl(PlaybackStateCompat.ACTION_PLAY_PAUSE)) {
                controlCount += 1;
            }
            if(hasControl(PlaybackStateCompat.ACTION_SKIP_TO_NEXT)) {
                controlCount += 1;
            }
            if(hasControl(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)) {
                controlCount += 1;
            }
            if(hasControl(PlaybackStateCompat.ACTION_FAST_FORWARD)) {
                controlCount += 1;
            }
            if(hasControl(PlaybackStateCompat.ACTION_REWIND)) {
                controlCount += 1;
            }
            int[] actions = new int[controlCount];
            for(int i=0; i<actions.length; i++) {
                actions[i] = i;
            }
            style.setShowActionsInCompactView(actions);
            nb.setStyle(style);
        }
    }

    public void init() {
        if (init) return;

        INSTANCE = this;

        context = getReactApplicationContext();

        emitter = new MusicControlEventEmitter(context);

        session = new MediaSessionCompat(context, "MusicControl");
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        session.setCallback(new MediaSessionCallback(emitter));

        volume = new MusicControlVolumeListener(context, emitter, true, 100, 100);
        if(remoteVolume) {
            session.setPlaybackToRemote(volume);
        } else {
            session.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
        }

        md = new MediaMetadataCompat.Builder();
        pb = new PlaybackStateCompat.Builder();
        pb.setActions(controls);

        nb = new NotificationCompat.Builder(context, CHANNEL_ID);
        nb.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
         nb.setPriority(NotificationCompat.PRIORITY_HIGH);

        updateNotificationMediaStyle();

        state = pb.build();

        notification = new MusicControlNotification(this, context);
        notification.updateActions(controls, null);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicControlNotification.REMOVE_NOTIFICATION);
        filter.addAction(MusicControlNotification.MEDIA_BUTTON);
        filter.addAction(Intent.ACTION_MEDIA_BUTTON);
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        receiver = new MusicControlReceiver(this, context);
        context.registerReceiver(receiver, filter);

        Intent myIntent = new Intent(context, MusicControlNotification.NotificationService.class);

        afListener = new MusicControlAudioFocusListener(context, emitter, volume);

        ContextCompat.startForegroundService(context, myIntent);
        context.registerComponentCallbacks(this);

        isPlaying = false;
        init = true;
    }

    @ReactMethod
    public synchronized void stopControl() {
        if (!init)
            return;

        if (notification != null)
            notification.hide();
        session.release();

        ReactApplicationContext context = getReactApplicationContext();

        context.unregisterReceiver(receiver);
        context.unregisterComponentCallbacks(this);

        if (artworkThread != null && artworkThread.isAlive())
            artworkThread.interrupt();
        artworkThread = null;

        session = null;
        notification = null;
        volume = null;
        receiver = null;
        state = null;
        md = null;
        pb = null;
        nb = null;

        init = false;
    }

    synchronized public void destroy() {
        stopControl();
    }

    @ReactMethod
    public void enableBackgroundMode(boolean enable) {
        // Nothing?
    }

    @ReactMethod
    public void observeAudioInterruptions(boolean enable) {
        if (enable) {
            afListener.requestAudioFocus();
        } else {
            afListener.abandonAudioFocus();
        }
    }

    @ReactMethod
    synchronized public void setNowPlaying(ReadableMap metadata) {
        init();
        if(artworkThread != null && artworkThread.isAlive()) artworkThread.interrupt();

        final int notificationId = metadata.hasKey("notificationId") ? metadata.getInt("notificationId") : DEFAULT_NOTIFICATION_ID;
        emitter.setNotificationId(notificationId);
        notification.setNotificationId(notificationId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = metadata.hasKey("channelId") ? metadata.getString("channelId") : DEFAULT_CHANNEL_ID;
            createChannel(context, channelId);
        }

        String title = metadata.hasKey("title") ? metadata.getString("title") : null;
        String artist = metadata.hasKey("artist") ? metadata.getString("artist") : null;
        String album = metadata.hasKey("album") ? metadata.getString("album") : null;
        String genre = metadata.hasKey("genre") ? metadata.getString("genre") : null;
        String description = metadata.hasKey("description") ? metadata.getString("description") : null;
        String date = metadata.hasKey("date") ? metadata.getString("date") : null;
        long duration = metadata.hasKey("duration") ? (long)(metadata.getDouble("duration") * 1000) : 0;
        int notificationColor = metadata.hasKey("color") ? metadata.getInt("color") : NotificationCompat.COLOR_DEFAULT;
        String notificationIcon = metadata.hasKey("notificationIcon") ? metadata.getString("notificationIcon") : null;

        // If a color is supplied, we need to clear the MediaStyle set during init().
        // Otherwise, the color will not be used for the notification's background.
        boolean removeFade = metadata.hasKey("color");
        if(removeFade) {
            nb.setStyle(new MediaStyle());
        }

        RatingCompat rating;
        if(metadata.hasKey("rating")) {
            if(ratingType == RatingCompat.RATING_PERCENTAGE) {
                rating = RatingCompat.newPercentageRating((float)metadata.getDouble("rating"));
            } else if(ratingType == RatingCompat.RATING_HEART) {
                rating = RatingCompat.newHeartRating(metadata.getBoolean("rating"));
            } else if(ratingType == RatingCompat.RATING_THUMB_UP_DOWN) {
                rating = RatingCompat.newThumbRating(metadata.getBoolean("rating"));
            } else {
                rating = RatingCompat.newStarRating(ratingType, (float)metadata.getDouble("rating"));
            }
        } else {
            rating = RatingCompat.newUnratedRating(ratingType);
        }

        md.putText(MediaMetadataCompat.METADATA_KEY_TITLE, title);
        md.putText(MediaMetadataCompat.METADATA_KEY_ARTIST, artist);
        md.putText(MediaMetadataCompat.METADATA_KEY_ALBUM, album);
        md.putText(MediaMetadataCompat.METADATA_KEY_GENRE, genre);
        md.putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, description);
        md.putText(MediaMetadataCompat.METADATA_KEY_DATE, date);
        md.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);
        if (android.os.Build.VERSION.SDK_INT > 19) {
            md.putRating(MediaMetadataCompat.METADATA_KEY_RATING, rating);
        }

        nb.setContentTitle(title);
        nb.setContentText(artist);
        nb.setContentInfo(album);
        nb.setColor(notificationColor);

        notification.setCustomNotificationIcon(notificationIcon);

        if(metadata.hasKey("artwork")) {
            String artwork = null;
            boolean localArtwork = false;

            if(metadata.getType("artwork") == ReadableType.Map) {
                artwork = metadata.getMap("artwork").getString("uri");
                localArtwork = true;
            } else {
                artwork = metadata.getString("artwork");
            }

            final String artworkUrl = artwork;
            final boolean artworkLocal = localArtwork;

            artworkThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = loadArtwork(artworkUrl, artworkLocal);

                    if(md != null) {
                        md.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap);
                        session.setMetadata(md.build());
                    }
                    if(nb != null) {
                        nb.setLargeIcon(bitmap);
                        notification.show(nb, isPlaying);
                    }

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
    synchronized public void updatePlayback(ReadableMap info) {
        init();

        long updateTime;
        long elapsedTime;
        long bufferedTime = info.hasKey("bufferedTime") ? (long)(info.getDouble("bufferedTime") * 1000) : state.getBufferedPosition();
        float speed = info.hasKey("speed") ? (float)info.getDouble("speed") : state.getPlaybackSpeed();
        int pbState = info.hasKey("state") ? info.getInt("state") : state.getState();
        int maxVol = info.hasKey("maxVolume") ? info.getInt("maxVolume") : volume.getMaxVolume();
        int vol = info.hasKey("volume") ? info.getInt("volume") : volume.getCurrentVolume();
        ratingType = info.hasKey("rating") ? info.getInt("rating") : ratingType;

        if(info.hasKey("elapsedTime")) {
            elapsedTime = (long)(info.getDouble("elapsedTime") * 1000);
            updateTime = SystemClock.elapsedRealtime();
        } else {
            elapsedTime = state.getPosition();
            updateTime = state.getLastPositionUpdateTime();
        }

        pb.setState(pbState, elapsedTime, speed, updateTime);
        pb.setBufferedPosition(bufferedTime);
        pb.setActions(controls);

        isPlaying = pbState == PlaybackStateCompat.STATE_PLAYING || pbState == PlaybackStateCompat.STATE_BUFFERING;
        if(session.isActive()) notification.show(nb, isPlaying);

        state = pb.build();
        session.setPlaybackState(state);

        session.setRatingType(ratingType);

        if(remoteVolume) {
            session.setPlaybackToRemote(volume.create(null, maxVol, vol));
        } else {
            session.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
        }
    }

    @ReactMethod
    synchronized public void resetNowPlaying() {
        if(!init) return;
        if(artworkThread != null && artworkThread.isAlive()) artworkThread.interrupt();
        artworkThread = null;

        md = new MediaMetadataCompat.Builder();

        if (notification != null) notification.hide();
        session.setActive(false);
    }

    @ReactMethod
    synchronized public void enableControl(String control, boolean enable, ReadableMap options) {
        init();

        Map<String, Integer> skipOptions = new HashMap<String, Integer>();

        long controlValue;
        switch(control) {
            case "skipForward":
                if (options.hasKey("interval"))
                    skipOptions.put("skipForward", options.getInt("interval"));
                controlValue = PlaybackStateCompat.ACTION_FAST_FORWARD;
                break;
            case "skipBackward":
                if (options.hasKey("interval"))
                    skipOptions.put("skipBackward", options.getInt("interval"));
                controlValue = PlaybackStateCompat.ACTION_REWIND;
                break;
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
                return;
            case "closeNotification":
                if(enable) {
                    if (options.hasKey("when")) {
                        if ("always".equals(options.getString("when"))) {
                            this.notificationClose = notificationClose.ALWAYS;
                        }else if ("paused".equals(options.getString("when"))) {
                            this.notificationClose = notificationClose.PAUSED;
                        }else {
                            this.notificationClose = notificationClose.NEVER;
                        }
                    }
                    return;
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

        notification.updateActions(controls, skipOptions);
        pb.setActions(controls);

        state = pb.build();
        session.setPlaybackState(state);

        updateNotificationMediaStyle();

        if(session.isActive()) {
            notification.show(nb, isPlaying);
        }
    }

    private Bitmap loadArtwork(String url, boolean local) {
        Bitmap bitmap = null;

        try {
            // If we are running the app in debug mode, the "local" image will be served from htt://localhost:8080, so we need to check for this case and load those images from URL
            if(local && !url.startsWith("http")) {
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
        } catch(IOException | IndexOutOfBoundsException ex) {
            Log.w(TAG, "Could not load the artwork", ex);
        }

        return bitmap;
    }

    @Override
    public void onTrimMemory(int level) {
        switch(level) {
            // Trims memory when it reaches a moderate level and the session is inactive
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
                if(session.isActive()) break;

                // Trims memory when it reaches a critical level
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:

                Log.w(TAG, "Control resources are being removed due to system's low memory (Level: " + level + ")");
                destroy();
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

    }

    @Override
    public void onLowMemory() {
        Log.w(TAG, "Control resources are being removed due to system's low memory (Level: MEMORY_COMPLETE)");
        destroy();
    }

    public enum NotificationClose {
        ALWAYS,
        PAUSED,
        NEVER
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}
