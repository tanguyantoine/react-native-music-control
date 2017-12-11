#import "MusicControlManager.h"
#import <React/RCTConvert.h>
#import <React/RCTBridge.h>
#import <React/RCTEventDispatcher.h>
#import <AVFoundation/AVFoundation.h>

@import MediaPlayer;

@interface MusicControlManager ()

@property (nonatomic, copy) NSString *artworkUrl;

@end

#define MEDIA_STATE_PLAYING @"STATE_PLAYING"
#define MEDIA_STATE_PAUSED @"STATE_PAUSED"
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
    @"chapterCount": MPNowPlayingInfoPropertyChapterCount \
}

@implementation MusicControlManager

@synthesize bridge = _bridge;

RCT_EXPORT_MODULE()

- (NSDictionary *)constantsToExport
{
    return @{
        @"STATE_PLAYING": MEDIA_STATE_PLAYING,
        @"STATE_PAUSED": MEDIA_STATE_PAUSED
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

    // Set the playback rate from the state if no speed has been defined
    // If they provide the speed, then use it
    if ([details objectForKey:MEDIA_STATE] != nil && [details objectForKey:MEDIA_SPEED] == nil) {
        NSNumber *speed = [[details objectForKey:MEDIA_STATE] isEqual:MEDIA_STATE_PAUSED]
        ? [NSNumber numberWithDouble:0]
        : [NSNumber numberWithDouble:1];

        [details setValue:speed forKey:MEDIA_SPEED];
    }

    NSMutableDictionary *mediaDict = [[NSMutableDictionary alloc] initWithDictionary: center.nowPlayingInfo];

    center.nowPlayingInfo = [self update:mediaDict with:details andSetDefaults:false];


    if ([details objectForKey:@"artwork"] != self.artworkUrl) {
        [self updateArtworkIfNeeded:[details objectForKey:@"artwork"]];
    }

}


RCT_EXPORT_METHOD(setNowPlaying:(NSDictionary *) details)
{
    MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];
    NSMutableDictionary *mediaDict = [NSMutableDictionary dictionary];


    center.nowPlayingInfo = [self update:mediaDict with:details andSetDefaults:true];

  [self updateArtworkIfNeeded:[details objectForKey:@"artwork"]];
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
  [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(audioInterrupted:) name:AVAudioSessionInterruptionNotification object:nil];
  return self;
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
    [[NSNotificationCenter defaultCenter] removeObserver:self name:AVAudioSessionRouteChangeNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:AVAudioSessionInterruptionNotification object:nil];
}

- (void)onPause:(MPRemoteCommandEvent*)event { [self sendEvent:@"pause"]; }
- (void)onPlay:(MPRemoteCommandEvent*)event { [self sendEvent:@"play"]; }
- (void)onChangePlaybackPosition:(MPChangePlaybackPositionCommandEvent*)event { [self sendEventWithValue:@"changePlaybackPosition" withValue:[NSString stringWithFormat:@"%.15f", event.positionTime]]; }
- (void)onStop:(MPRemoteCommandEvent*)event { [self sendEvent:@"stop"]; }
- (void)onTogglePlayPause:(MPRemoteCommandEvent*)event { [self sendEvent:@"togglePlayPause"]; }
- (void)onEnableLanguageOption:(MPRemoteCommandEvent*)event { [self sendEvent:@"enableLanguageOption"]; }
- (void)onDisableLanguageOption:(MPRemoteCommandEvent*)event { [self sendEvent:@"disableLanguageOption"]; }
- (void)onNextTrack:(MPRemoteCommandEvent*)event { [self sendEvent:@"nextTrack"]; }
- (void)onPreviousTrack:(MPRemoteCommandEvent*)event { [self sendEvent:@"previousTrack"]; }
- (void)onSeekForward:(MPRemoteCommandEvent*)event { [self sendEvent:@"seekForward"]; }
- (void)onSeekBackward:(MPRemoteCommandEvent*)event { [self sendEvent:@"seekBackward"]; }
- (void)onSkipBackward:(MPRemoteCommandEvent*)event { [self sendEvent:@"skipBackward"]; }
- (void)onSkipForward:(MPRemoteCommandEvent*)event { [self sendEvent:@"skipForward"]; }

- (NSArray<NSString *> *)supportedEvents {
    return @[@"RNMusicControlEvent"];
}

- (void)sendEvent:(NSString*)event {
    [self sendEventWithName:@"RNMusicControlEvent"
                       body:@{@"name": event}];
}

- (void)sendEventWithValue:(NSString*)event withValue:(NSString*)value{
   [self sendEventWithName:@"RNMusicControlEvent" body:@{@"name": event, @"value":value}];
}

- (void)updateArtworkIfNeeded:(id)artwork
{
    NSString *url = nil;
    if (artwork) {
        if ([artwork isKindOfClass:[NSString class]]) {
             url = artwork;
        } else if ([[artwork valueForKey: @"uri"] isKindOfClass:[NSString class]]) {
             url = [artwork valueForKey: @"uri"];
        }
    }

    if (url != nil) {
        self.artworkUrl = url;

        // Custom handling of artwork in another thread, will be loaded async
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), ^{
            UIImage *image = nil;

            // check whether artwork path is present
            if (![url isEqual: @""]) {
                // artwork is url download from the interwebs
                if ([url hasPrefix: @"http://"] || [url hasPrefix: @"https://"]) {
                    NSURL *imageURL = [NSURL URLWithString:url];
                    NSData *imageData = [NSData dataWithContentsOfURL:imageURL];
                    image = [UIImage imageWithData:imageData];
                } else {
                    // artwork is local. so create it from a UIImage
                    BOOL fileExists = [[NSFileManager defaultManager] fileExistsAtPath:url];
                    if (fileExists) {
                        image = [UIImage imageNamed:url];
                    }
                }
            }

            // Check if image was available otherwise don't do anything
            if (image == nil) {
                return;
            }

            // check whether image is loaded
            CGImageRef cgref = [image CGImage];
            CIImage *cim = [image CIImage];

            if (cim != nil || cgref != NULL) {
                dispatch_async(dispatch_get_main_queue(), ^{

                    // Check if URL wasn't changed in the meantime
                    if ([url isEqual:self.artworkUrl]) {
                        MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];
                        MPMediaItemArtwork *artwork = [[MPMediaItemArtwork alloc] initWithImage: image];
                        NSMutableDictionary *mediaDict = (center.nowPlayingInfo != nil) ? [[NSMutableDictionary alloc] initWithDictionary: center.nowPlayingInfo] : [NSMutableDictionary dictionary];
                        [mediaDict setValue:artwork forKey:MPMediaItemPropertyArtwork];
                        center.nowPlayingInfo = mediaDict;
                    }
                });
            }
        });
    }
}

- (void)audioHardwareRouteChanged:(NSNotification *)notification {
    NSInteger routeChangeReason = [notification.userInfo[AVAudioSessionRouteChangeReasonKey] integerValue];
    if (routeChangeReason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) {
        //headphones unplugged or bluetooth device disconnected, iOS will pause audio
        [self sendEvent:@"pause"];
    }
}

- (void)audioInterrupted:(NSNotification *)notification {
    NSInteger interuptionType = [notification.userInfo[AVAudioSessionInterruptionTypeKey] integerValue];
    if (interuptionType == AVAudioSessionInterruptionTypeBegan) {
        // Playback interrupted by an incoming phone call.
        [self sendEvent:@"pause"];
    }
}

@end
