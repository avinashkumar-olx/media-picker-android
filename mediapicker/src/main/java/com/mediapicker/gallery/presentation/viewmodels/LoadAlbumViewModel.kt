package com.mediapicker.gallery.presentation.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediapicker.gallery.domain.entity.PhotoAlbum
import com.mediapicker.gallery.domain.entity.PhotoFile
import com.mediapicker.gallery.domain.repositories.GalleryRepository
import kotlinx.coroutines.launch

class LoadAlbumViewModel(private val galleryRepository: GalleryRepository) : ViewModel() {

    private val albumLiveData = MutableLiveData<HashSet<PhotoAlbum>>()

    private val albumPhotosLiveData = MutableLiveData<List<PhotoFile>>()

    fun getAlbums() = albumLiveData

    fun getAlbumPhotos() = albumPhotosLiveData

    fun loadAlbums() {
        viewModelScope.launch {
            albumLiveData.postValue(galleryRepository.getAlbums())
        }
    }

    fun loadAlbumPhotos(albumId: String) {
        viewModelScope.launch {
            albumPhotosLiveData.postValue(galleryRepository.getAlbumPhotos(albumId))
        }
    }

}
