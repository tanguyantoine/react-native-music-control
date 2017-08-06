/**
 * @providesModule MusicControl
 * @flow
 */
'use strict';

import { NativeModules, DeviceEventEmitter, Platform } from 'react-native';
const NativeMusicControl = NativeModules.MusicControlManager;
import resolveAssetSource from 'react-native/Libraries/Image/resolveAssetSource';

var handlers = { };
var subscription = null;

var ios = Platform.OS === 'ios';

var MusicControl = {

  STATE_ERROR: ios ? NativeMusicControl.STATE_PAUSED : NativeMusicControl.STATE_ERROR,
  STATE_STOPPED: NativeMusicControl.STATE_STOPPED,
  STATE_PLAYING: NativeMusicControl.STATE_PLAYING,
  STATE_PAUSED: NativeMusicControl.STATE_PAUSED,
  STATE_STOPPED: ios ? NativeMusicControl.STATE_PAUSED : NativeMusicControl.STATE_STOPPED,
  STATE_BUFFERING: ios ? NativeMusicControl.STATE_PAUSED : NativeMusicControl.STATE_BUFFERING,

  RATING_HEART: ios ? 0 : NativeMusicControl.RATING_HEART,
  RATING_THUMBS_UP_DOWN: ios ? 0 : NativeMusicControl.RATING_THUMBS_UP_DOWN,
  RATING_3_STARS: ios ? 0 : NativeMusicControl.RATING_3_STARS,
  RATING_4_STARS: ios ? 0 : NativeMusicControl.RATING_4_STARS,
  RATING_5_STARS: ios ? 0 : NativeMusicControl.RATING_5_STARS,
  RATING_PERCENTAGE: ios ? 0 : NativeMusicControl.RATING_PERCENTAGE,

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
  enableControl: function(controlName, bool, options = {}){
    NativeMusicControl.enableControl(controlName, bool, options || {})
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
  }
};

module.exports = MusicControl;
