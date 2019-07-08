package com.tanguyantoine.react;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;

import java.util.Map;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;
import static com.tanguyantoine.react.MusicControlModule.CHANNEL_ID;
import static com.tanguyantoine.react.MusicControlModule.NOTIFICATION_ID;

public class MusicControlNotification {

    protected static final String REMOVE_NOTIFICATION = "music_control_remove_notification";
    protected static final String MEDIA_BUTTON = "music_control_media_button";
    protected static final String PACKAGE_NAME = "music_control_package_name";

    private final ReactApplicationContext context;
    private final MusicControlModule module;

    private int smallIcon;
    private int customIcon;
    private NotificationCompat.Action play, pause, stop, next, previous, skipForward, skipBackward;

    public MusicControlNotification(MusicControlModule module, ReactApplicationContext context) {
        this.context = context;
        this.module = module;

        Resources r = context.getResources();
        String packageName = context.getPackageName();

        // Optional custom icon with fallback to the play icon
        smallIcon = r.getIdentifier("music_control_icon", "drawable", packageName);
        if (smallIcon == 0) smallIcon = r.getIdentifier("play", "drawable", packageName);
    }

    public synchronized void setCustomNotificationIcon(String resourceName) {
        if (resourceName == null) {
            customIcon = 0;
            return;
        }

        Resources r = context.getResources();
        String packageName = context.getPackageName();

        customIcon = r.getIdentifier(resourceName, "drawable", packageName);
    }

    public synchronized void updateActions(long mask, Map<String, Integer> options) {
        play = createAction("play", "Play", mask, PlaybackStateCompat.ACTION_PLAY, play);
        pause = createAction("pause", "Pause", mask, PlaybackStateCompat.ACTION_PAUSE, pause);
        stop = createAction("stop", "Stop", mask, PlaybackStateCompat.ACTION_STOP, stop);
        next = createAction("next", "Next", mask, PlaybackStateCompat.ACTION_SKIP_TO_NEXT, next);
        previous = createAction("previous", "Previous", mask, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS, previous);

        if (options != null && options.containsKey("skipForward") && (options.get("skipForward") == 10 || options.get("skipForward") == 5 || options.get("skipForward") == 30)) {
            skipForward = createAction("skip_forward_" + options.get("skipForward").toString(), "Skip Forward", mask, PlaybackStateCompat.ACTION_FAST_FORWARD, skipForward);
        } else {
            skipForward = createAction("skip_forward_10", "Skip Forward", mask, PlaybackStateCompat.ACTION_FAST_FORWARD, skipForward);
        }

        if (options != null && options.containsKey("skipBackward") && (options.get("skipBackward") == 10 || options.get("skipBackward") == 5 || options.get("skipBackward") == 30)) {
            skipBackward = createAction("skip_backward_" + options.get("skipBackward").toString(), "Skip Backward", mask, PlaybackStateCompat.ACTION_REWIND, skipBackward);
        } else {
            skipBackward = createAction("skip_backward_10", "Skip Backward", mask, PlaybackStateCompat.ACTION_REWIND, skipBackward);
        }
    }

    /**
     * NOTE: Synchronized since the NotificationService called prepare without a re-entrant lock.
     *       Other call sites (e.g. show/hide in module) are already synchronized.
     */
    public synchronized Notification prepareNotification(NotificationCompat.Builder builder, boolean isPlaying) {
        // Add the buttons

        builder.mActions.clear();
        if (previous != null) builder.addAction(previous);
        if (skipBackward != null) builder.addAction(skipBackward);
        if (play != null && !isPlaying) builder.addAction(play);
        if (pause != null && isPlaying) builder.addAction(pause);
        if (stop != null) builder.addAction(stop);
        if (next != null) builder.addAction(next);
        if (skipForward != null) builder.addAction(skipForward);

        // Set whether notification can be closed based on closeNotification control (default PAUSED)
        if (module.notificationClose == MusicControlModule.NotificationClose.ALWAYS) {
            builder.setOngoing(false);
        } else if (module.notificationClose == MusicControlModule.NotificationClose.PAUSED) {
            builder.setOngoing(isPlaying);
        } else { // NotificationClose.NEVER
            builder.setOngoing(true);
        }

        builder.setSmallIcon(customIcon != 0 ? customIcon : smallIcon);

        // Open the app when the notification is clicked
        String packageName = context.getPackageName();
        Intent openApp = context.getPackageManager().getLaunchIntentForPackage(packageName);
        builder.setContentIntent(PendingIntent.getActivity(context, 0, openApp, 0));

        // Remove notification
        Intent remove = new Intent(REMOVE_NOTIFICATION);
        remove.putExtra(PACKAGE_NAME, context.getApplicationInfo().packageName);
        builder.setDeleteIntent(PendingIntent.getBroadcast(context, 0, remove, PendingIntent.FLAG_UPDATE_CURRENT));

        return builder.build();
    }

