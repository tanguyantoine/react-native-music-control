# react-native-music control
 
Installation For React Native < v0.60

## Linking on iOS

### Automatic

```
react-native link
```

### Manual

In XCode, right click Libraries. Click Add Files to "[Your project]". Navigate to node_modules/react-native-music-control. Add the file MusicControl.xcodeproj.

In the Project Navigator, select your project. Click the build target. Click Build Phases. Expand Link Binary With Libraries. Click the plus button and add libMusicControl.a under Workspace.

### CocoaPods

```
pod 'react-native-music-control', :path => '../node_modules/react-native-music-control'
```

Run `pod install` in /ios folder.

- - - -

## Linking on Android

### Automatic

```
react-native link
```

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

**MainApplication.java**

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

**Add following to your project AndroidManifest.xml**
```
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```
- - - -
