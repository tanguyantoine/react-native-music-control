package com.tanguyantoine.react;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableMap;


import java.util.Map;


public class MusicControlModule extends ReactContextBaseJavaModule {
  private ReactContext reactContext;
  public static final String MUSIC_CONTROL_EVENT_NAME = "RNMusicControlEvent";

  public MusicControlModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
     return "MusicControlManager";
  }

  @ReactMethod
  public void enableBackgroundMode(Boolean enable) {
    WritableMap params = Arguments.createMap();
  }

  @ReactMethod
  public void setNowPlaying(ReadableMap infos) {
    String title = infos.getString("title");
    //sendEvent("play);
  }

  @ReactMethod
  public void resetNowPlaying() {
    // TODO
  }

  @ReactMethod
  public void enableContol(String controlName, Boolean enabled) {
    // TODO
  }

  private void sendEvent(String eventName) {
    WritableMap params = Arguments.createMap();
    params.putString("name", eventName);
    reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(MUSIC_CONTROL_EVENT_NAME, params);
  }

}
