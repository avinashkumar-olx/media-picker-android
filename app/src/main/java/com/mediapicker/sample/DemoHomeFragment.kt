package com.mediapicker.sample

import android.os.Bundle
import com.mediapicker.gallery.domain.entity.PhotoFile
import com.mediapicker.gallery.presentation.fragments.HomeFragment
import com.mediapicker.gallery.presentation.utils.DefaultPage
import com.mediapicker.gallery.presentation.viewmodels.VideoFile

class DemoHomeFragment : HomeFragment() {

    override fun getLayoutId(): Int {
        return R.layout.demo_fragment_main
    }

    override fun setHomeAsUp(): Boolean = false

    override fun shouldHideToolBar(): Boolean = true

    companion object {
        fun getInstance(
            listOfSelectedPhotos: List<PhotoFile> = emptyList(),
            listOfSelectedVideos: List<VideoFile> = emptyList(),
            defaultPageType: DefaultPage = DefaultPage.PhotoPage
        ): DemoHomeFragment {
            return DemoHomeFragment().apply {
                this.arguments = Bundle().apply {
                    putParcelableArrayList(EXTRA_SELECTED_PHOTOS, ArrayList(listOfSelectedPhotos))
                    putParcelableArrayList(EXTRA_SELECTED_VIDEOS, ArrayList(listOfSelectedVideos))
                    putParcelable(EXTRA_DEFAULT_PAGE, defaultPageType)
                }
            }
        }
    }
}