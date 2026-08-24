package com.mediapicker.gallery.data.repositories

import android.content.ContentResolver
import android.database.Cursor
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.mediapicker.gallery.domain.entity.PhotoAlbum
import com.mediapicker.gallery.domain.entity.PhotoFile
import com.mediapicker.gallery.domain.repositories.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.emptySet

open class GalleryService(private val contentResolver: ContentResolver) : GalleryRepository {

    companion object {
        const val COL_FULL_PHOTO_URL = "fullPhotoUrl"
    }


    override suspend fun getAlbums(): HashSet<PhotoAlbum> {
        return runCatching {
            queryMedia()
        }.getOrElse {
            hashSetOf()
        }
    }

    override suspend fun getAlbumPhotos(albumId: String): List<PhotoFile> {
        return runCatching {
            queryPhotosInBucket(albumId)
        }.getOrElse {
            emptyList()
        }
    }

    private suspend fun queryPhotosInBucket(albumId: String): List<PhotoFile> =
        withContext(Dispatchers.IO) {
            val selection = "${MediaStore.Images.Media.BUCKET_ID} =? AND " +
                    "${MediaStore.Images.Media.MIME_TYPE} !=?"
            val selectionArgs = arrayOf(albumId, gifMimeType())
            val photos = mutableListOf<PhotoFile>()
            MediaStore.Images.Media.query(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, selection, selectionArgs,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        photos.add(getPhoto(cursor))
                    } while (cursor.moveToNext())
                }
            }
            return@withContext photos
        }

    private fun gifMimeType() =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension("gif") ?: "image/gif"

    /**
     * Builds the folder list. Each album keeps only its newest photo — the cover shown by
     * GalleryFolderAdapter. The remaining entries are fetched per album by [getAlbumPhotos]
     * when that folder is actually opened, so an album never holds (nor parcels) the whole
     * bucket. The cursor is sorted DATE_ADDED DESC, so the first row seen for a bucket is
     * the newest photo.
     */
    private suspend fun queryMedia(): HashSet<PhotoAlbum> = withContext(Dispatchers.IO) {
        val mutableListOfFolders = hashSetOf<PhotoAlbum>()
        val selection = MediaStore.Images.Media.MIME_TYPE + "!=?"
        val selectionTypeGifArgs = arrayOf(gifMimeType())
        MediaStore.Images.Media.query(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, selection, selectionTypeGifArgs,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val album = getAlbumEntry(cursor)
                    album?.let { item ->
                        if (!mutableListOfFolders.contains(item)) {
                            item.addEntryToAlbum(getPhoto(cursor))
                            mutableListOfFolders.add(item)
                        }
                    }
                } while (cursor.moveToNext())
            }
        }
        return@withContext mutableListOfFolders
    }

    private suspend fun getAlbumEntry(cursor: Cursor): PhotoAlbum? = withContext(Dispatchers.IO) {
        val albumIdIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)

        val albumNameIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        if (albumIdIndex != -1 && albumNameIndex != -1) {
            val id = cursor.getInt(albumIdIndex)
            val name = cursor.getString(albumNameIndex)
            return@withContext PhotoAlbum(id.toString(), name)
        } else {
            return@withContext null
        }

    }

    private suspend fun getPhoto(cursor: Cursor): PhotoFile = withContext(Dispatchers.IO) {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
        val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
        val mimeType =
            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))
        val col = cursor.getColumnIndex(COL_FULL_PHOTO_URL)
        var fullPhotoUrl = ""
        if (col != -1) {
            fullPhotoUrl = cursor.getString(col)
        }
        return@withContext PhotoFile.Builder()
            .imageId(id)
            .path(path)
            .smallPhotoUrl("")
            .fullPhotoUrl(fullPhotoUrl)
            .photoBackendId(0L)
            .mimeType()
            .build()
    }

}
