package com.tanguyantoine.react;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class MusicControlReceiver extends BroadcastReceiver {

    private final MusicControlModule module;
    public MusicControlReceiver(MusicControlModule module) {
        this.module = module;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(module.session == null || module.notification == null) return;
        String action = intent.getAction();

        if(MusicControlNotification.REMOVE_NOTIFICATION.equals(action)) {

            module.notification.hide();
            module.session.setActive(false);

        } else if(MusicControlNotification.MEDIA_BUTTON.equals(action) || Intent.ACTION_MEDIA_BUTTON.equals(action)) {

            if(!intent.hasExtra(Intent.EXTRA_KEY_EVENT)) return;
            // Replace this to MediaButtonReceiver.handleIntent when React Native updates the support library
            KeyEvent ke = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            module.session.getController().dispatchMediaButtonEvent(ke);

        }
    }

}
