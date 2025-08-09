package com.mediapicker.gallery.presentation.fragments

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.mediapicker.gallery.Gallery
import com.mediapicker.gallery.GalleryConfig
import com.mediapicker.gallery.R
import com.mediapicker.gallery.databinding.OssFragmentCarousalBinding
import com.mediapicker.gallery.domain.contract.GalleryPagerCommunicator
import com.mediapicker.gallery.domain.entity.GalleryViewMediaType
import com.mediapicker.gallery.domain.entity.MediaGalleryEntity
import com.mediapicker.gallery.domain.entity.PhotoFile
import com.mediapicker.gallery.presentation.activity.GalleryActivity
import com.mediapicker.gallery.presentation.activity.MediaGalleryActivity
import com.mediapicker.gallery.presentation.adapters.PagerAdapter
import com.mediapicker.gallery.presentation.carousalview.CarousalActionListener
import com.mediapicker.gallery.presentation.carousalview.MediaGalleryView
import com.mediapicker.gallery.presentation.utils.DefaultPage
import com.mediapicker.gallery.presentation.utils.PermissionsUtil
import com.mediapicker.gallery.presentation.utils.getActivityScopedViewModel
import com.mediapicker.gallery.presentation.utils.getFragmentScopedViewModel
import com.mediapicker.gallery.presentation.viewmodels.BridgeViewModel
import com.mediapicker.gallery.presentation.viewmodels.HomeViewModel
import com.mediapicker.gallery.presentation.viewmodels.VideoFile
import com.mediapicker.gallery.utils.SnackbarUtils
import java.io.Serializable

private const val PHOTO_PREVIEW = 43475

