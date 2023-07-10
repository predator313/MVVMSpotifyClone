package com.aamirashraf.spotifyclone.data.remote

import com.aamirashraf.spotifyclone.data.entities.Song
import com.aamirashraf.spotifyclone.other.Constants.SONG_COLLECTION
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MusicDatabase {
    private val firestore = FirebaseFirestore.getInstance()
    private var songCollection = firestore.collection(SONG_COLLECTION)
    suspend fun getAllSongs() :List<Song>{
       return try {
            songCollection.get().await().toObjects(Song::class.java)
        }catch (e:Exception){
            emptyList()
        }
    }
}