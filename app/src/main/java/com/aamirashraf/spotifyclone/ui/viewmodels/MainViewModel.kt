package com.aamirashraf.spotifyclone.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aamirashraf.spotifyclone.data.entities.Song
import com.aamirashraf.spotifyclone.exoplayer.MusicServiceConnection
import com.aamirashraf.spotifyclone.exoplayer.isPlayEnabled
import com.aamirashraf.spotifyclone.exoplayer.isPlaying
import com.aamirashraf.spotifyclone.exoplayer.isPrepared
import com.aamirashraf.spotifyclone.other.Constants.MEDIA_ROOT_ID
import com.aamirashraf.spotifyclone.other.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection
):ViewModel(){
    private val _mediaItems = MutableLiveData<Resource<List<Song>>>()
    val mediaItems:MutableLiveData<Resource<List<Song>>> = _mediaItems

    val isConnected=musicServiceConnection.isConnected
    val networkerror=musicServiceConnection.isNetworkError
    val currPlayingSong=musicServiceConnection.curPlayingSong
    val playbackState=musicServiceConnection.playbackState
    init {
        _mediaItems.postValue(Resource.Loading())
        musicServiceConnection.subscribe(MEDIA_ROOT_ID,object :MediaBrowserCompat.SubscriptionCallback(){
            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                super.onChildrenLoaded(parentId, children)
                val items = children.map {
                    Song(
                        it.mediaId!!,
                        it.description.title.toString(),
                        it.description.subtitle.toString(),
                        it.description.mediaUri.toString(),
                        it.description.iconUri.toString()
                    )
                }
                _mediaItems.postValue(Resource.Success(items))
            }
        })
    }
    fun skipToNextSong(){
        musicServiceConnection.transportControls.skipToNext()
    }
    fun skipToPrevious(){
        musicServiceConnection.transportControls.skipToPrevious()
    }
    fun seekTo(pos:Long){
        musicServiceConnection.transportControls.seekTo(pos)
    }
    fun playingOrToggleSong(mediaItem:Song,toggle:Boolean=false){
        val isPrepared = playbackState.value?.isPrepared?:false
        if(isPrepared && mediaItem.mediaId== currPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID)){
            playbackState.value?.let {playbackState->
                when{
                    playbackState.isPlaying-> if (toggle)musicServiceConnection.transportControls.pause()
                    playbackState.isPlayEnabled ->musicServiceConnection.transportControls.play()
                    else -> Unit
                }

            }
        }else{
            musicServiceConnection.transportControls.playFromMediaId(mediaItem.mediaId,null)
        }
    }


    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unSubscrible(MEDIA_ROOT_ID,object :SubscriptionCallback(){})
    }
}