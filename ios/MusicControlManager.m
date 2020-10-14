#import "MusicControlManager.h"
#import <React/RCTConvert.h>
#import <React/RCTBridge.h>
#import <React/RCTEventDispatcher.h>
#import <AVFoundation/AVFoundation.h>

@import MediaPlayer;

@interface MusicControlManager ()

@property (nonatomic, copy) NSString *artworkUrl;
@property (nonatomic, assign) BOOL audioInterruptionsObserved;

@end

#define MEDIA_STATE_PLAYING @"STATE_PLAYING"
#define MEDIA_STATE_PAUSED @"STATE_PAUSED"
#define MEDIA_STATE_STOPPED @"STATE_STOPPED"
#define MEDIA_STATE_ERROR @"STATE_ERROR"
#define MEDIA_STATE_BUFFERING @"STATE_BUFFERING"
#define MEDIA_STATE_RATING_PERCENTAGE @"STATE_RATING_PERCENTAGE"
#define MEDIA_SPEED @"speed"
#define MEDIA_STATE @"state"
#define MEDIA_DICT @{@"album": MPMediaItemPropertyAlbumTitle, \
    @"trackCount": MPMediaItemPropertyAlbumTrackCount, \
    @"trackNumber": MPMediaItemPropertyAlbumTrackNumber, \
    @"artist": MPMediaItemPropertyArtist, \
    @"composer": MPMediaItemPropertyComposer, \
    @"discCount": MPMediaItemPropertyDiscCount, \
    @"discNumber": MPMediaItemPropertyDiscNumber, \
    @"genre": MPMediaItemPropertyGenre, \
    @"persistentID": MPMediaItemPropertyPersistentID, \
    @"duration": MPMediaItemPropertyPlaybackDuration, \
    @"title": MPMediaItemPropertyTitle, \
    @"elapsedTime": MPNowPlayingInfoPropertyElapsedPlaybackTime, \
    MEDIA_SPEED: MPNowPlayingInfoPropertyPlaybackRate, \
    @"playbackQueueIndex": MPNowPlayingInfoPropertyPlaybackQueueIndex, \
    @"playbackQueueCount": MPNowPlayingInfoPropertyPlaybackQueueCount, \
    @"chapterNumber": MPNowPlayingInfoPropertyChapterNumber, \
    @"chapterCount": MPNowPlayingInfoPropertyChapterCount, \
    @"isLiveStream": MPNowPlayingInfoPropertyIsLiveStream \
}

@implementation MusicControlManager

@synthesize bridge = _bridge;

RCT_EXPORT_MODULE()

- (NSDictionary *)constantsToExport
{
    return @{
        @"STATE_PLAYING": MEDIA_STATE_PLAYING,
        @"STATE_PAUSED": MEDIA_STATE_PAUSED,
        @"STATE_STOPPED" : MEDIA_STATE_STOPPED,
        @"STATE_ERROR" :MEDIA_STATE_ERROR,
        @"STATE_BUFFERING":MEDIA_STATE_BUFFERING,
        @"STATE_RATING_PERCENTAGE":MEDIA_STATE_RATING_PERCENTAGE,
    };
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_METHOD(updatePlayback:(NSDictionary *) originalDetails)
{
    MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];

    if (center.nowPlayingInfo == nil) {
        return;
    }

    NSMutableDictionary *details = [originalDetails mutableCopy];
    NSString *state = [details objectForKey:MEDIA_STATE];

    // Set the playback rate from the state if no speed has been defined
    // If they provide the speed, then use it
    if (state != nil && [details objectForKey:MEDIA_SPEED] == nil) {
        NSNumber *speed = [state isEqual:MEDIA_STATE_PAUSED]
        ? [NSNumber numberWithDouble:0]
        : [NSNumber numberWithDouble:1];

        [details setValue:speed forKey:MEDIA_SPEED];
    }
    if ([state isEqual:MEDIA_STATE_STOPPED]) {
        MPRemoteCommandCenter *remoteCenter = [MPRemoteCommandCenter sharedCommandCenter];
        [self toggleHandler:remoteCenter.stopCommand withSelector:@selector(onStop:) enabled:false];
    }

    NSMutableDictionary *mediaDict = [[NSMutableDictionary alloc] initWithDictionary: center.nowPlayingInfo];

    center.nowPlayingInfo = [self update:mediaDict with:details andSetDefaults:false];

    // Playback state is separated in 11+
    if (@available(iOS 11.0, *)) {
        if ([state isEqual:MEDIA_STATE_PLAYING]) {
            center.playbackState = MPNowPlayingPlaybackStatePlaying;
        } else if ([state isEqual:MEDIA_STATE_PAUSED]) {
            center.playbackState = MPNowPlayingPlaybackStatePaused;
        } else if ([state isEqual:MEDIA_STATE_STOPPED]) {
            center.playbackState = MPNowPlayingPlaybackStateStopped;
        }
    }

    NSString *artworkUrl = [self getArtworkUrl:[originalDetails objectForKey:@"artwork"]];
    [self updateArtworkIfNeeded:artworkUrl];
}


