/**
 * @providesModule MusicControl
 * @flow
 */
'use strict';

import { NativeModules, DeviceEventEmitter } from 'react-native';
const NativeMusicControl = NativeModules.MusicControlManager;

/**
 * High-level docs for the MusicControl iOS API can be written here.
 */
var handlers = { };
var subscription = null;

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

  setPlayback: function (info) {
    // Backwards compatibility. Use updatePlayback instead.
    NativeMusicControl.updatePlayback(info);
  },
  updatePlayback: function(info) {
    NativeMusicControl.updatePlayback(info);
  },
  enableBackgroundMode: function(enable){
    NativeMusicControl.enableBackgroundMode(enable)
  },
  setNowPlaying: function(info){
    NativeMusicControl.setNowPlaying(info)
  },
  resetNowPlaying: function(){
    NativeMusicControl.resetNowPlaying()
  },
  enableControl: function(controlName, bool, options = {}){
    NativeMusicControl.enableControl(controlName, bool, options || {})
  },
  handleCommand: function(commandName){
    if(handlers[commandName]){
      handlers[commandName]()
    }
  },
  on: function(actionName, cb){
    if(subscription){
      subscription.remove();
    }
    subscription = DeviceEventEmitter.addListener(
      'RNMusicControlEvent',
      (event) => {
        MusicControl.handleCommand(event.name)
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
  }
};

module.exports = MusicControl;
