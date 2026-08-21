package com.mediapicker.gallery.presentation.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.mediapicker.gallery.R
import com.olx.permify.Permify
import com.olx.permify.callback.PermissionRequestCallback
import com.olx.permify.callback.RationalPermissionCallback

object PermissionsUtil {

    private fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            Build.VERSION.SDK_INT > Build.VERSION_CODES.Q -> arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            else -> arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    private fun getMediaPermissions(): List<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /** Full library access — enough to skip re-prompting and show the gallery. */
    fun hasFullMediaAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(context, Manifest.permission.READ_MEDIA_IMAGES) ||
                isGranted(context, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            isGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Any access that can load media (full or Android 14+ partial selection).
     * Partial access still shows the upgrade banner via fragment [checkPermission].
     */
    fun hasMediaAccess(context: Context): Boolean {
        if (hasFullMediaAccess(context)) return true
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            isGranted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    }

    private fun isGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissions(
        fragment: Fragment,
        onAllPermissionsGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        // Full access already granted: skip the request so we don't hit a no-op prompt
        // and then fail to mount the gallery. Partial access still requests so the user
        // can upgrade via the "Allow" banner.
        if (hasFullMediaAccess(fragment.requireContext())) {
            runAfterFragmentTransactions(fragment, onAllPermissionsGranted)
            return
        }

        val permissions = getRequiredPermissions()
        Permify.requestPermission(
            fragment = fragment,
            permissions = permissions.toList(),
            showDialogs = false,
            rationalPermissionCallback = object : RationalPermissionCallback {
                override fun onRationalPermissionCallback(temporaryPermissionDenied: List<String>) {
                }
            },
            permissionRequestCallback = object : PermissionRequestCallback {
                override fun onResult(
                    allGranted: Boolean,
                    grantedList: List<String>,
                    deniedList: List<String>
                ) {
                    val grantedMap = permissions.associateWith { permission ->
                        grantedList.contains(permission)
                    }
                    // Permify invokes this synchronously. A pending permission result is
                    // replayed during Fragment.performResume, i.e. while FragmentManager is
                    // still executing transactions, so callers that swap a ViewPager adapter
                    // would crash with "FragmentManager is already executing transactions".
                    // Deferring to the next main-loop message lets the transaction finish first.
                    runAfterFragmentTransactions(fragment) {
                        handlePermissionsResult(
                            fragment.requireActivity(),
                            grantedMap,
                            onAllPermissionsGranted,
                            onPermissionDenied
                        )
                    }
                }
            }
        )
    }

    private fun runAfterFragmentTransactions(fragment: Fragment, action: () -> Unit) {
        val runner = Runnable {
            if (fragment.isAdded) action()
        }
        fragment.view?.post(runner)
            ?: fragment.activity?.window?.decorView?.post(runner)
            ?: runner.run()
    }

    fun handlePermissionsResult(
        activity: FragmentActivity,
        granted: Map<String, Boolean>,
        onAllPermissionsGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        // Gallery needs media access only. Full (READ_MEDIA_*) or partial
        // (READ_MEDIA_VISUAL_USER_SELECTED) both count — do not require CAMERA or
        // treat partial-only as failure when full access was granted.
        if (hasMediaAccess(activity)) {
            onAllPermissionsGranted()
        } else {
            handleDeniedPermissions(activity, granted, onPermissionDenied)
        }
    }

    private fun handleDeniedPermissions(
        activity: FragmentActivity,
        granted: Map<String, Boolean>,
        onPermissionDenied: () -> Unit
    ) {
        val mediaPermissions = getMediaPermissions()
        val deniedMediaPermissions = granted
            .filter { !it.value && it.key in mediaPermissions }
            .keys

        deniedMediaPermissions.forEach { permission ->
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                showNeverAskAgainPermission(activity)
                return
            }
        }

        onPermissionDenied()
    }

    private fun showNeverAskAgainPermission(activity: FragmentActivity) {
        Toast.makeText(activity, activity.getString(R.string.permissions_denied_never_ask_again), Toast.LENGTH_LONG).show()

        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.permissions_required_title))
            .setMessage(activity.getString(R.string.permissions_required_message))
            .setPositiveButton(activity.getString(R.string.settings)) { _, _ ->
                openAppSettings(activity)
            }
            .setNegativeButton(activity.getString(R.string.cancel), null)
            .show()
    }

    fun showPermissionRationale(activity: AppCompatActivity) {
        Toast.makeText(activity, activity.getString(R.string.permissions_denied_rationale), Toast.LENGTH_LONG).show()
    }

    fun openAppSettings(context: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
}