    public synchronized void show(NotificationCompat.Builder builder, boolean isPlaying) {
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, prepareNotification(builder, isPlaying));
    }

    public void hide() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Intent myIntent = new Intent(context, MusicControlNotification.NotificationService.class);
            context.stopService(myIntent);

        }
    }

    /**
     * Code taken from newer version of the support library located in PlaybackStateCompat.toKeyCode
     * Replace this to PlaybackStateCompat.toKeyCode when React Native updates the support library
     */
    private int toKeyCode(long action) {
        if (action == PlaybackStateCompat.ACTION_PLAY) {
            return KeyEvent.KEYCODE_MEDIA_PLAY;
        } else if (action == PlaybackStateCompat.ACTION_PAUSE) {
            return KeyEvent.KEYCODE_MEDIA_PAUSE;
        } else if (action == PlaybackStateCompat.ACTION_SKIP_TO_NEXT) {
            return KeyEvent.KEYCODE_MEDIA_NEXT;
        } else if (action == PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) {
            return KeyEvent.KEYCODE_MEDIA_PREVIOUS;
        } else if (action == PlaybackStateCompat.ACTION_STOP) {
            return KeyEvent.KEYCODE_MEDIA_STOP;
        } else if (action == PlaybackStateCompat.ACTION_FAST_FORWARD) {
            return KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
        } else if (action == PlaybackStateCompat.ACTION_REWIND) {
            return KeyEvent.KEYCODE_MEDIA_REWIND;
        } else if (action == PlaybackStateCompat.ACTION_PLAY_PAUSE) {
            return KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
        }
        return KeyEvent.KEYCODE_UNKNOWN;
    }

    private NotificationCompat.Action createAction(String iconName, String title, long mask, long action, NotificationCompat.Action oldAction) {
        if ((mask & action) == 0) return null; // When this action is not enabled, return null
        if (oldAction != null)
            return oldAction; // If this action was already created, we won't create another instance

        // Finds the icon with the given name
        Resources r = context.getResources();
        String packageName = context.getPackageName();
        int icon = r.getIdentifier(iconName, "drawable", packageName);

        // Creates the intent based on the action
        int keyCode = toKeyCode(action);
        Intent intent = new Intent(MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        intent.putExtra(PACKAGE_NAME, packageName);
        PendingIntent i = PendingIntent.getBroadcast(context, keyCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Action(icon, title, i);
    }

    public static class NotificationService extends Service {

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        private boolean isRunning;
        private Notification notification;

        @Override
        public void onCreate() {
            super.onCreate();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notification = MusicControlModule.INSTANCE.notification.prepareNotification(MusicControlModule.INSTANCE.nb, false);
                startForeground(NOTIFICATION_ID, notification);
                isRunning = true;
            }
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (intent.getAction() != null && intent.getAction().equals("StopService") && notification != null && isRunning) {
                    stopForeground(true);
                    isRunning = false;
                    stopSelf();
                }
            }
            return START_NOT_STICKY;
        }

        @Override
        public void onTaskRemoved(Intent rootIntent) {
            isRunning = false;
            // Destroy the notification and sessions when the task is removed (closed, killed, etc)
            if (MusicControlModule.INSTANCE != null) {
                MusicControlModule.INSTANCE.destroy();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true);
            }
            stopSelf(); // Stop the service as we won't need it anymore
        }

        @Override
        public void onDestroy() {
            isRunning = false;

            if (MusicControlModule.INSTANCE != null) {
                MusicControlModule.INSTANCE.destroy();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true);
            }

            stopSelf();
        }
    }
}
