package com.aamirashraf.spotifyclone.exoplayer


import android.app.PendingIntent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.aamirashraf.spotifyclone.other.Constants.MUSIC_SERVICE_TAG
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject

class MusicService :MediaBrowserServiceCompat() {
    //media browser service compact is special kind of service that deals with the media related stuff
    @Inject
    lateinit var dataSourceFactory:DefaultDataSourceFactory
    @Inject
    lateinit var exoPlayer:SimpleExoPlayer
    private val serviceJob=Job()
    private val serviceScoped = CoroutineScope(Dispatchers.Main+serviceJob) //custom scoped
    private lateinit var mediaSession:MediaSessionCompat
    override fun onCreate() {
        super.onCreate()
        val activityIntent=packageManager?.getLaunchIntentForPackage(packageName)?.let {
            //here we make activity intent as the pending intent
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        mediaSession= MediaSessionCompat(this,MUSIC_SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive=true
        }
        sessionToken=mediaSession.sessionToken
      val  mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(exoPlayer)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScoped.cancel()
    }
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        TODO("Not yet implemented")
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        TODO("Not yet implemented")
    }
}