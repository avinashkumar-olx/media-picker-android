package com.mediapicker.gallery.domain.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class PhotoProfile(
    var smallUrl: String? = null,
    var mediumUrl: String? = null,
    var bigUrl: String? = null,
    var backgroundUrl: String? = null
) : Parcelable {

    constructor(small: String, big: String) : this(smallUrl = small, bigUrl = big)

    constructor(photoset: PhotoSet) : this(
        smallUrl = photoset.smallPhoto?.url,
        mediumUrl = photoset.mediumPhoto?.url,
        bigUrl = photoset.bigPhoto?.url,
        backgroundUrl = photoset.backgroundPhoto?.url
    )
}
