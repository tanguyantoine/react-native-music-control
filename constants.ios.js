import { NativeModules } from 'react-native';
const NativeMusicControl = NativeModules.MusicControlManager;

const STATE_PLAYING = NativeMusicControl.STATE_PLAYING;
const STATE_PAUSED = NativeMusicControl.STATE_PAUSED;
const STATE_ERROR = NativeMusicControl.STATE_ERROR;
const STATE_STOPPED = NativeMusicControl.STATE_STOPPED;
const STATE_BUFFERING = NativeMusicControl.STATE_BUFFERING;
const RATING_HEART = NativeMusicControl.RATING_HEART;
const RATING_THUMBS_UP_DOWN = NativeMusicControl.RATING_THUMBS_UP_DOWN;
const RATING_3_STARS = NativeMusicControl.RATING_3_STARS;
const RATING_4_STARS = NativeMusicControl.RATING_4_STARS;
const RATING_5_STARS = NativeMusicControl.RATING_5_STARS;
const RATING_PERCENTAGE = NativeMusicControl.RATING_PERCENTAGE;

export default {
  STATE_PLAYING,
  STATE_PAUSED,
  STATE_ERROR,
  STATE_STOPPED,
  STATE_BUFFERING,
  RATING_HEART,
  RATING_THUMBS_UP_DOWN,
  RATING_3_STARS,
  RATING_4_STARS,
  RATING_5_STARS,
  RATING_PERCENTAGE,
}