open class PhotoCarousalFragment : BaseFragment(), GalleryPagerCommunicator,
    MediaGalleryView.OnGalleryItemClickListener {

    companion object {
        private const val TAG = "PhotoCarousalFragment"

        fun getInstance(
            listOfSelectedPhotos: List<PhotoFile> = emptyList(),
            listOfSelectedVideos: List<VideoFile> = emptyList(),
            defaultPageType: DefaultPage = DefaultPage.PhotoPage
        ): PhotoCarousalFragment {
            return PhotoCarousalFragment().apply {
                this.arguments = Bundle().apply {
                    putSerializable(EXTRA_SELECTED_PHOTOS, listOfSelectedPhotos as Serializable)
                    putSerializable(EXTRA_SELECTED_VIDEOS, listOfSelectedVideos as Serializable)
                    putSerializable(EXTRA_DEFAULT_PAGE, defaultPageType)
                }
            }
        }
    }

    private var permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            PermissionsUtil.handlePermissionsResult(
                requireActivity(),
                granted,
                onAllPermissionsGranted = { checkPermissions() },
                onPermissionDenied = { onPermissionDenied() }
            )
        }
    private var photoPreviewLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val index = data?.extras?.getInt("gallery_media_index", 0) ?: 0
                ossFragmentCarousalBinding?.mediaGalleryView?.setSelectedPhoto(index)
            }
        }

    private val homeViewModel: HomeViewModel by lazy {
        getFragmentScopedViewModel { HomeViewModel(Gallery.galleryConfig) }
    }

    private val bridgeViewModel: BridgeViewModel by lazy {
        getActivityScopedViewModel {
            BridgeViewModel(
                getPhotosFromArguments(),
                getVideosFromArguments()
            )
        }
    }

    private val defaultPageToOpen: DefaultPage by lazy {
        getPageFromArguments()
    }

    private val ossFragmentCarousalBinding: OssFragmentCarousalBinding? by lazy {
        ossFragmentBaseBinding?.baseContainer?.findViewById<LinearLayout>(R.id.linear_layout_parent)
            ?.let { OssFragmentCarousalBinding.bind(it) }
    }

    override fun getLayoutId() = R.layout.oss_fragment_carousal

    override fun getScreenTitle() =
        Gallery.galleryConfig.galleryLabels.homeTitle?.ifBlank { getString(R.string.oss_title_home_screen) }
            ?: getString(R.string.oss_title_home_screen)

    override fun setUpViews() {
        Log.d(TAG, "setUpViews is called")
        Gallery.pagerCommunicator = this

        if (Gallery.galleryConfig.showPreviewCarousal.showCarousal) {
            ossFragmentCarousalBinding?.mediaGalleryViewContainer?.visibility = View.VISIBLE
            ossFragmentCarousalBinding?.mediaGalleryView?.setOnGalleryClickListener(this)
            if (Gallery.galleryConfig.showPreviewCarousal.imageId != 0) {
                ossFragmentCarousalBinding?.mediaGalleryView?.updateDefaultPhoto(Gallery.galleryConfig.showPreviewCarousal.imageId)
            }
            if (Gallery.galleryConfig.showPreviewCarousal.previewText != 0) {
                ossFragmentCarousalBinding?.mediaGalleryView?.updateDefaultText(Gallery.galleryConfig.showPreviewCarousal.previewText)
            }
        }

        ossFragmentCarousalBinding?.actionButton?.text =
            if (Gallery.galleryConfig.galleryLabels.homeAction?.isNotBlank() == true)
                Gallery.galleryConfig.galleryLabels.homeAction
            else
                getString(R.string.oss_posting_next)
        ossFragmentCarousalBinding?.actionButton?.isAllCaps = Gallery.galleryConfig.textAllCaps
        ossFragmentCarousalBinding?.actionButton?.text =
            Gallery.galleryConfig.galleryLabels.homeAction?.ifBlank { getString(R.string.oss_posting_next) }
                ?: getString(R.string.oss_posting_next)
        ossFragmentBaseBinding?.ossCustomTool?.apply {
            toolbarTitle.isAllCaps = Gallery.galleryConfig.textAllCaps
            toolbarTitle.gravity = Gallery.galleryConfig.galleryLabels.titleAlignment
            toolbarBackButton.setImageResource(Gallery.galleryConfig.galleryUiConfig.backIcon)
        }
        ossFragmentCarousalBinding?.button?.setOnClickListener {
            requestPermissions()
        }
        requestPermissions()
    }


    private fun checkPermissions() {
        Log.d(TAG, "checkPermissions is called")
        if (!isRemoving && isAdded) {
            when (homeViewModel.getMediaType()) {
                GalleryConfig.MediaType.PhotoOnly -> {
                    setUpWithOutTabLayout()
                }

                GalleryConfig.MediaType.PhotoWithFolderOnly -> {
                    setUpWithOutTabLayout()
                }

                GalleryConfig.MediaType.PhotoWithoutCameraFolderOnly -> {
                    setUpWithOutTabLayout()
                }

                else -> {
                    setUpWithOutTabLayout()
                }
            }
            openPage()
            ossFragmentCarousalBinding?.actionButton?.apply {
                isSelected = false
                setOnClickListener { onActionButtonClicked() }
            }
            checkPermission()
        }
    }

    private fun onPermissionDenied() {
        Log.d(TAG, "onPermissionDenied is called")
        checkPermission()
        Gallery.galleryConfig.galleryCommunicator?.onPermissionDenied()
    }

    private fun addMediaForPager(mediaGalleryEntity: MediaGalleryEntity) {
        Log.d(TAG, "addMediaForPager is called")
        ossFragmentCarousalBinding?.mediaGalleryView?.addMediaForPager(mediaGalleryEntity)
    }

    private fun removeMediaFromPager(mediaGalleryEntity: MediaGalleryEntity) {
        Log.d(TAG, "removeMediaFromPager is called")
        ossFragmentCarousalBinding?.mediaGalleryView?.removeMediaFromPager(mediaGalleryEntity)
    }

    private fun showNeverAskAgainPermission() {
        Log.d(TAG, "showNeverAskAgainPermission is called")
        Gallery.galleryConfig.galleryCommunicator?.onNeverAskPermissionAgain()
    }

    override fun initViewModels() {
        Log.d(TAG, "initViewModels is called")
        super.initViewModels()
        bridgeViewModel.getActionState().observe(this) { changeActionButtonState(it) }
        bridgeViewModel.getError().observe(this) { showError(it) }
        bridgeViewModel.getClosingSignal().observe(this) { closeIfHostingOnActivity() }
    }

    private fun closeIfHostingOnActivity() {
        Log.d(TAG, "closeIfHostingOnActivity is called")
        if (requireActivity() is GalleryActivity) {
            requireActivity().finish()
        }
    }

    override fun setHomeAsUp() = run {
        Log.d(TAG, "setHomeAsUp is called")
        true
    }

    fun setActionButtonLabel(label: String) {
        Log.d(TAG, "setActionButtonLabel is called")
        ossFragmentCarousalBinding?.actionButton?.text = label
    }

    fun setCarousalActionListener(carousalActionListener: CarousalActionListener?) {
        Log.d(TAG, "setCarousalActionListener is called")
        Gallery.carousalActionListener = carousalActionListener
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed is called")
        closeIfHostingOnActivity()
        bridgeViewModel.onBackPressed()
    }

    private fun changeActionButtonState(state: Boolean) {
        Log.d(TAG, "changeActionButtonState is called")
        Gallery.galleryConfig.galleryCommunicator?.onStepValidate(state)
        ossFragmentCarousalBinding?.actionButton?.isSelected = state
    }

    private fun showError(error: String) {
        Log.d(TAG, "showError is called")
        view?.let { SnackbarUtils.show(it, error, Snackbar.LENGTH_LONG) }
    }

    private fun setUpWithOutTabLayout() {
        Log.d(TAG, "setUpWithOutTabLayout is called")
        ossFragmentCarousalBinding?.tabLayout?.visibility = View.GONE
        ossFragmentCarousalBinding?.mediaGalleryView?.setImagesForPager(
            convertPhotoFileToMediaGallery(
                getPhotosFromArguments()
            )
        )
        PagerAdapter(
            childFragmentManager,
            listOf(
                PhotoGridFragment.getInstance(
                    getString(R.string.oss_title_tab_photo),
                    getPhotosFromArguments()
                )
            )
        ).apply {
            ossFragmentCarousalBinding?.viewPager?.adapter = this
        }
    }

    private fun openPage() {
        Log.d(TAG, "openPage is called")
        if (defaultPageToOpen is DefaultPage.PhotoPage) {
            ossFragmentCarousalBinding?.viewPager?.currentItem = 0
        } else {
            ossFragmentCarousalBinding?.viewPager?.currentItem = 1
        }
    }

    private fun onActionButtonClicked() {
        Log.d(TAG, "onActionButtonClicked is called")
        bridgeViewModel.complyRules()
    }

    private fun setUpWithTabLayout() {
        Log.d(TAG, "setUpWithTabLayout is called")
        ossFragmentCarousalBinding?.viewPager.apply {
            PagerAdapter(
                childFragmentManager, listOf(
                    PhotoGridFragment.getInstance(
                        getString(R.string.oss_title_tab_photo),
                        getPhotosFromArguments()
                    ),
                    VideoGridFragment.getInstance(
                        getString(R.string.oss_title_tab_video),
                        getVideosFromArguments()
                    )
                )
            ).apply {
                ossFragmentCarousalBinding?.viewPager?.adapter = this
            }
            ossFragmentCarousalBinding?.tabLayout?.setupWithViewPager(ossFragmentCarousalBinding?.viewPager)
        }
    }

