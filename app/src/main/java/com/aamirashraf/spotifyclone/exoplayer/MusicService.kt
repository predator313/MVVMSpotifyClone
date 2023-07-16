package com.aamirashraf.spotifyclone.exoplayer


import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.aamirashraf.spotifyclone.exoplayer.callbacks.MusicPlaybackPreparer
import com.aamirashraf.spotifyclone.exoplayer.callbacks.MusicPlayerEventListener
import com.aamirashraf.spotifyclone.exoplayer.callbacks.MusicPlayerNotificationListener
import com.aamirashraf.spotifyclone.other.Constants.MEDIA_ROOT_ID
import com.aamirashraf.spotifyclone.other.Constants.MUSIC_SERVICE_TAG
import com.aamirashraf.spotifyclone.other.Constants.NETWORK_ERROR
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

class MusicService :MediaBrowserServiceCompat() {
    //media browser service compact is special kind of service that deals with the media related stuff
    @Inject
    lateinit var dataSourceFactory:DefaultDataSourceFactory
    @Inject
    lateinit var exoPlayer:SimpleExoPlayer
    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource
    private lateinit var musicNotificationManager: MusicNotificationManager
    private val serviceJob=Job()
    private val serviceScoped = CoroutineScope(Dispatchers.Main+serviceJob) //custom scoped
    private lateinit var mediaSession:MediaSessionCompat
    var isForegroundService=false
    private var curPlayingSong : MediaMetadataCompat?=null
    private var isPlayerInitialized=false
    private lateinit var musicPlayerEventListener: MusicPlayerEventListener
    companion object{
        var curSongDuration=0L
            private set
        //private set means we can read write from MusicService class only
        //from other class we can only read not write
    }
    override fun onCreate() {
        super.onCreate()
        serviceScoped.launch {
            firebaseMusicSource.fetchMediaData()
        }
        val activityIntent=packageManager?.getLaunchIntentForPackage(packageName)?.let {
            //here we make activity intent as the pending intent
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        mediaSession= MediaSessionCompat(this,MUSIC_SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive=true
        }
        sessionToken=mediaSession.sessionToken
        musicNotificationManager= MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this),

        ){
            //lambda fun
            //if last parameter is lambda fun we can also use it in this way
            curSongDuration=exoPlayer.duration
        }
        val musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource){
            curPlayingSong=it
            preparePlayer(firebaseMusicSource.songs,
            it,
            true)
        }

      val  mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer)
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoPlayer)
        musicPlayerEventListener= MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)
    }
    private inner class MusicQueueNavigator:TimelineQueueNavigator(mediaSession){
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {

            return firebaseMusicSource.songs[windowIndex].description
        }

    }
    private fun preparePlayer(
        songs:List<MediaMetadataCompat>,
        itemToPlay:MediaMetadataCompat?,
        playNow:Boolean
    ){
        val currSongIndex= if(curPlayingSong==null)0 else songs.indexOf(itemToPlay)
        exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoPlayer.seekTo(currSongIndex,0L)
        exoPlayer.playWhenReady = playNow
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScoped.cancel()
        exoPlayer.removeListener(musicPlayerEventListener)
        exoPlayer.release()
    }
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(MEDIA_ROOT_ID,null)
        //if we want client verification we may use some logic here also
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        //deals with the particular playlist
        when(parentId){
            MEDIA_ROOT_ID->{
                val resultsSent=firebaseMusicSource.onReady {isInitialized->
                    if(isInitialized){
                        result.sendResult(firebaseMusicSource.asMediaItems())
                        if(!isPlayerInitialized && firebaseMusicSource.songs.isNotEmpty()){
                            preparePlayer(firebaseMusicSource.songs,firebaseMusicSource.songs[0],false)
                            isPlayerInitialized=true
                        }
                    }else{
                        mediaSession.sendSessionEvent(NETWORK_ERROR,null)
                        result.sendResult(null)
                    }

                }
                if (!resultsSent){
                    result.detach()
                }
            }
        }
    }
}