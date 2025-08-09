package com.mediapicker.gallery.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mediapicker.gallery.Gallery
import com.mediapicker.gallery.GalleryConfig
import com.mediapicker.gallery.domain.action.RuleAction
import com.mediapicker.gallery.domain.entity.PhotoFile

class BridgeViewModel(
    private var listOfSelectedPhotos: List<PhotoFile>,
    private var listOfSelectedVideos: List<VideoFile>,
) : ViewModel() {

    companion object {
        private const val TAG = "BridgeViewModel"
    }

    private val galleryConfig: GalleryConfig
        get() = Gallery.galleryConfig

    private val ruleAction: RuleAction
        get() = RuleAction(Gallery.galleryConfig.validation)

    private val reloadMediaLiveData = MutableLiveData<Unit>()

    private val recordVideoLiveData = MutableLiveData<Unit>()

    private val actionButtonStateLiveData = MutableLiveData<Boolean>()

    private val errorStateLiveData = MutableLiveData<String>()

    private val closeHostingViewLiveData = MutableLiveData<Boolean>()

    fun recordVideoWithNativeCamera(): MutableLiveData<Unit> {
        Log.d(TAG, "recordVideoWithNativeCamera is called")
        return recordVideoLiveData
    }

    fun getActionState(): MutableLiveData<Boolean> {
        Log.d(TAG, "getActionState is called")
        return actionButtonStateLiveData
    }

    fun getMediaStateLiveData(): MutableLiveData<Unit> {
        Log.d(TAG, "getMediaStateLiveData is called")
        return reloadMediaLiveData
    }

    fun getError(): MutableLiveData<String> {
        Log.d(TAG, "getError is called")
        return errorStateLiveData
    }

    fun getClosingSignal(): MutableLiveData<Boolean> {
        Log.d(TAG, "getClosingSignal is called")
        return closeHostingViewLiveData
    }

    fun setCurrentSelectedPhotos(listOfSelectedPhotos: List<PhotoFile>) {
        Log.d(TAG, "setCurrentSelectedPhotos is called")
        galleryConfig.galleryCommunicator?.onImageUpdated()
        this.listOfSelectedPhotos = listOfSelectedPhotos
        shouldEnableActionButton()
    }

    fun setCurrentSelectedVideos(listOfSelectedVideos: List<VideoFile>) {
        Log.d(TAG, "setCurrentSelectedVideos is called")
        this.listOfSelectedVideos = listOfSelectedVideos
        shouldEnableActionButton()
    }

    fun getSelectedPhotos(): List<PhotoFile> {
        Log.d(TAG, "getSelectedPhotos is called")
        return listOfSelectedPhotos
    }

    private fun shouldEnableActionButton() {
        Log.d(TAG, "shouldEnableActionButton is called")
        if(galleryConfig.shouldOnlyValidatePhoto()){
            val status = ruleAction.shouldEnableActionButton(listOfSelectedPhotos.size)
            actionButtonStateLiveData.postValue(status)
        }else{
            val status = ruleAction.shouldEnableActionButton(Pair(listOfSelectedPhotos.size, listOfSelectedVideos.size))
            actionButtonStateLiveData.postValue(status)
        }
    }

    private fun onActionButtonClick() {
        Log.d(TAG, "onActionButtonClick is called")
        galleryConfig.galleryCommunicator?.actionButtonClick(listOfSelectedPhotos, listOfSelectedVideos)
    }


    fun shouldRecordVideo() {
        Log.d(TAG, "shouldRecordVideo is called")
        if (galleryConfig.shouldUseVideoCamera) {
            recordVideoLiveData.postValue(Unit)
        } else {
            galleryConfig.galleryCommunicator?.recordVideo()
        }
    }

    fun onBackPressed() {
        Log.d(TAG, "onBackPressed is called")
        galleryConfig.galleryCommunicator?.onCloseMainScreen()
    }

    fun getMaxSelectionLimit(): Int {
        Log.d(TAG, "getMaxSelectionLimit is called")
        return galleryConfig.validation.getMaxPhotoSelectionRule().maxSelectionLimit
    }

    fun getMaxVideoSelectionLimit(): Int {
        Log.d(TAG, "getMaxVideoSelectionLimit is called")
        return galleryConfig.validation.getMaxVideoSelectionRule().maxSelectionLimit
    }

    fun getMaxLimitErrorResponse(): String {
        Log.d(TAG, "getMaxLimitErrorResponse is called")
        return galleryConfig.validation.getMaxPhotoSelectionRule().message
    }

    fun getMaxVideoLimitErrorResponse(): String {
        Log.d(TAG, "getMaxVideoLimitErrorResponse is called")
        return galleryConfig.validation.getMaxVideoSelectionRule().message
    }

    fun reloadMedia() {
        Log.d(TAG, "reloadMedia is called")
        reloadMediaLiveData.postValue(Unit)
    }

    fun shouldUseMyCamera(): Boolean {
        Log.d(TAG, "shouldUseMyCamera is called")
        galleryConfig.galleryCommunicator?.captureImage()
        galleryConfig.galleryCommunicator?.takePicture()
        return galleryConfig.shouldUsePhotoCamera
    }

    fun onFolderSelect() {
        Log.d(TAG, "onFolderSelect is called")
        galleryConfig.galleryCommunicator?.onFolderSelect()
    }

    fun complyRules() {
        Log.d(TAG, "complyRules is called")
        val error = ruleAction.getFirstFailingMessage(Pair(listOfSelectedPhotos.size, listOfSelectedVideos.size))
        if (error.isEmpty()) {
            onActionButtonClick()
            closeHostingViewLiveData.postValue(true)
        } else {
            errorStateLiveData.postValue(error)
        }
    }

}