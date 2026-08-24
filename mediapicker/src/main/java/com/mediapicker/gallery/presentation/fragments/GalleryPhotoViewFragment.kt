package com.mediapicker.gallery.presentation.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mediapicker.gallery.Gallery
import com.mediapicker.gallery.R
import com.mediapicker.gallery.data.repositories.GalleryService
import com.mediapicker.gallery.databinding.OssFragmentFolderViewBinding
import com.mediapicker.gallery.domain.entity.PhotoFile
import com.mediapicker.gallery.presentation.adapters.IGalleryItemClickListener
import com.mediapicker.gallery.presentation.adapters.SelectPhotoImageAdapter
import com.mediapicker.gallery.presentation.utils.Constants.EXTRA_SELECTED_ALBUM_ID
import com.mediapicker.gallery.presentation.utils.Constants.EXTRA_SELECTED_ALBUM_NAME
import com.mediapicker.gallery.presentation.utils.ItemDecorationAlbumColumns
import com.mediapicker.gallery.presentation.utils.ValidatePhotos
import com.mediapicker.gallery.presentation.utils.ValidationResult
import com.mediapicker.gallery.presentation.utils.getFragmentScopedViewModel
import com.mediapicker.gallery.presentation.viewmodels.LoadAlbumViewModel
import com.mediapicker.gallery.utils.SnackbarUtils

const val COLUMNS_COUNT = 3

class GalleryPhotoViewFragment : BaseGalleryViewFragment() {

    /**
     * Lazy so it is safe to touch from either [initViewModels] or [setUpViews] — the album
     * photos arrive asynchronously and may land before the views are set up.
     */
    val adapter: SelectPhotoImageAdapter by lazy {
        SelectPhotoImageAdapter(
            emptyList(),
            currentSelectedPhotos.toList(),
            galleryItemClickListener,
            fromGallery = false
        )
    }

    private var photoValidationAction: ValidatePhotos = ValidatePhotos()

    private val loadAlbumViewModel: LoadAlbumViewModel? by lazy {
        context?.contentResolver?.let { contentResolver ->
            getFragmentScopedViewModel {
                LoadAlbumViewModel(GalleryService(contentResolver))
            }
        }
    }

    private val albumId: String by lazy {
        requireArguments().getString(EXTRA_SELECTED_ALBUM_ID).orEmpty()
    }

    private val albumName: String by lazy {
        requireArguments().getString(EXTRA_SELECTED_ALBUM_NAME).orEmpty()
    }

    /**
     * The activity owns the selection; this is the same set instance, not a copy, so adds and
     * removes made here stay in sync with the result the activity returns.
     */
    private val currentSelectedPhotos: LinkedHashSet<PhotoFile>
        get() = galleryActionListener?.getCurrentSelectedPhotos() ?: LinkedHashSet()

    private val ossFragmentFolderView: OssFragmentFolderViewBinding? by lazy {
        ossFragmentBaseBinding?.baseContainer?.findViewById<LinearLayout>(R.id.linear_layout_parent)
            ?.let { OssFragmentFolderViewBinding.bind(it) }
    }

    private fun removePhotoFromSelection(photo: PhotoFile) {
        currentSelectedPhotos.removePhoto(photo)
        Gallery.carousalActionListener?.onItemClicked(photo, false)
        refreshSelection()
    }

    private fun refreshSelection() {
        adapter.listCurrentPhotos = currentSelectedPhotos.toList()
        adapter.notifyDataSetChanged()
    }

    private fun showError(msg: String?) {
        SnackbarUtils.show(view, msg, Snackbar.LENGTH_SHORT)
    }

    override fun getScreenTitle() = albumName

    override fun getLayoutId() = R.layout.oss_fragment_folder_view

    override fun initViewModels() {
        super.initViewModels()
        loadAlbumViewModel?.getAlbumPhotos()?.observe(this) { setAlbumPhotos(it) }
        loadAlbumViewModel?.loadAlbumPhotos(albumId)
    }

    private fun setAlbumPhotos(photos: List<PhotoFile>) {
        adapter.listCurrentPhotos = currentSelectedPhotos.toList()
        adapter.updateGalleryItems(photos)
    }

    override fun setUpViews() {
        ossFragmentFolderView?.actionButton?.setOnClickListener { onActionButtonClick() }

        ossFragmentFolderView?.folderRV?.apply {
            this.addItemDecoration(
                ItemDecorationAlbumColumns(
                    resources.getDimensionPixelSize(R.dimen.module_base),
                    COLUMNS_COUNT
                )
            )
            this.layoutManager = GridLayoutManager(activity, COLUMNS_COUNT)
            this.adapter = this@GalleryPhotoViewFragment.adapter
        }

        ossFragmentFolderView?.actionButton?.isSelected = true

        if (Gallery.galleryConfig?.galleryLabels?.galleryFolderAction?.isNotBlank() == true) {
            ossFragmentFolderView?.actionButton?.text =
                Gallery.galleryConfig?.galleryLabels?.galleryFolderAction
        }
        ossFragmentBaseBinding?.ossCustomTool?.toolbarTitle?.isAllCaps =
            Gallery.galleryConfig?.textAllCaps == true
        ossFragmentFolderView?.actionButton?.isAllCaps = Gallery.galleryConfig?.textAllCaps == true
    }

    @SuppressLint("CheckResult")
    private fun handleItemClickListener(photo: PhotoFile, position: Int) {
        if (currentSelectedPhotos.containsPhoto(photo)) {
            removePhotoFromSelection(photo)
        } else {
            validateNewPhoto(photo)
        }
    }

    private fun validateNewPhoto(photo: PhotoFile) {
        when (val validationResult =
            photoValidationAction.canAddThisToList(currentSelectedPhotos.size, photo)) {
            is ValidationResult.Success -> {
                galleryActionListener?.onPhotoSelected(photo)
                Gallery.carousalActionListener?.onItemClicked(photo, true)
                refreshSelection()
            }

            is ValidationResult.Failure -> {
                showError(validationResult.msg)
            }
        }
    }

    private fun onActionButtonClick() {
        galleryActionListener?.onActionClicked(false)
    }

    override fun setHomeAsUp() = true

    private val galleryItemClickListener = object : IGalleryItemClickListener {

        override fun onPhotoItemClick(photoFile: PhotoFile, position: Int) {
            handleItemClickListener(photoFile, position)
        }

        override fun onFolderItemClick() {

        }

        override fun onCameraIconClick() {

        }
    }

    companion object {
        /**
         * Only the album identity travels in the arguments. Passing the album itself put every
         * photo of the bucket into the saved-state parcel, which blew past the 1MB binder limit
         * on large folders (TransactionTooLargeException). Entries are re-queried from
         * MediaStore instead.
         */
        fun getInstance(albumId: String, albumName: String) =
            GalleryPhotoViewFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_SELECTED_ALBUM_ID, albumId)
                    putString(EXTRA_SELECTED_ALBUM_NAME, albumName)
                }
            }
    }
}
