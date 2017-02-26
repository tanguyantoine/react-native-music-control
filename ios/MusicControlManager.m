#import "MusicControlManager.h"
#import <React/RCTConvert.h>
#import <React/RCTBridge.h>
#import <React/RCTEventDispatcher.h>
#import <AVFoundation/AVAudioSession.h>

@import MediaPlayer;

@interface MusicControlManager ()

@property (nonatomic, copy) NSString *artworkUrl;

@end

@implementation MusicControlManager

@synthesize bridge = _bridge;

RCT_EXPORT_MODULE()

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_METHOD(togglePause:(nonnull NSNumber *) elapsed)
{
    MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];
    
    if (center.nowPlayingInfo == nil) {
        return;
    }
    
    NSMutableDictionary *mediaDict = [[NSMutableDictionary alloc] initWithDictionary: center.nowPlayingInfo];

    NSNumber *speed = [[mediaDict objectForKey:MPNowPlayingInfoPropertyPlaybackRate] isEqual:[NSNumber numberWithDouble:0]]
        ? [NSNumber numberWithDouble:1]
        : [NSNumber numberWithDouble:0];

    
    [mediaDict setValue:speed forKey:MPNowPlayingInfoPropertyPlaybackRate];
    [mediaDict setValue:elapsed forKey:MPNowPlayingInfoPropertyElapsedPlaybackTime];
    
    center.nowPlayingInfo = mediaDict;
}

RCT_EXPORT_METHOD(setNowPlaying:(NSDictionary *) details)
{

    MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];

    // Create media dictionary from existing keys or create a new one, this way we can update single attributes if we want to
    NSMutableDictionary *mediaDict = (center.nowPlayingInfo != nil) ? [[NSMutableDictionary alloc] initWithDictionary: center.nowPlayingInfo] : [NSMutableDictionary dictionary];

    if ([details objectForKey: @"album"] != nil) {
        [mediaDict setValue:[details objectForKey: @"album"] forKey:MPMediaItemPropertyAlbumTitle];
    }

    if ([details objectForKey: @"trackCount"] != nil) {
        [mediaDict setValue:[details objectForKey: @"trackCount"] forKey:MPMediaItemPropertyAlbumTrackCount];
    }

    if ([details objectForKey: @"trackNumber"] != nil) {
        [mediaDict setValue:[details objectForKey: @"trackNumber"] forKey:MPMediaItemPropertyAlbumTrackNumber];
    }

    if ([details objectForKey: @"artist"] != nil) {
        [mediaDict setValue:[details objectForKey: @"artist"] forKey:MPMediaItemPropertyArtist];
    }

    if ([details objectForKey: @"composer"] != nil) {
        [mediaDict setValue:[details objectForKey: @"composer"] forKey:MPMediaItemPropertyComposer];
    }

    if ([details objectForKey: @"discCount"] != nil) {
        [mediaDict setValue:[details objectForKey: @"discCount"] forKey:MPMediaItemPropertyDiscCount];
    }

    if ([details objectForKey: @"discNumber"] != nil) {
        [mediaDict setValue:[details objectForKey: @"discNumber"] forKey:MPMediaItemPropertyDiscNumber];
    }

    if ([details objectForKey: @"genre"] != nil) {
        [mediaDict setValue:[details objectForKey: @"genre"] forKey:MPMediaItemPropertyGenre];
    }

    if ([details objectForKey: @"persistentID"] != nil) {
        [mediaDict setValue:[details objectForKey: @"persistentID"] forKey:MPMediaItemPropertyPersistentID];
    }

    if ([details objectForKey: @"duration"] != nil) {
        [mediaDict setValue:[details objectForKey: @"duration"] forKey:MPMediaItemPropertyPlaybackDuration];
    }

    if ([details objectForKey: @"title"] != nil) {
        [mediaDict setValue:[details objectForKey: @"title"] forKey:MPMediaItemPropertyTitle];
    }

    if ([details objectForKey: @"elapsedPlaybackTime"] != nil) {
        [mediaDict setValue:[details objectForKey: @"elapsedPlaybackTime"] forKey:MPNowPlayingInfoPropertyElapsedPlaybackTime];
    }

    if ([details objectForKey: @"playbackRate"] != nil) {
        [mediaDict setValue:[details objectForKey: @"playbackRate"] forKey:MPNowPlayingInfoPropertyPlaybackRate];
    } else {
        // In iOS Simulator, always include the MPNowPlayingInfoPropertyPlaybackRate key in your nowPlayingInfo dictionary
        [mediaDict setValue:[NSNumber numberWithDouble:1] forKey:MPNowPlayingInfoPropertyPlaybackRate];
    }

    if ([details objectForKey: @"playbackQueueIndex"] != nil) {
        [mediaDict setValue:[details objectForKey: @"playbackQueueIndex"] forKey:MPNowPlayingInfoPropertyPlaybackQueueIndex];
    }

    if ([details objectForKey: @"playbackQueueCount"] != nil) {
        [mediaDict setValue:[details objectForKey: @"playbackQueueCount"] forKey:MPNowPlayingInfoPropertyPlaybackQueueCount];
    }

    if ([details objectForKey: @"chapterNumber"] != nil) {
        [mediaDict setValue:[details objectForKey: @"chapterNumber"] forKey:MPNowPlayingInfoPropertyChapterNumber];
    }

    if ([details objectForKey: @"chapterCount"] != nil) {
        [mediaDict setValue:[details objectForKey: @"chapterCount"] forKey:MPNowPlayingInfoPropertyChapterCount];
    }

    center.nowPlayingInfo = mediaDict;

    // Custom handling of artwork in another thread, will be loaded async
    self.artworkUrl = details[@"artwork"];
    [self updateNowPlayingArtwork];
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

#pragma mark internal

- (void) toggleHandler:(MPRemoteCommand *) command withSelector:(SEL) selector enabled:(BOOL) enabled {
    [command removeTarget:self action:selector];
    if(enabled){
        [command addTarget:self action:selector];        
    }
    command.enabled = enabled;
}

- (void)dealloc {
    MPRemoteCommandCenter *remoteCenter = [MPRemoteCommandCenter sharedCommandCenter];
    [self toggleHandler:remoteCenter.pauseCommand withSelector:@selector(onPause:) enabled:false];
    [self toggleHandler:remoteCenter.playCommand withSelector:@selector(onPlay:) enabled:false];
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
}


- (void)onPause:(MPRemoteCommandEvent*)event { [self sendEvent:@"pause"]; }
- (void)onPlay:(MPRemoteCommandEvent*)event { [self sendEvent:@"play"]; }
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

- (void)sendEvent:(NSString*)event {
    [self.bridge.eventDispatcher sendDeviceEventWithName:@"RNMusicControlEvent"
                                                 body:@{@"name": event}];
}

- (void)updateNowPlayingArtwork
{
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), ^{
        NSString *url = self.artworkUrl;
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

@end
