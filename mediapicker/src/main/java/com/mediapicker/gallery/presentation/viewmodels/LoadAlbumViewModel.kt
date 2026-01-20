package com.mediapicker.gallery.presentation.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediapicker.gallery.domain.entity.PhotoAlbum
import com.mediapicker.gallery.domain.repositories.GalleryRepository
import kotlinx.coroutines.launch

class LoadAlbumViewModel(private val galleryRepository: GalleryRepository) : ViewModel() {

    private val albumLiveData = MutableLiveData<HashSet<PhotoAlbum>>()

    fun getAlbums() = albumLiveData

    fun loadAlbums() {
        viewModelScope.launch {
            albumLiveData.postValue(galleryRepository.getAlbums())
        }
    }

}
