package com.mediapicker.gallery.domain.entity

import android.os.Parcelable
import android.text.TextUtils
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
class PhotoFile private constructor(
    var imageId: Long = 0L,
    var path: String? = null,
    var smallPhotoUrl: String? = null,
    var fullPhotoUrl: String? = null,
    var photoBackendId: Long? = null,
    var action: Action? = null,
    var apolloKey: String? = null,
    var status: Status? = null,
    var error: String? = null,
    var adId: String? = null,
    var mimeType: String? = null
) : IGalleryItem, Parcelable {

    val isAlreadyUploaded: Boolean
        get() = !TextUtils.isEmpty(fullPhotoUrl)

    private constructor(builder: Builder) : this(
        imageId = builder.imageId,
        path = builder.path,
        smallPhotoUrl = builder.smallPhotoUrl,
        fullPhotoUrl = builder.fullPhotoUrl,
        photoBackendId = builder.photoBackendId,
        action = builder.action,
        apolloKey = builder.apolloKey,
        status = builder.status,
        error = builder.error,
        adId = builder.adId,
        mimeType = builder.mimeType
    )

    constructor(photoSet: PhotoSet) : this(
        imageId = photoSet.id?.let { java.lang.Long.parseLong(it) } ?: 0L,
        photoBackendId = photoSet.id?.let { java.lang.Long.parseLong(it) },
        smallPhotoUrl = photoSet.getImageURL(PhotoSize.SMALL),
        fullPhotoUrl = photoSet.getImageURL(PhotoSize.FULL),
        apolloKey = photoSet.externalId,
        status = Status.OK,
        action = Action.NONE
    )

    private fun existsFile(path: String): Boolean {
        return File(path).exists()
    }

    fun existsPhoto(): Boolean {
        var existPhoto = true
        path?.isEmpty()?.let { existPhoto = existsFile(path!!) }
        return existPhoto
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as PhotoFile?
        if (this.imageId == 0L || that!!.imageId == 0L) {
            return this.path == that!!.path
        }
        return this.imageId == that.imageId
    }

    override fun hashCode(): Int {
        return imageId.hashCode()
    }

    class Builder {
        var imageId: Long = 0L
        var path: String = ""
        var smallPhotoUrl: String = ""
        internal var fullPhotoUrl = ""
        var photoBackendId: Long? = null
        var action: Action? = null
        var apolloKey: String? = null
        var status: Status? = null
        var error: String? = null
        var adId: String? = null
        var mimeType: String? = null

        fun imageId(imageId: Long): Builder {
            this.imageId = imageId
            return this
        }

        fun path(path: String): Builder {
            this.path = path
            return this
        }

        fun smallPhotoUrl(smallPhotoUrl: String): Builder {
            this.smallPhotoUrl = smallPhotoUrl
            return this
        }

        fun fullPhotoUrl(fullPhotoUrl: String): Builder {
            this.fullPhotoUrl = fullPhotoUrl
            return this
        }

        fun photoBackendId(photoBackendId: Long?): Builder {
            this.photoBackendId = photoBackendId
            return this
        }

        fun action(action: Action): Builder {
            this.action = action
            return this
        }

        fun apolloKey(apolloKey: String): Builder {
            this.apolloKey = apolloKey
            return this
        }

        fun status(status: Status): Builder {
            this.status = status
            return this
        }

        fun error(error: String): Builder {
            this.error = error
            return this
        }

        fun adId(adId: String): Builder {
            this.adId = adId
            return this
        }

        fun mimeType(): Builder {
            return this
        }

        fun build(): PhotoFile {
            return PhotoFile(this)
        }
    }
}
