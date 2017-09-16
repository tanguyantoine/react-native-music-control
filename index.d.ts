// Type definitions for react-native-music-control
// Project: https://github.com/tanguyantoine/react-native-music-control
// Definitions by: Dao Nam Tien <https://github.com/tiendn>
// TypeScript Version: 2.5.2

type PlayingInfo = {
    title: string,
    artwork: string, // url
    artist: string,
    album: string,
    genre: string,
    duration: number,
    description: string,
    date: Date,
    rating: number
}

type PlaybackInfo = {
    state: MusicControl,
    elapsedTime: number
}

type ControlEvent = { 
    // Default false for all.
    play: string,
    pause: string,
    stop: string,
    nextTrack: string,
    previousTrack: string,
    seek: string, 
    skipForward: string, // options: { interval: number}
    skipBackward: string, // options: { interval: number}
    seekForward: string, // iOS only. -- Try Sound.setCurrentTime(time: number) react-native-sound
    seekBackward: string, // iOS only.
    enableLanguageOption: string, // iOS only
    disableLanguageOption: string, // iOS only
    setRating: string, // Android only.
    volume: string, // Android only. Only affected when remoteVolume is enabled
    remoteVolume: string // Android only.
    closeNotification: string // Android only, options: {when: 'always'||'paused'||'never'}
}

export default class MusicControl {

    /**
     * Define state status.
     */
    static STATE_PLAYING: NativeMusicControl //  Playing. Ex: when playing audio again.
    static STATE_PAUSED: NativeMusicControl // Paused
    static STATE_ERROR: NativeMusicControl // Error
    static STATE_STOPPED: NativeMusicControl // Stopped
    static STATE_BUFFERING: NativeMusicControl // Buffering

    // Rating is not supported on iOS. This is kept here for compatibility
    // static RATING_HEART: 0;
    // static RATING_THUMBS_UP_DOWN: 0;
    // static RATING_3_STARS: 0;
    // static RATING_4_STARS: 0;
    // static RATING_5_STARS: 0;
    // static RATING_PERCENTAGE: 0;

    /**
     * Backwards compatibility. Use updatePlayback instead.
     * @param info 
     */
    static setPlayback(info: object): void

    /**
     * Update playback after pause or etc...
     * @param info: Object = { state, elapsedTime }
     */
    static updatePlayback(info: object): void

    /**
     * Set enable background mode.
     * @param enable 
     */
    static enableBackgroundMode(enable: boolean): void

    /**
     * Set now playing
     * @param info 
     */
    static setNowPlaying(info: PlayingInfo): void

    /**
     * Reset current playing.
     */
    static resetNowPlaying(): void

    /**
     * 
     * @param controlName :
     * @param bool 
     * @param options // Depends on what event handled. 
     * Android only supports the intervals 5, 10, & 30, while iOS supports any number
     * The interval value only changes what number displays in the UI, 
     * the actual logic to skip forward or backward by a given amount must be implemented in the appropriate callbacks
     */
    static enableControl(eventName: ControlEvent, bool: boolean, options: object ): void

    static handleCommand(commandName): void

    /**
     * Set enable event audio control.
     * @param eventName 
     * @param callback 
     */
    static on(eventName: ControlEvent, callback: function): void
    
    /**
     * Set disable event audio control.
     * @param eventName 
     * @param callback 
     */
    static off(eventName: ControlEvent, callback: function): void

    /**
     * It is possible to customize the icon used in the notification on Android. 
     * By default you can add a drawable resource to your package with the file name music_control_icon 
     * And the notification will use your custom icon. 
     * If you need to specify a custom icon name, or change your notification icon during runtime, 
     * The setNowPlaying function accepts a string for an Android drawable resource name in the notificationIcon prop. 
     * Keep in mind that just like with music_control_icon the resource specified has to be in the drawable package of your Android app.
     */
    // static setCustomNotificationIcon(path: string): void
}
