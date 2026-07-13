package com.mediapicker.gallery.presentation.viewmodels.factory

import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.loader.content.CursorLoader

class SafeCursorLoader(
    context: Context,
    uri: Uri,
    projection: Array<String>?,
    selection: String?,
    selectionArgs: Array<String?>,
    sortOrder: String?
) : CursorLoader(context, uri, projection, selection, selectionArgs, sortOrder) {

    override fun loadInBackground(): Cursor? {
        return try {
            super.loadInBackground()
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
