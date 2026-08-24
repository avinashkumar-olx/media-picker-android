package com.mediapicker.gallery

import android.os.Parcel
import com.mediapicker.gallery.presentation.fragments.GalleryPhotoViewFragment
import com.mediapicker.gallery.presentation.utils.Constants.EXTRA_SELECTED_ALBUM_ID
import com.mediapicker.gallery.presentation.utils.Constants.EXTRA_SELECTED_ALBUM_NAME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Guards the fix for the TransactionTooLargeException that killed the process when a folder with
 * a few thousand photos was opened: the album's entries used to ride along in the fragment
 * arguments, so the saved-state parcel grew with the size of the user's gallery.
 */
@RunWith(RobolectricTestRunner::class)
class GalleryPhotoViewFragmentArgsTest {

    private fun marshalledSize(fragment: GalleryPhotoViewFragment): Int {
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(fragment.arguments)
            return parcel.dataSize()
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun `arguments carry only the album identity`() {
        val args = GalleryPhotoViewFragment.getInstance("42", "Camera").requireArguments()

        assertEquals(setOf(EXTRA_SELECTED_ALBUM_ID, EXTRA_SELECTED_ALBUM_NAME), args.keySet())
        assertEquals("42", args.getString(EXTRA_SELECTED_ALBUM_ID))
        assertEquals("Camera", args.getString(EXTRA_SELECTED_ALBUM_NAME))
    }

    @Test
    fun `arguments stay far below the binder limit and do not scale with the album`() {
        val size = marshalledSize(GalleryPhotoViewFragment.getInstance("42", "Camera"))

        // Two short strings. The crash reports showed 1.2MB here.
        assertTrue("arguments parcel unexpectedly large: $size bytes", size < 1_000)
    }
}
