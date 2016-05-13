
package com.tanguyantoine.react;

import android.app.Activity;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

public class MusicControlPackage implements ReactPackage {
  Activity mActivity;
  MusicControlModule musicControl;

  public MusicControlPackage(Activity activity) {
    mActivity = activity;
  }

  @Override
  public List<NativeModule> createNativeModules (ReactApplicationContext context) {
    List<NativeModule> modules = new ArrayList<>();
    musicControl = new MusicControlModule(context, mActivity);
    modules.add(musicControl);
    return modules;
  }

  @Override
  public List<Class<? extends JavaScriptModule>> createJSModules() {
    return Collections.emptyList();
  }

  @Override
  public List<ViewManager> createViewManagers(ReactApplicationContext context) {
    return Collections.emptyList();
  }

  public void newIntent(Intent intent){
    musicControl.handleIntent(intent);
  }
}