RCT_EXPORT_METHOD(setNowPlaying:(NSDictionary *) details)
{
    MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];
    NSMutableDictionary *mediaDict = [NSMutableDictionary dictionary];


    center.nowPlayingInfo = [self update:mediaDict with:details andSetDefaults:true];

    NSString *artworkUrl = [self getArtworkUrl:[details objectForKey:@"artwork"]];
    [self updateArtworkIfNeeded:artworkUrl];
}

RCT_EXPORT_METHOD(resetNowPlaying)
{
    MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];
    center.nowPlayingInfo = nil;
    self.artworkUrl = nil;
}

RCT_EXPORT_METHOD(enableControl:(NSString *) controlName enabled:(BOOL) enabled options:(NSDictionary *)options)
{
    MPRemoteCommandCenter *remoteCenter = [MPRemoteCommandCenter sharedCommandCenter];

    if ([controlName isEqual: @"pause"]) {
        [self toggleHandler:remoteCenter.pauseCommand withSelector:@selector(onPause:) enabled:enabled];
    } else if ([controlName isEqual: @"play"]) {
        [self toggleHandler:remoteCenter.playCommand withSelector:@selector(onPlay:) enabled:enabled];

    } else if ([controlName isEqual: @"changePlaybackPosition"]) {
        [self toggleHandler:remoteCenter.changePlaybackPositionCommand withSelector:@selector(onChangePlaybackPosition:) enabled:enabled];

    } else if ([controlName isEqual: @"stop"]) {
        [self toggleHandler:remoteCenter.stopCommand withSelector:@selector(onStop:) enabled:enabled];

    } else if ([controlName isEqual: @"togglePlayPause"]) {
        [self toggleHandler:remoteCenter.togglePlayPauseCommand withSelector:@selector(onTogglePlayPause:) enabled:enabled];

    } else if ([controlName isEqual: @"enableLanguageOption"]) {
        [self toggleHandler:remoteCenter.enableLanguageOptionCommand withSelector:@selector(onEnableLanguageOption:) enabled:enabled];

    } else if ([controlName isEqual: @"disableLanguageOption"]) {
        [self toggleHandler:remoteCenter.disableLanguageOptionCommand withSelector:@selector(onDisableLanguageOption:) enabled:enabled];

    } else if ([controlName isEqual: @"nextTrack"]) {
        [self toggleHandler:remoteCenter.nextTrackCommand withSelector:@selector(onNextTrack:) enabled:enabled];

    } else if ([controlName isEqual: @"previousTrack"]) {
        [self toggleHandler:remoteCenter.previousTrackCommand withSelector:@selector(onPreviousTrack:) enabled:enabled];

    } else if ([controlName isEqual: @"seekForward"]) {
        [self toggleHandler:remoteCenter.seekForwardCommand withSelector:@selector(onSeekForward:) enabled:enabled];

    } else if ([controlName isEqual: @"seekBackward"]) {
        [self toggleHandler:remoteCenter.seekBackwardCommand withSelector:@selector(onSeekBackward:) enabled:enabled];
    } else if ([controlName isEqual:@"skipBackward"]) {
        if (options[@"interval"]) {
            remoteCenter.skipBackwardCommand.preferredIntervals = @[options[@"interval"]];
        }
        [self toggleHandler:remoteCenter.skipBackwardCommand withSelector:@selector(onSkipBackward:) enabled:enabled];
    } else if ([controlName isEqual:@"skipForward"]) {
        if (options[@"interval"]) {
            remoteCenter.skipForwardCommand.preferredIntervals = @[options[@"interval"]];
        }
        [self toggleHandler:remoteCenter.skipForwardCommand withSelector:@selector(onSkipForward:) enabled:enabled];
    }
}

