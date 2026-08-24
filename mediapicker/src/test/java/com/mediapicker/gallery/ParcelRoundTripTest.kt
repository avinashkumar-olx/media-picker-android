package com.mediapicker.gallery

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.BundleCompat
import androidx.core.os.ParcelCompat
import android.os.Bundle
import com.mediapicker.gallery.domain.entity.Action
import com.mediapicker.gallery.domain.entity.PhotoAlbum
import com.mediapicker.gallery.domain.entity.PhotoFile
import com.mediapicker.gallery.domain.entity.PhotoSet
import com.mediapicker.gallery.domain.entity.Status
import com.mediapicker.gallery.presentation.fragments.BaseFragment
import com.mediapicker.gallery.presentation.utils.DefaultPage
import com.mediapicker.gallery.presentation.viewmodels.VideoFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ParcelRoundTripTest {

    /**
     * Marshals through a real Parcel. @Parcelize's CREATOR is synthetic and not visible to the
     * Kotlin frontend within this module, so go via writeParcelable/readParcelable.
     */
    private inline fun <reified T : Parcelable> roundTrip(value: T): T {
        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(value, 0)
            parcel.setDataPosition(0)
            return ParcelCompat.readParcelable(parcel, T::class.java.classLoader, T::class.java)!!
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun `PhotoFile survives a parcel round trip with all fields intact`() {
        val original = PhotoFile.Builder()
            .imageId(42L)
            .path("/sdcard/a.jpg")
            .smallPhotoUrl("http://small")
            .fullPhotoUrl("http://full")
            .photoBackendId(99L)
            .action(Action.NONE)
            .status(Status.OK)
            .apolloKey("apollo")
            .adId("ad-1")
            .build()

        val copy = roundTrip(original)

        assertEquals(42L, copy.imageId)
        assertEquals("/sdcard/a.jpg", copy.path)
        assertEquals("http://small", copy.smallPhotoUrl)
        assertEquals("http://full", copy.fullPhotoUrl)
        assertEquals(99L, copy.photoBackendId)
        assertEquals(Action.NONE, copy.action)
        assertEquals(Status.OK, copy.status)
        assertEquals("apollo", copy.apolloKey)
        assertEquals("ad-1", copy.adId)
        assertTrue(copy.isAlreadyUploaded)
        assertEquals(original, copy)
    }

    @Test
    fun `PhotoAlbum round trips and keeps its nested IGalleryItem entries`() {
        val album = PhotoAlbum("album-1", "Camera")
        album.addEntryToAlbum(PhotoFile.Builder().imageId(1L).path("/a.jpg").build())
        album.addEntryToAlbum(PhotoFile.Builder().imageId(2L).path("/b.jpg").build())

        val copy = roundTrip(album)

        assertEquals("album-1", copy.albumId)
        assertEquals("Camera", copy.name)
        assertEquals(2, copy.getAlbumEntries().size)
        assertEquals(1L, (copy.getAlbumEntries()[0] as PhotoFile).imageId)
        assertEquals(2L, (copy.getAlbumEntries()[1] as PhotoFile).imageId)
    }

    @Test
    fun `PhotoSet round trips including nested Photo fields`() {
        val set = PhotoSet().apply {
            id = "7"
            externalId = "ext"
            smallPhoto = com.mediapicker.gallery.domain.entity.Photo(10, 20, "http://s")
            bigPhoto = com.mediapicker.gallery.domain.entity.Photo(30, 40, "http://b")
        }

        val copy = roundTrip(set)

        assertEquals("7", copy.id)
        assertEquals("ext", copy.externalId)
        assertEquals("http://s", copy.smallPhoto?.url)
        assertEquals("http://b", copy.bigPhoto?.url)
        assertEquals(30, copy.bigPhoto?.width)
    }

    @Test
    fun `VideoFile round trips but never writes its Bitmap or Uri across the parcel`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val original = VideoFile(5L, Uri.parse("content://video/5"), "clip.mp4", 3000, 1024, bitmap)
        original.isSelected = true

        val copy = roundTrip(original)

        assertEquals(5L, copy.id)
        assertEquals("clip.mp4", copy.name)
        assertEquals(3000, copy.duration)
        assertEquals(1024, copy.size)
        // The whole point of @IgnoredOnParcel: no Bitmap crosses the Binder.
        assertNull(copy.thumbnail)
        assertNull(copy.uri)
        assertEquals(original, copy)
    }

    @Test
    fun `DefaultPage sealed objects round trip through a Bundle`() {
        val bundle = Bundle().apply {
            putParcelable(BaseFragment.EXTRA_DEFAULT_PAGE, DefaultPage.VideoPage)
        }
        val copy = BundleCompat.getParcelable(
            bundle, BaseFragment.EXTRA_DEFAULT_PAGE, DefaultPage::class.java
        )
        assertNotNull(copy)
        assertEquals(DefaultPage.VideoPage, copy)
    }

    @Test
    fun `photo list survives the real Bundle marshal and unmarshal used by GalleryActivity`() {
        val photos = arrayListOf(
            PhotoFile.Builder().imageId(1L).path("/a.jpg").build(),
            PhotoFile.Builder().imageId(2L).path("/b.jpg").build()
        )
        val bundle = Bundle().apply {
            putParcelableArrayList(BaseFragment.EXTRA_SELECTED_PHOTOS, photos)
        }

        // Force an actual marshal/unmarshal, the way a real Binder transaction would.
        val parcel = Parcel.obtain()
        parcel.writeBundle(bundle)
        parcel.setDataPosition(0)
        val restored = parcel.readBundle(PhotoFile::class.java.classLoader)!!
        parcel.recycle()

        val out = BundleCompat.getParcelableArrayList(
            restored, BaseFragment.EXTRA_SELECTED_PHOTOS, PhotoFile::class.java
        )
        assertEquals(2, out?.size)
        assertEquals(1L, out?.get(0)?.imageId)
        assertEquals("/b.jpg", out?.get(1)?.path)
    }

    @Test
    fun `LinkedHashSet selection order is preserved across the list based round trip`() {
        val selected = LinkedHashSet<PhotoFile>()
        (1..5).forEach { selected.add(PhotoFile.Builder().imageId(it.toLong()).path("/$it.jpg").build()) }

        val bundle = Bundle().apply {
            putParcelableArrayList(BaseFragment.EXTRA_SELECTED_PHOTOS, ArrayList(selected))
        }
        val restored = LinkedHashSet(
            BundleCompat.getParcelableArrayList(
                bundle, BaseFragment.EXTRA_SELECTED_PHOTOS, PhotoFile::class.java
            )!!
        )

        assertEquals(selected.map { it.imageId }, restored.map { it.imageId })
    }
}
