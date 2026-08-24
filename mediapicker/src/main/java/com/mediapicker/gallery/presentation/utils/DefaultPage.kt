package com.mediapicker.gallery.presentation.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class DefaultPage : Parcelable {
    @Parcelize
    data object PhotoPage : DefaultPage()

    @Parcelize
    data object VideoPage : DefaultPage()
}
