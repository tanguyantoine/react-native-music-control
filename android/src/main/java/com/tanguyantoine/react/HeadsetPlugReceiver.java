package com.tanguyantoine.react;

import android.content.BroadcastReceiver;
import com.facebook.react.bridge.ReactApplicationContext;
import android.view.KeyEvent;
import android.content.Context;
import android.content.Intent;

public class HeadsetPlugReceiver extends BroadcastReceiver {

  private final MusicControlModule module;
  private final String packageName;
  private final ReactApplicationContext reactContext;

  public HeadsetPlugReceiver(MusicControlModule module, ReactApplicationContext context) {
    this.module = module;
    this.packageName = context.getPackageName();
    this.reactContext = context;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    if (module.session == null) return;
    String action = intent.getAction();

    if (!action.equals(Intent.ACTION_HEADSET_PLUG)) {
      return;
    }
    boolean connectedHeadphones = (intent.getIntExtra("state", 0) == 1);

    // headphones have been unplugged so we should fire pause event
    if (!connectedHeadphones) {
      // Dispatch pause to MusicControlListener
      // Copy of MediaButtonReceiver.handleIntent without action check
      KeyEvent ke = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE);
      module.session.getController().dispatchMediaButtonEvent(ke);
    }
  }
}
