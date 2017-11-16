/**
 * @providesModule MusicControl
 * @flow
 */
'use strict';

import {
  NativeModules,
  NativeEventEmitter,
  DeviceEventEmitter,
  Platform
} from 'react-native';
const NativeMusicControl = NativeModules.MusicControlManager;
import resolveAssetSource from 'react-native/Libraries/Image/resolveAssetSource';
const IS_ANDROID = Platform.OS === 'android';

/**
 * High-level docs for the MusicControl iOS API can be written here.
 */
var handlers = {};
var listenerOfNativeMusicControl = null;
const RN_MUSIC_CONTROL_EVENT_NAME = 'RNMusicControlEvent';

var formatInfo = function(info){
  // NOTE: CHECK IF WE HAVE AN IOS ASSET FROM REACT STYLE IMAGE REQUIRE
  if(info.artwork) {
    info.artwork = resolveAssetSource(info.artwork) || info.artwork;
  }
  return info;
}

var handleRNMusicControlEvent = function(event){
  MusicControl.handleCommand(event.name, event.value);
}

var MusicControl = {

  STATE_PLAYING: NativeMusicControl.STATE_PLAYING,
  STATE_PAUSED: NativeMusicControl.STATE_PAUSED,
  STATE_ERROR: NativeMusicControl.STATE_PAUSED,
  STATE_STOPPED: NativeMusicControl.STATE_PAUSED,
  STATE_BUFFERING: NativeMusicControl.STATE_PAUSED,

  // Rating is not supported on iOS. This is kept here for compatibility
  RATING_HEART: IS_ANDROID ? NativeMusicControl.RATING_HEART : 0,
  RATING_THUMBS_UP_DOWN: IS_ANDROID ? NativeMusicControl.RATING_THUMBS_UP_DOWN : 0,
  RATING_3_STARS: IS_ANDROID ? NativeMusicControl.RATING_3_STARS : 0,
  RATING_4_STARS: IS_ANDROID ? NativeMusicControl.RATING_4_STARS : 0,
  RATING_5_STARS: IS_ANDROID ? NativeMusicControl.RATING_5_STARS : 0,
  RATING_PERCENTAGE: IS_ANDROID ? NativeMusicControl.RATING_PERCENTAGE : 0,

  // NOTE: BACKWARDS COMPATIBILITY. USE "updatePlayback" INSTEAD.
  setPlayback: function (info) {
    NativeMusicControl.updatePlayback( formatInfo(info) );
  },
  updatePlayback: function(info) {
    NativeMusicControl.updatePlayback( formatInfo(info) );
  },
  enableBackgroundMode: function(enable){
    NativeMusicControl.enableBackgroundMode(enable);
  },
  setNowPlaying: function(info){
    NativeMusicControl.setNowPlaying( formatInfo(info) );
  },
  resetNowPlaying: function(){
    NativeMusicControl.resetNowPlaying()
  },
  enableControl: function(controlName, bool, options = {}){
    NativeMusicControl.enableControl(controlName, bool, options)
  },
  handleCommand: function(commandName, value){
    if(handlers[commandName]){
      handlers[commandName](value)
    }
  },
  on: function(actionName, cb){
    if ( listenerOfNativeMusicControl == null ) {
      if ( IS_ANDROID ) {
        listenerOfNativeMusicControl = DeviceEventEmitter.addListener(
          RN_MUSIC_CONTROL_EVENT_NAME,
          handleRNMusicControlEvent
        );
      } else {
        listenerOfNativeMusicControl = new NativeEventEmitter(NativeMusicControl).addListener(
          RN_MUSIC_CONTROL_EVENT_NAME,
          handleRNMusicControlEvent
        );
      }
    }
    handlers[actionName] = cb
  },
  off: function(actionName){
    delete( handlers[actionName] )
    if ( !Object.keys(handlers).length > 0 && listenerOfNativeMusicControl != null ) {
      listenerOfNativeMusicControl.remove()
      listenerOfNativeMusicControl = null
    }
  }
};

export default MusicControl;
