package com.aamirashraf.spotifyclone.exoplayer

import android.support.v4.media.session.PlaybackStateCompat

inline val PlaybackStateCompat.isPrepared
    get()=state==PlaybackStateCompat.STATE_BUFFERING
            ||
            state==PlaybackStateCompat.STATE_PAUSED
            ||
            state==PlaybackStateCompat.STATE_PLAYING
inline val PlaybackStateCompat.isPlaying
    get() = state == PlaybackStateCompat.STATE_PLAYING
            ||
            state==PlaybackStateCompat.STATE_BUFFERING
inline val PlaybackStateCompat.isPlayEnabled
    get() = actions and PlaybackStateCompat.ACTION_PLAY!=0L
            ||
            (actions and PlaybackStateCompat.ACTION_PLAY_PAUSE !=0L &&state==PlaybackStateCompat.STATE_PAUSED)