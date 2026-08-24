package com.mediapicker.gallery.domain.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaGalleryEntity(
    val fileName: String?,
    val mediaId: Long?,
    val path: String?,
    val isLocalImage: Boolean = false,
    val mediaType: GalleryViewMediaType
) : Parcelable
