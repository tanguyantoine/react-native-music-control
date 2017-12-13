/**
 * @providesModule MusicControl
 * @flow
 */
'use strict';

import { NativeModules, DeviceEventEmitter, NativeEventEmitter, Platform } from 'react-native';
const NativeMusicControl = NativeModules.MusicControlManager;
import resolveAssetSource from 'react-native/Libraries/Image/resolveAssetSource';

var handlers = { };
var subscription = null;
var IS_ANDROID = Platform.OS === 'android';

var MusicControl = {

  STATE_PLAYING: NativeMusicControl.STATE_PLAYING,
  STATE_PAUSED: NativeMusicControl.STATE_PAUSED,
  STATE_ERROR: IS_ANDROID ? NativeMusicControl.STATE_ERROR : NativeMusicControl.STATE_PAUSED,
  STATE_STOPPED: IS_ANDROID ? NativeMusicControl.STATE_STOPPED : NativeMusicControl.STATE_PAUSED,
  STATE_BUFFERING: IS_ANDROID ? NativeMusicControl.STATE_BUFFERING : NativeMusicControl.STATE_PAUSED,

  RATING_HEART: IS_ANDROID ? NativeMusicControl.RATING_HEART : 0,
  RATING_THUMBS_UP_DOWN: IS_ANDROID ? NativeMusicControl.RATING_THUMBS_UP_DOWN : 0,
  RATING_3_STARS: IS_ANDROID ? NativeMusicControl.RATING_3_STARS : 0,
  RATING_4_STARS: IS_ANDROID ? NativeMusicControl.RATING_4_STARS : 0,
  RATING_5_STARS: IS_ANDROID ? NativeMusicControl.RATING_5_STARS : 0,
  RATING_PERCENTAGE: IS_ANDROID ? NativeMusicControl.RATING_PERCENTAGE : 0,

  enableBackgroundMode: function(enable){
    NativeMusicControl.enableBackgroundMode(enable)
  },
  setNowPlaying: function(info){
    // Check if we have an android asset from react style image require
    if(info.artwork) {
        info.artwork = resolveAssetSource(info.artwork) || info.artwork;
    }

    NativeMusicControl.setNowPlaying(info);
  },
  setPlayback: function(info){
    // Backwards compatibility. Use updatePlayback instead.
    NativeMusicControl.updatePlayback(info)
  },
  updatePlayback: function(info){
    NativeMusicControl.updatePlayback(info)
  },
  resetNowPlaying: function(){
    NativeMusicControl.resetNowPlaying()
  },
  enableControl: function(controlName, enable, options = {}){
    NativeMusicControl.enableControl(controlName, enable, options || {})
  },
  handleCommand: function(commandName, value){
    if(handlers[commandName]){
      handlers[commandName](value)
    }
  },
  on: function(actionName, cb){
    if(subscription){
      subscription.remove();
    }
    subscription = (IS_ANDROID ? DeviceEventEmitter : new NativeEventEmitter(NativeMusicControl))
      .addListener(
        'RNMusicControlEvent',
        (event) => {
          MusicControl.handleCommand(event.name, event.value)
        }
      );
    handlers[actionName] = cb
  },
  off: function(actionName, cb){
    delete(handlers[actionName])
    if(!Object.keys(handlers).length && subscription){
      subscription.remove()
      subscription = null;
    }
  },
  stopControl: function() {
    if (subscription) {
      subscription.remove();
    }
    subscription = null;
    handlers = {};
    NativeMusicControl.stopControl();
  }
};

export default MusicControl;
