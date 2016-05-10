/**
 * @providesModule MusicControl
 * @flow
 */
'use strict';

import { NativeModules } from 'react-native';
const NativeMusicControl = NativeModules.MusicControlManager;

/**
 * High-level docs for the MusicControl iOS API can be written here.
 */

 console.log(NativeModules, MusicControl);

var MusicControl = {
  setNowPlaying: function(info){
    NativeMusicControl.setNowPlaying(info)
  },
  resetNowPlaying: function(){
    NativeMusicControl.resetNowPlaying()
  },
  enableContol: function(controlName, bool){
    NativeMusicControl.enableContol(controlName, bool)
  },
  handleCommand: function(callback){

  }
};

module.exports = MusicControl;
