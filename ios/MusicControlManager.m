#import "MusicControlManager.h"
#import "RCTConvert.h"
#import "RCTEventDispatcher.h"

@import MediaPlayer;

@implementation MusicControlManager

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(setNowPlaying:(NSDictionary *) details)
{
    NSLog([details description]);
    MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];
    
    // Create media dictionary from existing keys or create a new one, this way we can update single attributes if we want to
    NSMutableDictionary *mediaDict = (center.nowPlayingInfo != nil) ? [[NSMutableDictionary alloc] initWithDictionary: center.nowPlayingInfo] : [NSMutableDictionary dictionary];
    
    if ([details objectForKey: @"albumTitle"] != nil) {
        [mediaDict setValue:[details objectForKey: @"albumTitle"] forKey:MPMediaItemPropertyAlbumTitle];
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
    
    if ([details objectForKey: @"playbackDuration"] != nil) {
        [mediaDict setValue:[details objectForKey: @"playbackDuration"] forKey:MPMediaItemPropertyPlaybackDuration];
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
    if ([details objectForKey: @"artwork"] != nil) {
        [self setNowPlayingArtwork: [details objectForKey: @"artwork"]];
    }
}

RCT_EXPORT_METHOD(resetNowPlaying)
{
    MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];
    center.nowPlayingInfo = nil;
}


RCT_EXPORT_METHOD(enableContol:(NSString *) controlName enabled:(BOOL) enabled)
{
    MPRemoteCommandCenter *remoteCenter = [MPRemoteCommandCenter sharedCommandCenter];
    if ([controlName isEqual: @"@pause"]) {
        remoteCenter.pauseCommand.enabled = enabled;
    } else if ([controlName isEqual: @"play"]) {
        remoteCenter.playCommand.enabled = enabled;
    } else if ([controlName isEqual: @"stop"]) {
        remoteCenter.stopCommand.enabled = enabled;
    } else if ([controlName isEqual: @"togglePlayPause"]) {
        remoteCenter.togglePlayPauseCommand.enabled = enabled;
    } else if ([controlName isEqual: @"enableLanguageOption"]) {
        remoteCenter.enableLanguageOptionCommand.enabled = enabled;
    } else if ([controlName isEqual: @"disableLanguageOption"]) {
        remoteCenter.disableLanguageOptionCommand.enabled = enabled;
    } else if ([controlName isEqual: @"nextTrack"]) {
        remoteCenter.nextTrackCommand.enabled = enabled;
    } else if ([controlName isEqual: @"previousTrack"]) {
        remoteCenter.previousTrackCommand.enabled = enabled;
    } else if ([controlName isEqual: @"seekForward"]) {
        remoteCenter.seekForwardCommand.enabled = enabled;
    } else if ([controlName isEqual: @"seekBackward"]) {
        remoteCenter.seekBackwardCommand.enabled = enabled;
    }
}

#pragma mark internal

- (void)setNowPlayingArtwork:(NSString*)url
{
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
                NSString *basePath = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
                NSString *fullPath = [NSString stringWithFormat:@"%@%@", basePath, url];
                BOOL fileExists = [[NSFileManager defaultManager] fileExistsAtPath:fullPath];
                if (fileExists) {
                    image = [UIImage imageNamed:fullPath];
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
            // Callback to main queue to set nowPlayingInfo
            dispatch_async(dispatch_get_main_queue(), ^{
                MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];
                MPMediaItemArtwork *artwork = [[MPMediaItemArtwork alloc] initWithImage: image];
                NSMutableDictionary *mediaDict = (center.nowPlayingInfo != nil) ? [[NSMutableDictionary alloc] initWithDictionary: center.nowPlayingInfo] : [NSMutableDictionary dictionary];
                [mediaDict setValue:artwork forKey:MPMediaItemPropertyArtwork];
                center.nowPlayingInfo = mediaDict;
            });
        }
    });
}

@end
