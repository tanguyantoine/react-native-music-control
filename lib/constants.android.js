"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var react_native_1 = require("react-native");
var NativeMusicControl = react_native_1.NativeModules.MusicControlManager;
var STATE_PLAYING = NativeMusicControl.STATE_PLAYING;
var STATE_PAUSED = NativeMusicControl.STATE_PAUSED;
var STATE_ERROR = NativeMusicControl.STATE_PAUSED;
var STATE_STOPPED = NativeMusicControl.STATE_PAUSED;
var STATE_BUFFERING = NativeMusicControl.STATE_PAUSED;
var RATING_HEART = 0;
var RATING_THUMBS_UP_DOWN = 0;
var RATING_3_STARS = 0;
var RATING_4_STARS = 0;
var RATING_5_STARS = 0;
var RATING_PERCENTAGE = 0;
exports.default = {
    STATE_PLAYING: STATE_PLAYING,
    STATE_PAUSED: STATE_PAUSED,
    STATE_ERROR: STATE_ERROR,
    STATE_STOPPED: STATE_STOPPED,
    STATE_BUFFERING: STATE_BUFFERING,
    RATING_HEART: RATING_HEART,
    RATING_THUMBS_UP_DOWN: RATING_THUMBS_UP_DOWN,
    RATING_3_STARS: RATING_3_STARS,
    RATING_4_STARS: RATING_4_STARS,
    RATING_5_STARS: RATING_5_STARS,
    RATING_PERCENTAGE: RATING_PERCENTAGE
};
