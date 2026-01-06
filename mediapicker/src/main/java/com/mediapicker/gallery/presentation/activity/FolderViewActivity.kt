package com.mediapicker.gallery.presentation.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.mediapicker.gallery.domain.entity.PhotoAlbum
import com.mediapicker.gallery.domain.entity.PhotoFile
import com.mediapicker.gallery.presentation.fragments.FolderViewFragment
import com.mediapicker.gallery.presentation.fragments.GalleryPhotoViewFragment
import com.mediapicker.gallery.presentation.fragments.containsPhoto
import com.mediapicker.gallery.presentation.fragments.removePhoto
import com.mediapicker.gallery.presentation.utils.Constants.EXTRA_SELECTED_PHOTO
import com.mediapicker.gallery.presentation.utils.Constants.PHOTO_SELECTION_REQUEST_CODE


class FolderViewActivity : BaseFragmentActivity(), GalleryActionListener {

    private var currentSelectedPhotos = LinkedHashSet<PhotoFile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdgePadding()
        setCurrentSelectedPhotos()
        setFragment(FolderViewFragment.getInstance())
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun applyEdgeToEdgePadding() {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.navigationBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                topMargin = insets.top
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setCurrentSelectedPhotos() {
        currentSelectedPhotos =
            intent.getSerializableExtra(EXTRA_SELECTED_PHOTO) as LinkedHashSet<PhotoFile>
    }

    override fun moveToPhotoGrid(photoAlbum: PhotoAlbum) {
        setFragment(GalleryPhotoViewFragment.getInstance(photoAlbum, currentSelectedPhotos))
    }

    override fun onPhotoSelected(postingDraftPhoto: PhotoFile) {
        if (currentSelectedPhotos.containsPhoto(postingDraftPhoto)) {
            currentSelectedPhotos.removePhoto(postingDraftPhoto)
        } else {
            currentSelectedPhotos.add(postingDraftPhoto)
        }
    }

    override fun isPhotoAlreadySelected(postingDraftPhoto: PhotoFile): Boolean {
        if (currentSelectedPhotos.containsPhoto(postingDraftPhoto)) {
            currentSelectedPhotos.removePhoto(postingDraftPhoto)
            return true
        }
        return false
    }

    override fun onActionClicked(shouldThrowResult: Boolean) {
        if (shouldThrowResult) {
            setResult(
                Activity.RESULT_OK,
                Intent().apply { this.putExtra(EXTRA_SELECTED_PHOTO, currentSelectedPhotos) })
            finish()
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun showCrossButton() {
        // showCloseButton()
    }

//    override fun onBackPressed() {
//        val fragments = supportFragmentManager.backStackEntryCount
//        when {
//            fragments == 1 -> finish()
//            fragments > 1 -> supportFragmentManager.popBackStack()
//            else -> super.onBackPressed()
//        }
//    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val fragments = supportFragmentManager.backStackEntryCount
            when {
                fragments == 1 -> finish()
                fragments > 1 -> supportFragmentManager.popBackStack()
                else -> {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
    }
}

interface GalleryActionListener {
    fun moveToPhotoGrid(photoAlbum: PhotoAlbum)
    fun onPhotoSelected(postingDraftPhoto: PhotoFile)
    fun onActionClicked(shouldThrowResult: Boolean)
    fun isPhotoAlreadySelected(postingDraftPhoto: PhotoFile): Boolean
    fun showCrossButton()
}