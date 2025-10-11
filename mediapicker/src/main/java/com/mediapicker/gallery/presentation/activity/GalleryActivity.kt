package com.mediapicker.gallery.presentation.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.mediapicker.gallery.domain.entity.PhotoFile
import com.mediapicker.gallery.presentation.fragments.BaseFragment
import com.mediapicker.gallery.presentation.fragments.HomeFragment
import com.mediapicker.gallery.presentation.utils.DefaultPage
import com.mediapicker.gallery.presentation.viewmodels.VideoFile
import java.io.Serializable

class GalleryActivity : BaseFragmentActivity() {

    companion object {
        fun getGalleryActivityIntent(
            listOfSelectedPhotos: List<PhotoFile> = emptyList(),
            listOfSelectedVideos: List<VideoFile> = emptyList(),
            defaultPageType: DefaultPage = DefaultPage.PhotoPage,
            context: Context
        ): Intent {
            return Intent(context, GalleryActivity::class.java).apply {
                putExtras(Bundle().apply {
                    this.putSerializable(
                        BaseFragment.EXTRA_SELECTED_PHOTOS,
                        listOfSelectedPhotos as Serializable
                    )
                    this.putSerializable(
                        BaseFragment.EXTRA_SELECTED_VIDEOS,
                        listOfSelectedVideos as Serializable
                    )
                    this.putSerializable(BaseFragment.EXTRA_DEFAULT_PAGE, defaultPageType)
                })
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.navigationBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                topMargin = insets.top
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }
            WindowInsetsCompat.CONSUMED
        }
        setFragment(
            HomeFragment.getInstance(
                getSelectedPhotos(),
                getSelectedVideos(),
                getDefaultPage()
            ), false
        )
    }

    private fun getSelectedPhotos(): List<PhotoFile> {
        val list = intent.extras?.getSerializable(BaseFragment.EXTRA_SELECTED_PHOTOS) as List<*>
        return list.filterIsInstance<PhotoFile>()
    }

    private fun getSelectedVideos(): List<VideoFile> {
        val list = intent.extras?.getSerializable(BaseFragment.EXTRA_SELECTED_PHOTOS) as List<*>
        return list.filterIsInstance<VideoFile>()
    }

    private fun getDefaultPage(): DefaultPage {
        return intent.extras?.getSerializable(BaseFragment.EXTRA_DEFAULT_PAGE) as DefaultPage
    }
}