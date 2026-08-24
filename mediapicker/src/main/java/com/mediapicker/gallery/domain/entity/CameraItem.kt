package com.mediapicker.gallery.domain.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class CameraItem(
    var name: String = "Camera",
    var albumId: String? = "-1",
    var albumEntries: List<PhotoFile> = ArrayList()
) : IGalleryItem, Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as CameraItem?
        return if (this.albumId == null) {
            that!!.albumId == null
        } else this.albumId == that!!.albumId
    }

    override fun hashCode(): Int {
        return if (this.albumId == null) {
            0
        } else albumId!!.hashCode()
    }
}
