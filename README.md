# react-native-music-control

Mix between : https://github.com/Muntligt/cordova-plugin-remotecommand and https://github.com/Muntligt/cordova-plugin-nowplaying but for React Native


# Install

TODO

# Use

```javascript
import MusicControl from 'react-native-music-control';
```

*Now Playing*

NB: You should call this method after a sound is playing

```javascript
MusicControl.setNowPlaying({
  title: 'Billie Jean',
  artwork: 'http://lorempixel.com/400/400',
  ...
})
```



*Reset now playing*

```javascript
MusicControl.resetNowPlaying()
```

*Enable/disable controls on lockscreen*

```javascript
MusicControl.enableContol('nextTrack', true)
MusicControl.enableContol('previousTrack', false)
```

*Register to events*

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
