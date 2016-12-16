package com.tanguyantoine.react;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.KeyEvent;

public class MusicControlReceiver extends BroadcastReceiver {

    private final MusicControlNotification notification;
    private final MediaSessionCompat session;

    public MusicControlReceiver(MusicControlNotification notification, MediaSessionCompat session) {
        this.notification = notification;
        this.session = session;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(MusicControlNotification.REMOVE_NOTIFICATION)) {

            notification.hide();
            session.setActive(false);

        } else if(Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction()) && intent.hasExtra(Intent.EXTRA_KEY_EVENT)) {

            // Replace this to MediaButtonReceiver.handleIntent when React Native updates the support library
            KeyEvent ke = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            session.getController().dispatchMediaButtonEvent(ke);

        }
    }

}
