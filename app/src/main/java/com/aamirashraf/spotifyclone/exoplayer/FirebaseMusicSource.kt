package com.aamirashraf.spotifyclone.exoplayer

import android.provider.MediaStore.Audio
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_URI
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE
import androidx.core.net.toUri
import com.aamirashraf.spotifyclone.data.remote.MusicDatabase
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


class FirebaseMusicSource @Inject constructor(
    private val musicDatabase: MusicDatabase
) {
    //to get all the song from the firebase
    var songs= emptyList<MediaMetadataCompat>()
    suspend fun fetchMediaData()= withContext(Dispatchers.IO){
        state=State.STATE_INITIALIZING
        val allSongs = musicDatabase.getAllSongs()
        songs=allSongs.map { song ->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_ARTIST,song.subtitle)
                .putString(METADATA_KEY_MEDIA_ID,song.mediaId)
                .putString(METADATA_KEY_TITLE,song.title)
                .putString(METADATA_KEY_DISPLAY_TITLE,song.title)
                .putString(METADATA_KEY_DISPLAY_ICON_URI,song.imageUrl)
                .putString(METADATA_KEY_MEDIA_URI,song.songUrl)
                .putString(METADATA_KEY_ALBUM_ART_URI,song.imageUrl)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE,song.subtitle)
                .putString(METADATA_KEY_DISPLAY_DESCRIPTION,song.subtitle)
                .build()
        }
        state=State.STATE_INITIALIZED
    }
    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory):ConcatenatingMediaSource{
        //this function used to make playlist of songs
        val concatenatingMediaSource=ConcatenatingMediaSource()
        songs.forEach { song->
            val mediaSource=ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(song.getString(METADATA_KEY_MEDIA_URI).toUri()))
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }
    fun asMediaItems()=songs.map{song->
        val description=MediaDescriptionCompat.Builder()
            .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            .setTitle(song.description.title)
            .setSubtitle(song.description.subtitle)
            .setMediaId(song.description.mediaId)
            .setIconUri(song.description.iconUri)
            .build()
        MediaBrowserCompat.MediaItem(description,FLAG_PLAYABLE)
    }
    private val onReadyListeners = mutableListOf<(Boolean)->Unit >()
    private var state:State=State.STATE_CREATED
        set(value){
            if(value==State.STATE_INITIALIZED || value==State.STATE_ERROR){
                //means it is initialized or error means it is finished
                synchronized(onReadyListeners){
                    //we use synchronized for the thread safe way
                    //what happens inside the block is only be accessed from same thread
                    field=value
                    //field means current value and value means the new value
                    onReadyListeners.forEach { listener->
                        listener(state==State.STATE_INITIALIZED)//TRUE means state is initialized false means error
                    }

                }

            }
            else{
                field=value
            }
        }
    fun onReady(action:(Boolean)->Unit):Boolean{
        return if(state==State.STATE_CREATED || state==State.STATE_INITIALIZING){
            onReadyListeners.add(action)
            false
        } else{
            action(state==State.STATE_INITIALIZED)
            true
        }
    }
}
enum class State{
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}