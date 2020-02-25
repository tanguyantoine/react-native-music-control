"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var react_native_1 = require("react-native");
var NativeMusicControl = react_native_1.NativeModules.MusicControlManager;
var STATE_PLAYING = NativeMusicControl.STATE_PLAYING;
var STATE_PAUSED = NativeMusicControl.STATE_PAUSED;
var STATE_ERROR = NativeMusicControl.STATE_ERROR;
var STATE_STOPPED = NativeMusicControl.STATE_STOPPED;
var STATE_BUFFERING = NativeMusicControl.STATE_BUFFERING;
var RATING_HEART = NativeMusicControl.RATING_HEART;
var RATING_THUMBS_UP_DOWN = NativeMusicControl.RATING_THUMBS_UP_DOWN;
var RATING_3_STARS = NativeMusicControl.RATING_3_STARS;
var RATING_4_STARS = NativeMusicControl.RATING_4_STARS;
var RATING_5_STARS = NativeMusicControl.RATING_5_STARS;
var RATING_PERCENTAGE = NativeMusicControl.RATING_PERCENTAGE;
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
