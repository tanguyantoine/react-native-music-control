/**
 * @providesModule MusicControl
 * @flow
 */
'use strict';

import { NativeModules, NativeEventEmitter } from 'react-native';
const NativeMusicControl = NativeModules.MusicControlManager;
import resolveAssetSource from 'react-native/Libraries/Image/resolveAssetSource';

/**
 * High-level docs for the MusicControl iOS API can be written here.
 */
var handlers = {};
var listenerOfNativeMusicControl = null;

var formatInfo = function(info){
  // NOTE: CHECK IF WE HAVE AN IOS ASSET FROM REACT STYLE IMAGE REQUIRE
  if(info.artwork) {
    info.artwork = resolveAssetSource(info.artwork) || info.artwork;
  }
  return info
}

var MusicControl = {

  STATE_PLAYING: NativeMusicControl.STATE_PLAYING,
  STATE_PAUSED: NativeMusicControl.STATE_PAUSED,
  STATE_ERROR: NativeMusicControl.STATE_PAUSED,
  STATE_STOPPED: NativeMusicControl.STATE_PAUSED,
  STATE_BUFFERING: NativeMusicControl.STATE_PAUSED,

  // Rating is not supported on iOS. This is kept here for compatibility
  RATING_HEART: 0,
  RATING_THUMBS_UP_DOWN: 0,
  RATING_3_STARS: 0,
  RATING_4_STARS: 0,
  RATING_5_STARS: 0,
  RATING_PERCENTAGE: 0,

  // NOTE: BACKWARDS COMPATIBILITY. USE "updatePlayback" INSTEAD.
  setPlayback: function (info) {
    info = formatInfo(info)
    NativeMusicControl.updatePlayback(info);
  },
  updatePlayback: function(info) {
    info = formatInfo(info)
    NativeMusicControl.updatePlayback(info);
  },
  enableBackgroundMode: function(enable){
    NativeMusicControl.enableBackgroundMode(enable)
  },
  setNowPlaying: function(info){
    info = formatInfo(info)
    NativeMusicControl.setNowPlaying(info);
  },
  resetNowPlaying: function(){
    NativeMusicControl.resetNowPlaying()
  },
  enableControl: function(controlName, bool, options = {}){
    NativeMusicControl.enableControl(controlName, bool, options || {})
  },
  handleCommand: function(commandName, value){
    if(handlers[commandName]){
      handlers[commandName](value)
    }
  },
  on: function(actionName, cb){
    if ( listenerOfNativeMusicControl == null ) {
      listenerOfNativeMusicControl = new NativeEventEmitter(NativeMusicControl).addListener(
        'RNMusicControlEvent',
        (event) => {
          MusicControl.handleCommand(event.name, event.value)
        }
      );
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
