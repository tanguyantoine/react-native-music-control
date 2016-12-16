# react-native-music-control

React Native module to control remote controls on lockscreen + display Now playing Info on lockscreen (MPNowPlayingInfoCenter)

Play well with React [Native Sound](https://github.com/zmxv/react-native-sound)

Mix between :

* https://github.com/Muntligt/cordova-plugin-remotecommand (iOS)
* https://github.com/Muntligt/cordova-plugin-nowplaying (iOS)
* https://github.com/homerours/cordova-music-controls-plugin (Android)
* https://github.com/shi11/RemoteControls/pull/32 (Android)


# Install

**Add it to your project**

```
npm install react-native-music-control --save
```

## iOS

### Manual

In XCode, right click Libraries. Click Add Files to "[Your project]". Navigate to node_modules/react-native-music-control. Add the file MusicControl.xcodeproj.

In the Project Navigator, select your project. Click the build target. Click Build Phases. Expand Link Binary With Libraries. Click the plus button and add libMusicControl.a under Workspace.


## Android

### Automatic

`react-native link react-native-music-control`

### Manual

**android/app/build.gradle**

```diff
dependencies {
    ...
    compile "com.facebook.react:react-native:+"  // From node_modules
+   compile project(':react-native-music-control')
}
```

**android/settings.gradle**
```diff
...
include ':app'
+include ':react-native-music-control'
+project(':react-native-music-control').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-music-control/android')
```

**MainActivity.java**

```diff
+import com.tanguyantoine.react.MusicControl;

public class MainApplication extends Application implements ReactApplication {
    //......

    @Override
    protected List<ReactPackage> getPackages() {
        return Arrays.<ReactPackage>asList(
+           new MusicControl(),
            new MainReactPackage()
        );
    }

    //......
  }
```

# Use

```javascript
import MusicControl from 'react-native-music-control';
```

**Now Playing**

NB: You should call this method after a sound is playing

```javascript
MusicControl.setNowPlaying({
  title: 'Billie Jean',
  artwork: 'http://lorempixel.com/400/400', // URL or File path
  artist: 'Michael Jackson',
  album: 'Thriller',
  genre: 'Post-disco, Rhythm and Blues, Funk, Dance-pop',
  duration: 294, // (Seconds)
  description: '', // Android Only
  color: 0xFFFFFF, // Notification Color - Android Only
  date: '1983-01-02T00:00:00Z', // Release Date (RFC 3339) - Android Only
  rating: 84 // Android Only (Percentage)
})
```

**Playback**

Currently, Android only

```javascript
MusicControl.setPlayback({
  state: MusicControl.STATE_PLAYING, // (STATE_ERROR, STATE_STOPPED, STATE_PLAYING, STATE_PAUSED, STATE_BUFFERING)
  volume: 100, // (Percentage)
  speed: 1, // Playback Rate
  elapsedTime: 103, // (Seconds)
  bufferedTime: 200 // (Seconds)
})
```

**Reset now playing**

```javascript
MusicControl.resetNowPlaying()
```

**Enable/disable controls**

iOS: Lockscreen

Android: Notification and external devices (cars, watches)

```javascript
MusicControl.enableControl('play', true)
MusicControl.enableControl('pause', true)
MusicControl.enableControl('stop', false)
MusicControl.enableControl('nextTrack', true)
MusicControl.enableControl('previousTrack', false)
MusicControl.enableControl('seekForward', false);
MusicControl.enableControl('seekBackward', false);
MusicControl.enableControl('seek', false) // Android only
MusicControl.enableControl('rate', false) // Android only
MusicControl.enableControl('volume', true) // Android only
MusicControl.enableControl('enableLanguageOption', false); // iOS only
MusicControl.enableControl('disableLanguageOption', false); // iOS only
MusicControl.enableControl('skipForward', false); // iOS only
MusicControl.enableControl('skipBackward', false); // iOS only
```

`skipBackward` and `skipForward` controls on iOS accept additional configuration options with `interval` key:

```javascript
MusicControl.enableControl('skipBackward', true, {interval: 15}))
MusicControl.enableControl('skipForward', true, {interval: 30}))
```

**Register to events**

```javascript
componentDidMount() {
    MusicControl.enableBackgroundMode(true);
    
    MusicControl.on('play', ()=> {
      this.props.dispatch(playRemoteControl());
    })
    
    MusicControl.on('pause', ()=> {
      this.props.dispatch(pauseRemoteControl());
    })
    
    MusicControl.on('stop', ()=> {
      this.props.dispatch(stopRemoteControl());
    })
    
    MusicControl.on('nextTrack', ()=> {
      this.props.dispatch(nextRemoteControl());
    })
    
    MusicControl.on('previousTrack', ()=> {
      this.props.dispatch(previousRemoteControl());
    })
    
    MusicControl.on('seekForward', ()=> {});
    MusicControl.on('seekBackward', ()=> {});
    
    MusicControl.on('seek', (pos)=> {}); // Android only (Seconds)
    MusicControl.on('rate', (rating)=> {}); // Android only (Percentage)
    MusicControl.on('volume', (volume)=> {}); // Android only (Percentage)
    
    MusicControl.on('togglePlayPause', ()=> {}); // iOS only
    MusicControl.on('enableLanguageOption', ()=> {}); // iOS only
    MusicControl.on('disableLanguageOption', ()=> {}); // iOS only
    MusicControl.on('skipForward', ()=> {}); // iOS only
    MusicControl.on('skipBackward', ()=> {}); // iOS only
}
```



# TODOS

- [x] Android support
- [ ] Test
- [x] Publish package
- [x] React-Native link configuration for Android
- [ ] React-Native link configuration for iOS
- [x] Android : Handle remote events
- [x] Android : Display cover artwork


# Contributing

### Of coursssssseeeeee. I'm waiting your PR :)
 