/* We need to set the category to allow remote control etc... */

RCT_EXPORT_METHOD(enableBackgroundMode:(BOOL) enabled){
    AVAudioSession *session = [AVAudioSession sharedInstance];
    [session setCategory: AVAudioSessionCategoryPlayback error: nil];
    [session setActive: enabled error: nil];
}

RCT_EXPORT_METHOD(stopControl){
    [self stop];
}

RCT_EXPORT_METHOD(observeAudioInterruptions:(BOOL) observe){
    if (self.audioInterruptionsObserved == observe) {
        return;
    }
    if (observe) {
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(audioInterrupted:) name:AVAudioSessionInterruptionNotification object:nil];
    } else {
        [[NSNotificationCenter defaultCenter] removeObserver:self name:AVAudioSessionInterruptionNotification object:nil];
    }
    self.audioInterruptionsObserved = observe;
}

#pragma mark internal

- (NSDictionary *) update:(NSMutableDictionary *) mediaDict with:(NSDictionary *) details andSetDefaults:(BOOL) setDefault {

    for (NSString *key in MEDIA_DICT) {
        if ([details objectForKey:key] != nil) {
            [mediaDict setValue:[details objectForKey:key] forKey:[MEDIA_DICT objectForKey:key]];
        }

        // In iOS Simulator, always include the MPNowPlayingInfoPropertyPlaybackRate key in your nowPlayingInfo dictionary
        // only if we are creating a new dictionary
        if ([key isEqualToString:MEDIA_SPEED] && [details objectForKey:key] == nil && setDefault) {
            [mediaDict setValue:[NSNumber numberWithDouble:1] forKey:[MEDIA_DICT objectForKey:key]];
        }
    }

    return mediaDict;
}

- (void) toggleHandler:(MPRemoteCommand *) command withSelector:(SEL) selector enabled:(BOOL) enabled {
    [command removeTarget:self action:selector];
    if(enabled){
        [command addTarget:self action:selector];
    }
    command.enabled = enabled;
}

- (id)init {
    self = [super init];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(audioHardwareRouteChanged:) name:AVAudioSessionRouteChangeNotification object:nil];
    [[UIApplication sharedApplication] beginReceivingRemoteControlEvents];
    self.audioInterruptionsObserved = false;
    return self;
}

+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

- (void)dealloc {
    [self stop];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:AVAudioSessionRouteChangeNotification object:nil];
}

- (void)stop {
    MPRemoteCommandCenter *remoteCenter = [MPRemoteCommandCenter sharedCommandCenter];
    [self resetNowPlaying];
    [self toggleHandler:remoteCenter.pauseCommand withSelector:@selector(onPause:) enabled:false];
    [self toggleHandler:remoteCenter.playCommand withSelector:@selector(onPlay:) enabled:false];
    [self toggleHandler:remoteCenter.changePlaybackPositionCommand withSelector:@selector(onChangePlaybackPosition:) enabled:false];
    [self toggleHandler:remoteCenter.stopCommand withSelector:@selector(onStop:) enabled:false];
    [self toggleHandler:remoteCenter.togglePlayPauseCommand withSelector:@selector(onTogglePlayPause:) enabled:false];
    [self toggleHandler:remoteCenter.enableLanguageOptionCommand withSelector:@selector(onEnableLanguageOption:) enabled:false];
    [self toggleHandler:remoteCenter.disableLanguageOptionCommand withSelector:@selector(onDisableLanguageOption:) enabled:false];
    [self toggleHandler:remoteCenter.nextTrackCommand withSelector:@selector(onNextTrack:) enabled:false];
    [self toggleHandler:remoteCenter.previousTrackCommand withSelector:@selector(onPreviousTrack:) enabled:false];
    [self toggleHandler:remoteCenter.seekForwardCommand withSelector:@selector(onSeekForward:) enabled:false];
    [self toggleHandler:remoteCenter.seekBackwardCommand withSelector:@selector(onSeekBackward:) enabled:false];
    [self toggleHandler:remoteCenter.skipBackwardCommand withSelector:@selector(onSkipBackward:) enabled:false];
    [self toggleHandler:remoteCenter.skipForwardCommand withSelector:@selector(onSkipForward:) enabled:false];
    [self observeAudioInterruptions:false];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:AVAudioSessionRouteChangeNotification object:nil];
}

