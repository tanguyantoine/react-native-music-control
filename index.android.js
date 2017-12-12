/**
 * @providesModule MusicControl
 * @flow
 */
'use strict';

import { NativeModules, DeviceEventEmitter } from 'react-native';
const NativeMusicControl = NativeModules.MusicControlManager;
import resolveAssetSource from 'react-native/Libraries/Image/resolveAssetSource';

var handlers = { };
var subscription = null;

var MusicControl = {

  STATE_ERROR: NativeMusicControl.STATE_ERROR,
  STATE_STOPPED: NativeMusicControl.STATE_STOPPED,
  STATE_PLAYING: NativeMusicControl.STATE_PLAYING,
  STATE_PAUSED: NativeMusicControl.STATE_PAUSED,
  STATE_BUFFERING: NativeMusicControl.STATE_BUFFERING,

  RATING_HEART: NativeMusicControl.RATING_HEART,
  RATING_THUMBS_UP_DOWN: NativeMusicControl.RATING_THUMBS_UP_DOWN,
  RATING_3_STARS: NativeMusicControl.RATING_3_STARS,
  RATING_4_STARS: NativeMusicControl.RATING_4_STARS,
  RATING_5_STARS: NativeMusicControl.RATING_5_STARS,
  RATING_PERCENTAGE: NativeMusicControl.RATING_PERCENTAGE,

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
    NativeMusicControl.enableControl(controlName, enable, options)
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
    subscription = DeviceEventEmitter.addListener(
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
