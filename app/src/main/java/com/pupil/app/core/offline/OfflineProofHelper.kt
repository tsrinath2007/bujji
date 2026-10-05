package com.pupil.app.core.offline

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

object OfflineProofHelper {

    /**
     * Inspects the APK manifest package information at runtime to return all requested permissions.
     */
    fun getRequestedPermissions(context: Context): List<String> {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            }
            packageInfo.requestedPermissions?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Verifies at runtime whether android.permission.INTERNET is requested.
     * Guaranteed to return false for Pupil.
     */
    fun isInternetPermissionDeclared(context: Context): Boolean {
        val permissions = getRequestedPermissions(context)
        return permissions.any { it.equals(Manifest.permission.INTERNET, ignoreCase = true) }
    }

    /**
     * Checks if system Airplane Mode is currently enabled.
     */
    fun isAirplaneModeOn(context: Context): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        } catch (e: Exception) {
            false
        }
    }
}
