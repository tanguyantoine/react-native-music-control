/**
 * @providesModule MusicControl
 */

import { NativeModules, DeviceEventEmitter, NativeEventEmitter, Platform } from 'react-native'
// @ts-ignore
import resolveAssetSource from 'react-native/Libraries/Image/resolveAssetSource'
// @ts-ignore
import constants from './constants'
import { Command } from './types'

const NativeMusicControl = NativeModules.MusicControlManager
let handlers: { [key in Command]?: (value: any) => void } = {}
let listenerOfNativeMusicControl: any = null
const IS_ANDROID = Platform.OS === 'android'
type TPlayingInfo = any

const MusicControl = {
  STATE_PLAYING: constants.STATE_PLAYING,
  STATE_PAUSED: constants.STATE_PAUSED,
  STATE_ERROR: constants.STATE_ERROR,
  STATE_STOPPED: constants.STATE_STOPPED,
  STATE_BUFFERING: constants.STATE_BUFFERING,

  RATING_HEART: constants.RATING_HEART,
  RATING_THUMBS_UP_DOWN: constants.RATING_THUMBS_UP_DOWN,
  RATING_3_STARS: constants.RATING_3_STARS,
  RATING_4_STARS: constants.RATING_4_STARS,
  RATING_5_STARS: constants.RATING_5_STARS,
  RATING_PERCENTAGE: constants.RATING_PERCENTAGE,

  enableBackgroundMode: function(enable: boolean) {
    NativeMusicControl.enableBackgroundMode(enable)
  },
  setNowPlaying: function(info: TPlayingInfo) {
    // Check if we have an android asset from react style image require
    if (info.artwork) {
      info.artwork = resolveAssetSource(info.artwork) || info.artwork
    }

    NativeMusicControl.setNowPlaying(info)
  },
  setPlayback: function(info: TPlayingInfo): void {
    // Backwards compatibility. Use updatePlayback instead.
    NativeMusicControl.updatePlayback(info)
  },
  updatePlayback: function(info: TPlayingInfo): void {
    NativeMusicControl.updatePlayback(info)
  },
  resetNowPlaying: function() {
    NativeMusicControl.resetNowPlaying()
  },
  enableControl: function(controlName: string, enable: boolean, options = {}) {
    NativeMusicControl.enableControl(controlName, enable, options || {})
  },
  handleCommand: function(commandName: Command, value: any) {
    if (handlers[commandName]) {
      //@ts-ignore
      handlers[commandName](value)
    }
  },
  on: function(actionName: Command, cb: (value: any) => void) {
    if (!listenerOfNativeMusicControl) {
      listenerOfNativeMusicControl = (IS_ANDROID
        ? DeviceEventEmitter
        : new NativeEventEmitter(NativeMusicControl)
      ).addListener('RNMusicControlEvent', event => {
        MusicControl.handleCommand(event.name, event.value)
      })
    }
    handlers[actionName] = cb
  },
  off: function(actionName: Command): void {
    delete handlers[actionName]
    if (!Object.keys(handlers).length && listenerOfNativeMusicControl) {
      listenerOfNativeMusicControl.remove()
      listenerOfNativeMusicControl = null
    }
  },
  stopControl: function(): void {
    if (listenerOfNativeMusicControl) {
      listenerOfNativeMusicControl.remove()
      listenerOfNativeMusicControl = null
    }
    Object.keys(handlers).map(key => {
      //@ts-ignore
      delete handlers[key]
    })
    NativeMusicControl.stopControl()
  },
  handleAudioInterruptions: function(enable: boolean): void {
    NativeMusicControl.observeAudioInterruptions(enable)
  }
}

export default MusicControl
