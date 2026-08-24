package com.mediapicker.gallery.domain.repositories

import com.mediapicker.gallery.domain.entity.PhotoAlbum
import com.mediapicker.gallery.domain.entity.PhotoFile

interface GalleryRepository {

    suspend fun getAlbums(): HashSet<PhotoAlbum>

    /**
     * Photos of a single bucket, queried on demand.
     *
     * Album entries are deliberately not carried across screens in a [android.os.Bundle] —
     * a folder holding a few thousand photos parcels to well over the 1MB binder limit and
     * kills the process with TransactionTooLargeException when the state is saved.
     */
    suspend fun getAlbumPhotos(albumId: String): List<PhotoFile>
}
