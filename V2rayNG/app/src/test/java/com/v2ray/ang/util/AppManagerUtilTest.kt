package com.v2ray.ang.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Covers the per-app list filter: only packages that can open a socket are offered.
 */
class AppManagerUtilTest {

    @Test
    fun declaresInternetWithoutPermissionsReturnsFalse() {
        assertFalse(AppManagerUtil.declaresInternet(null))
        assertFalse(AppManagerUtil.declaresInternet(emptyArray()))
    }

    @Test
    fun declaresInternetWithInternetPermissionReturnsTrue() {
        assertTrue(AppManagerUtil.declaresInternet(arrayOf("android.permission.INTERNET")))
    }

    @Test
    fun declaresInternetWithOtherPermissionsReturnsFalse() {
        assertFalse(
            AppManagerUtil.declaresInternet(
                arrayOf(
                    "android.permission.CAMERA",
                    "android.permission.ACCESS_NETWORK_STATE",
                )
            )
        )
    }

    @Test
    fun declaresInternetAlongsideOtherPermissionsReturnsTrue() {
        assertTrue(
            AppManagerUtil.declaresInternet(
                arrayOf(
                    "android.permission.CAMERA",
                    "android.permission.INTERNET",
                    "android.permission.WAKE_LOCK",
                )
            )
        )
    }

    @Test
    fun declaresInternetIgnoresSimilarlyNamedPermission() {
        assertFalse(AppManagerUtil.declaresInternet(arrayOf("android.permission.INTERNET_EXTRA")))
    }
}