//    private fun setUpWithTabLayout() {
//        PagerAdapter(
//            childFragmentManager, listOf(
//                PhotoGridFragment.getInstance(
//                    getString(R.string.oss_title_tab_photo),
//                    getPhotosFromArguments()
//                ),
//                VideoGridFragment.getInstance(
//                    getString(R.string.oss_title_tab_video),
//                    getVideosFromArguments()
//                )
//            )
//        ).apply { ossFragmentCarousalBinding?.viewPager?.adapter = this }
//        ossFragmentCarousalBinding?.tabLayout?.setupWithViewPager(ossFragmentCarousalBinding?.viewPager)
//    }


    private fun getPageFromArguments(): DefaultPage {
        Log.d(TAG, "getPageFromArguments is called")
        this.arguments?.let {
            if (it.containsKey(EXTRA_DEFAULT_PAGE)) {
                return it.getSerializable(EXTRA_DEFAULT_PAGE) as DefaultPage
            }
        }
        return DefaultPage.PhotoPage
    }

    fun reloadMedia() {
        Log.d(TAG, "reloadMedia is called")
        bridgeViewModel.reloadMedia()
    }

    override fun onItemClicked(photoFile: PhotoFile, isSelected: Boolean) {
        Log.d(TAG, "onItemClicked is called")
        if (isSelected) {
            if (Gallery.galleryConfig.showPreviewCarousal.addImage) {
                addMediaForPager(getMediaEntity(photoFile))
            }
        } else {
            if (Gallery.galleryConfig.showPreviewCarousal.addImage) {
                removeMediaFromPager(getMediaEntity(photoFile))
            }
        }
    }

    private fun getMediaEntity(photo: PhotoFile): MediaGalleryEntity {
        Log.d(TAG, "getMediaEntity is called")
        var path: String? = photo.fullPhotoUrl
        var isLocalImage = false
        if (!TextUtils.isEmpty(photo.path) && photo.path?.contains("/")!!) {
            path = photo.path
            isLocalImage = true
        }
        return MediaGalleryEntity(
            photo.path,
            photo.imageId,
            path,
            isLocalImage,
            GalleryViewMediaType.IMAGE
        )
    }

    private fun convertPhotoFileToMediaGallery(photoList: List<PhotoFile>): ArrayList<MediaGalleryEntity> {
        Log.d(TAG, "convertPhotoFileToMediaGallery is called")
        val mediaList = ArrayList<MediaGalleryEntity>()
        for (photo in photoList) {
            mediaList.add(getMediaEntity(photo))
        }
        return mediaList
    }

    override fun onPreviewItemsUpdated(listOfSelectedPhotos: List<PhotoFile>) {
        Log.d(TAG, "onPreviewItemsUpdated is called")
        if (Gallery.galleryConfig.showPreviewCarousal.addImage) {
            ossFragmentCarousalBinding?.mediaGalleryView?.setImagesForPager(
                convertPhotoFileToMediaGallery(listOfSelectedPhotos)
            )
        }
    }

    override fun onGalleryItemClick(mediaIndex: Int) {
        Log.d(TAG, "onGalleryItemClick is called")
        Gallery.carousalActionListener?.onGalleryImagePreview(
            mediaIndex,
            bridgeViewModel.getSelectedPhotos().size
        )
        val intent = MediaGalleryActivity.createIntent(
            this,
            convertPhotoFileToMediaGallery(bridgeViewModel.getSelectedPhotos()),
            mediaIndex,
            ""
        )
        photoPreviewLauncher.launch(intent)
    }

    private fun requestPermissions() {
        Log.d(TAG, "requestPermissions is called")
        PermissionsUtil.requestPermissions(requireActivity(), permissionLauncher)
    }

    private fun checkPermission() {
        Log.d(TAG, "checkPermission is called")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED)
        ) {
            // Full access on Android 13 (API level 33) or higher
            ossFragmentCarousalBinding?.permissionAccessManagement?.visibility = View.GONE
        } else if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Partial access on Android 14 (API level 34) or higher
            ossFragmentCarousalBinding?.textView?.text =
                getString(R.string.photos_partially_granted)
            ossFragmentCarousalBinding?.button?.text = getString(R.string.allow)
            ossFragmentCarousalBinding?.permissionAccessManagement?.visibility = View.VISIBLE
        } else if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Full access up to Android 12 (API level 32)
            ossFragmentCarousalBinding?.permissionAccessManagement?.visibility = View.GONE
        } else {
            // Access denied
            ossFragmentCarousalBinding?.textView?.text = getString(R.string.photos_denied)
            ossFragmentCarousalBinding?.button?.text = getString(R.string.allow)
            ossFragmentCarousalBinding?.permissionAccessManagement?.visibility = View.VISIBLE
        }
    }
}
