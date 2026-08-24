package com.mediapicker.gallery.domain.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UploadedPhoto(val id: String, val url: String) : Parcelable
