package com.mediapicker.gallery.domain.repositories

import com.mediapicker.gallery.domain.entity.PhotoAlbum

interface GalleryRepository {

    suspend fun getAlbums(): HashSet<PhotoAlbum>
}