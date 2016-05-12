/**
 * Stub of MusicControl for Android.
 *
 * @providesModule MusicControl
 * @flow
 */
'use strict';

import { NativeModules, DeviceEventEmitter } from 'react-native';
const NativeMusicControl = NativeModules.MusicControlManager;
var warning = require('fbjs/lib/warning');

var handlers = { };
var subscription = null;

var MusicControl = {
  enableBackgroundMode: function(enable){
    warning(true, 'Not yet implemented for Android.');
    // NativeMusicControl.enableBackgroundMode(enable)
  },
  setNowPlaying: function(info){
    warning(true, 'Not yet implemented for Android.');
    // NativeMusicControl.setNowPlaying(info)
  },
  resetNowPlaying: function(){
    warning(true, 'Not yet implemented for Android.');
    // NativeMusicControl.resetNowPlaying()
  },
  enableContol: function(controlName, bool){
    warning(true, 'Not yet implemented for Android.');
    // NativeMusicControl.enableContol(controlName, bool)
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
