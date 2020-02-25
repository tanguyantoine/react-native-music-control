"use strict";
/**
 * @providesModule MusicControl
 */
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
var react_native_1 = require("react-native");
// @ts-ignore
var resolveAssetSource_1 = __importDefault(require("react-native/Libraries/Image/resolveAssetSource"));
// @ts-ignore
var constants_1 = __importDefault(require("./constants"));
var NativeMusicControl = react_native_1.NativeModules.MusicControlManager;
var handlers = {};
var listenerOfNativeMusicControl = null;
var IS_ANDROID = react_native_1.Platform.OS === 'android';
var MusicControl = {
    STATE_PLAYING: constants_1.default.STATE_PLAYING,
    STATE_PAUSED: constants_1.default.STATE_PAUSED,
    STATE_ERROR: constants_1.default.STATE_ERROR,
    STATE_STOPPED: constants_1.default.STATE_STOPPED,
    STATE_BUFFERING: constants_1.default.STATE_BUFFERING,
    RATING_HEART: constants_1.default.RATING_HEART,
    RATING_THUMBS_UP_DOWN: constants_1.default.RATING_THUMBS_UP_DOWN,
    RATING_3_STARS: constants_1.default.RATING_3_STARS,
    RATING_4_STARS: constants_1.default.RATING_4_STARS,
    RATING_5_STARS: constants_1.default.RATING_5_STARS,
    RATING_PERCENTAGE: constants_1.default.RATING_PERCENTAGE,
    enableBackgroundMode: function (enable) {
        NativeMusicControl.enableBackgroundMode(enable);
    },
    setNowPlaying: function (info) {
        // Check if we have an android asset from react style image require
        if (info.artwork) {
            info.artwork = resolveAssetSource_1.default(info.artwork) || info.artwork;
        }
        NativeMusicControl.setNowPlaying(info);
    },
    setPlayback: function (info) {
        // Backwards compatibility. Use updatePlayback instead.
        NativeMusicControl.updatePlayback(info);
    },
    updatePlayback: function (info) {
        NativeMusicControl.updatePlayback(info);
    },
    resetNowPlaying: function () {
        NativeMusicControl.resetNowPlaying();
    },
    enableControl: function (controlName, enable, options) {
        if (options === void 0) { options = {}; }
        NativeMusicControl.enableControl(controlName, enable, options || {});
    },
    handleCommand: function (commandName, value) {
        if (handlers[commandName]) {
            //@ts-ignore
            handlers[commandName](value);
        }
    },
    on: function (actionName, cb) {
        if (!listenerOfNativeMusicControl) {
            listenerOfNativeMusicControl = (IS_ANDROID
                ? react_native_1.DeviceEventEmitter
                : new react_native_1.NativeEventEmitter(NativeMusicControl)).addListener('RNMusicControlEvent', function (event) {
                MusicControl.handleCommand(event.name, event.value);
            });
        }
        handlers[actionName] = cb;
    },
    off: function (actionName) {
        delete handlers[actionName];
        if (!Object.keys(handlers).length && listenerOfNativeMusicControl) {
            listenerOfNativeMusicControl.remove();
            listenerOfNativeMusicControl = null;
        }
    },
    stopControl: function () {
        if (listenerOfNativeMusicControl) {
            listenerOfNativeMusicControl.remove();
            listenerOfNativeMusicControl = null;
        }
        Object.keys(handlers).map(function (key) {
            //@ts-ignore
            delete handlers[key];
        });
        NativeMusicControl.stopControl();
    },
    handleAudioInterruptions: function (enable) {
        NativeMusicControl.observeAudioInterruptions(enable);
    }
};
exports.default = MusicControl;