- (MPRemoteCommandHandlerStatus)onPause:(MPRemoteCommandEvent*)event {
    [self sendEvent:@"pause"];
    return MPRemoteCommandHandlerStatusSuccess;
}
- (MPRemoteCommandHandlerStatus)onPlay:(MPRemoteCommandEvent*)event {
    [self sendEvent:@"play"];
    return MPRemoteCommandHandlerStatusSuccess;
}
- (MPRemoteCommandHandlerStatus)onChangePlaybackPosition:(MPChangePlaybackPositionCommandEvent*)event { [self sendEventWithValue:@"changePlaybackPosition" withValue:[NSString stringWithFormat:@"%.15f", event.positionTime]];
    return MPRemoteCommandHandlerStatusSuccess;
}
- (MPRemoteCommandHandlerStatus)onStop:(MPRemoteCommandEvent*)event {
    [self sendEvent:@"stop"];
    return MPRemoteCommandHandlerStatusSuccess;
}
- (MPRemoteCommandHandlerStatus)onTogglePlayPause:(MPRemoteCommandEvent*)event {
    [self sendEvent:@"togglePlayPause"];
    return MPRemoteCommandHandlerStatusSuccess;
}
- (MPRemoteCommandHandlerStatus)onEnableLanguageOption:(MPRemoteCommandEvent*)event {
    [self sendEvent:@"enableLanguageOption"];
    return MPRemoteCommandHandlerStatusSuccess;
}
- (MPRemoteCommandHandlerStatus)onDisableLanguageOption:(MPRemoteCommandEvent*)event {
    [self sendEvent:@"disableLanguageOption"];
    return MPRemoteCommandHandlerStatusSuccess;
}
- (MPRemoteCommandHandlerStatus)onNextTrack:(MPRemoteCommandEvent*)event {
    [self sendEvent:@"nextTrack"];
    return MPRemoteCommandHandlerStatusSuccess;
}
- (MPRemoteCommandHandlerStatus)onPreviousTrack:(MPRemoteCommandEvent*)event {
    [self sendEvent:@"previousTrack"];
    return MPRemoteCommandHandlerStatusSuccess;
}
- (MPRemoteCommandHandlerStatus)onSeekForward:(MPRemoteCommandEvent*)event {
    [self sendEvent:@"seekForward"];
    return MPRemoteCommandHandlerStatusSuccess;
}
- (MPRemoteCommandHandlerStatus)onSeekBackward:(MPRemoteCommandEvent*)event {
    [self sendEvent:@"seekBackward"];
    return MPRemoteCommandHandlerStatusSuccess;
}
- (MPRemoteCommandHandlerStatus)onSkipBackward:(MPRemoteCommandEvent*)event {
    [self sendEvent:@"skipBackward"];
    return MPRemoteCommandHandlerStatusSuccess;
}
- (MPRemoteCommandHandlerStatus)onSkipForward:(MPRemoteCommandEvent*)event {
    [self sendEvent:@"skipForward"];
    return MPRemoteCommandHandlerStatusSuccess;
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"RNMusicControlEvent"];
}

- (void)sendEvent:(NSString*)event {
    [self sendEventWithName:@"RNMusicControlEvent"
                       body:@{@"name": event}];
}

