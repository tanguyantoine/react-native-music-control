/**
 * @providesModule MusicControl
 */
import { Command } from './types';
declare const MusicControl: {
    STATE_PLAYING: any;
    STATE_PAUSED: any;
    STATE_ERROR: any;
    STATE_STOPPED: any;
    STATE_BUFFERING: any;
    RATING_HEART: any;
    RATING_THUMBS_UP_DOWN: any;
    RATING_3_STARS: any;
    RATING_4_STARS: any;
    RATING_5_STARS: any;
    RATING_PERCENTAGE: any;
    enableBackgroundMode: (enable: boolean) => void;
    setNowPlaying: (info: any) => void;
    setPlayback: (info: any) => void;
    updatePlayback: (info: any) => void;
    resetNowPlaying: () => void;
    enableControl: (controlName: string, enable: boolean, options?: {}) => void;
    handleCommand: (commandName: Command, value: any) => void;
    on: (actionName: Command, cb: (value: any) => void) => void;
    off: (actionName: Command) => void;
    stopControl: () => void;
    handleAudioInterruptions: (enable: boolean) => void;
};
export default MusicControl;
