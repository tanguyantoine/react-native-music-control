# react-native-music-control

React Native module to control remote controls on lockscreen + display Now playing Info on lockscreen (MPNowPlayingInfoCenter)

Play well with React [Native Sound](https://github.com/zmxv/react-native-sound)

Mix between :

* https://github.com/Muntligt/cordova-plugin-remotecommand (iOS)
* https://github.com/Muntligt/cordova-plugin-nowplaying (iOS)
* https://github.com/homerours/cordova-music-controls-plugin (Android)


# Install

**Add it to your project**

```
npm install react-native-music-control --save
```

## iOS

In XCode, right click Libraries. Click Add Files to "[Your project]". Navigate to node_modules/react-native-music-control. Add the file MusicControl.xcodeproj.

In the Project Navigator, select your project. Click the build target. Click Build Phases. Expand Link Binary With Libraries. Click the plus button and add libMusicControl.a under Workspace.


## Android

**app/build.gradle**

```
 compile project(':react-native-music-control')
```

**MainActivity.java**

```
import com.tanguyantoine.react.MusicControlPackage;
```

**settings.gradle**

```
include ':react-native-music-control'

project(':react-native-music-control').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-music-control/android')
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
  artwork: 'http://lorempixel.com/400/400',
  ...
})
```

**Reset now playing**

```javascript
MusicControl.resetNowPlaying()
```

**Enable/disable controls on lockscreen**

```javascript
MusicControl.enableContol('nextTrack', true)
MusicControl.enableContol('previousTrack', false)
```

**Register to events**

```javascript
MusicControl.on('play', ()=> {
  console.log("Play");
})

MusicControl.on('nextTrack', ()=> {
  console.log("nextTrack");
})

MusicControl.on('previousTrack', ()=> {
  console.log("previousTrack");
})

MusicControl.on('pause', ()=> {
  console.log("pause");
})
```


# TODOS

- [ ] Android support
- [ ] Test
- [x] Publish package
- [ ] rnpm configuration
- [ ] Android : Handle remote events
- [ ] Android : Display cover artwork