- (NSString*)getArtworkUrl:(NSString*)artwork {
  NSString *artworkUrl = nil;

  if (artwork) {
      if ([artwork isKindOfClass:[NSString class]]) {
           artworkUrl = artwork;
      } else if ([[artwork valueForKey: @"uri"] isKindOfClass:[NSString class]]) {
           artworkUrl = [artwork valueForKey: @"uri"];
      }
  }

  return artworkUrl;
}

- (void)sendEventWithValue:(NSString*)event withValue:(NSString*)value{
   [self sendEventWithName:@"RNMusicControlEvent" body:@{@"name": event, @"value":value}];
}

- (void)updateArtworkIfNeeded:(id)artworkUrl
{
    if( artworkUrl == nil ) {
        return;
    }

    MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];
    if ([artworkUrl isEqualToString:self.artworkUrl] && [center.nowPlayingInfo objectForKey:MPMediaItemPropertyArtwork] != nil) {
        return;
    }

    self.artworkUrl = artworkUrl;

    // Custom handling of artwork in another thread, will be loaded async
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), ^{
        UIImage *image = nil;

        // check whether artwork path is present
        if ([artworkUrl isEqual: @""]) {
            return;
        }

        // artwork is url download from the interwebs
        if ([artworkUrl hasPrefix: @"http://"] || [artworkUrl hasPrefix: @"https://"]) {
            NSURL *imageURL = [NSURL URLWithString:artworkUrl];
            NSData *imageData = [NSData dataWithContentsOfURL:imageURL];
            image = [UIImage imageWithData:imageData];
        } else {
            NSString *localArtworkUrl = [artworkUrl stringByReplacingOccurrencesOfString:@"file://" withString:@""];
            BOOL fileExists = [[NSFileManager defaultManager] fileExistsAtPath:localArtworkUrl];
            if (fileExists) {
                image = [UIImage imageNamed:localArtworkUrl];
            }
        }

        // Check if image was available otherwise don't do anything
        if (image == nil) {
            return;
        }

        // check whether image is loaded
        CGImageRef cgref = [image CGImage];
        CIImage *cim = [image CIImage];

        if (cim == nil && cgref == NULL) {
            return;
        }

        dispatch_async(dispatch_get_main_queue(), ^{

            // Check if URL wasn't changed in the meantime
            if (![artworkUrl isEqual:self.artworkUrl]) {
                return;
            }

            MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];
            MPMediaItemArtwork *artwork = [[MPMediaItemArtwork alloc] initWithImage: image];
            NSMutableDictionary *mediaDict = (center.nowPlayingInfo != nil) ? [[NSMutableDictionary alloc] initWithDictionary: center.nowPlayingInfo] : [NSMutableDictionary dictionary];
            [mediaDict setValue:artwork forKey:MPMediaItemPropertyArtwork];
            center.nowPlayingInfo = mediaDict;
        });
    });
}

- (void)audioHardwareRouteChanged:(NSNotification *)notification {
    NSInteger routeChangeReason = [notification.userInfo[AVAudioSessionRouteChangeReasonKey] integerValue];
    if (routeChangeReason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) {
        //headphones unplugged or bluetooth device disconnected, iOS will pause audio
        [self sendEvent:@"pause"];
    }
}

- (void)audioInterrupted:(NSNotification *)notification {
    if (!self.audioInterruptionsObserved) {
        return;
    }
    NSInteger interruptionType = [notification.userInfo[AVAudioSessionInterruptionTypeKey] integerValue];
    NSInteger interruptionOption = [notification.userInfo[AVAudioSessionInterruptionOptionKey] integerValue];
    bool delayedSuspendedNotification = (@available(iOS 10.0, *)) && [notification.userInfo[AVAudioSessionInterruptionWasSuspendedKey] boolValue];

    if (interruptionType == AVAudioSessionInterruptionTypeBegan && !delayedSuspendedNotification) {
        // Playback interrupted by an incoming phone call.
        [self sendEvent:@"pause"];
    }
    if (interruptionType == AVAudioSessionInterruptionTypeEnded &&
           interruptionOption == AVAudioSessionInterruptionOptionShouldResume) {
        [self sendEvent:@"play"];
    }
}

@end
