package com.tanguyantoine.react;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class MusicControlReceiver extends BroadcastReceiver {

    private final MusicControlModule module;
    private final String packageName;

    public MusicControlReceiver(MusicControlModule module, String packageName) {
        this.module = module;
        this.packageName = packageName;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(module.session == null || module.notification == null) return;
        String action = intent.getAction();

        if(MusicControlNotification.REMOVE_NOTIFICATION.equals(action)) {

            if(!checkApp(intent)) return;

            module.notification.hide();
            module.session.setActive(false);

        } else if(MusicControlNotification.MEDIA_BUTTON.equals(action) || Intent.ACTION_MEDIA_BUTTON.equals(action)) {

            if(!intent.hasExtra(Intent.EXTRA_KEY_EVENT)) return;
            if(!checkApp(intent)) return;

            // Replace this to MediaButtonReceiver.handleIntent when React Native updates the support library
            KeyEvent ke = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            module.session.getController().dispatchMediaButtonEvent(ke);

        }
    }

    private boolean checkApp(Intent intent) {
        if(intent.hasExtra(MusicControlNotification.PACKAGE_NAME)) {
            String name = intent.getStringExtra(MusicControlNotification.PACKAGE_NAME);
            if(!packageName.equals(name)) return false; // This event is not for this package. We'll ignore it
        }
        return true;
    }

}
