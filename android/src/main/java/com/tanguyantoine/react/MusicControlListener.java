package com.tanguyantoine.react;

import android.support.v4.media.RatingCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.MediaSessionCompat;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class MusicControlListener extends MediaSessionCompat.Callback {

    private static void sendEvent(ReactApplicationContext context, String type, Object value) {
        WritableMap data = Arguments.createMap();
        data.putString("name", type);

        if(value == null) {
            // NOOP
        } else if(value instanceof Double || value instanceof Float) {
            data.putDouble("value", (double)value);
        } else if(value instanceof Boolean) {
            data.putBoolean("value", (boolean)value);
        } else if(value instanceof Integer) {
            data.putInt("value", (int)value);
        }

        context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("RNMusicControlEvent", data);
    }

    private final ReactApplicationContext context;

    MusicControlListener(ReactApplicationContext context) {
        this.context = context;
    }

    @Override
    public void onPlay() {
        sendEvent(context, "play", null);
    }

    @Override
    public void onPause() {
        sendEvent(context, "pause", null);
    }

    @Override
    public void onStop() {
        sendEvent(context, "stop", null);
    }

    @Override
    public void onSkipToNext() {
        sendEvent(context, "nextTrack", null);
    }

    @Override
    public void onSkipToPrevious() {
        sendEvent(context, "previousTrack", null);
    }

    @Override
    public void onSeekTo(long pos) {
        sendEvent(context, "seek", pos / 1000D);
    }

    @Override
    public void onFastForward() {
        sendEvent(context, "skipForward", null);
    }

    @Override
    public void onRewind() {
        sendEvent(context, "skipBackward", null);
    }

    @Override
    public void onSetRating(RatingCompat rating) {
        if(MusicControlModule.INSTANCE == null) return;
        int type = MusicControlModule.INSTANCE.ratingType;

        if(type == RatingCompat.RATING_PERCENTAGE) {
            sendEvent(context, "setRating", rating.getPercentRating());
        } else if(type == RatingCompat.RATING_HEART) {
            sendEvent(context, "setRating", rating.hasHeart());
        } else if(type == RatingCompat.RATING_THUMB_UP_DOWN) {
            sendEvent(context, "setRating", rating.isThumbUp());
        } else {
            sendEvent(context, "setRating", rating.getStarRating());
        }
    }

    public static class VolumeListener extends VolumeProviderCompat {

        private final ReactApplicationContext context;
        public VolumeListener(ReactApplicationContext context, boolean changeable, int maxVolume, int currentVolume) {
            super(changeable ? VOLUME_CONTROL_ABSOLUTE : VOLUME_CONTROL_FIXED, maxVolume, currentVolume);
            this.context = context;
        }

        public boolean isChangeable() {
            return getVolumeControl() != VolumeProviderCompat.VOLUME_CONTROL_FIXED;
        }

        @Override
        public void onSetVolumeTo(int volume) {
            setCurrentVolume(volume);
            sendEvent(context, "volume", volume);
        }

        @Override
        public void onAdjustVolume(int direction) {
            int maxVolume = getMaxVolume();
            int tick = direction * (maxVolume / 10);
            int volume = Math.max(Math.min(getCurrentVolume() + tick, maxVolume), 0);

            setCurrentVolume(volume);
            sendEvent(context, "volume", volume);
        }

        public VolumeListener create(Boolean changeable, Integer maxVolume, Integer currentVolume) {
            if(currentVolume == null) {
                currentVolume = getCurrentVolume();
            } else {
                setCurrentVolume(currentVolume);
            }

            if(changeable == null) changeable = isChangeable();
            if(maxVolume == null) maxVolume = getMaxVolume();

            if(changeable == isChangeable() && maxVolume == getMaxVolume()) return this;
            return new VolumeListener(context, changeable, maxVolume, currentVolume);
        }
    }

}
