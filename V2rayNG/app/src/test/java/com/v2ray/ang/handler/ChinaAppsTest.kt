package com.v2ray.ang.handler

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Covers the China Mode catalogue: families match with a package boundary, and unrelated
 * packages never match.
 */
class ChinaAppsTest {

    @Test
    fun tenantFamiliesMatch() {
        assertTrue(ChinaApps.isChinaApp("com.tencent.mm"))
        assertTrue(ChinaApps.isChinaApp("com.tencent.mobileqq"))
        assertTrue(ChinaApps.isChinaApp("com.taobao.taobao"))
        assertTrue(ChinaApps.isChinaApp("com.eg.android.AlipayGphone"))
    }

    @Test
    fun exactFamilyNameMatches() {
        assertTrue(ChinaApps.isChinaApp("com.icbc"))
        assertTrue(ChinaApps.isChinaApp("com.unionpay"))
        assertTrue(ChinaApps.isChinaApp("com.smile.gifmaker"))
    }

    @Test
    fun otherChinaFamiliesMatch() {
        assertTrue(ChinaApps.isChinaApp("com.ss.android.ugc.aweme"))
        assertTrue(ChinaApps.isChinaApp("com.sankuai.meituan"))
        assertTrue(ChinaApps.isChinaApp("com.chinamworld.bocmbci"))
        assertTrue(ChinaApps.isChinaApp("com.autonavi.minimap"))
    }

    @Test
    fun packagesWithForeignLookingIdsMatch() {
        // Bilibili, Ele.me, China Merchants Bank and Xiaohongshu do not follow the com.<vendor>
        // pattern, and were the ones the first catalogue missed.
        assertTrue(ChinaApps.isChinaApp("tv.danmaku.bili"))
        assertTrue(ChinaApps.isChinaApp("me.ele"))
        assertTrue(ChinaApps.isChinaApp("me.ele.crowdsource"))
        assertTrue(ChinaApps.isChinaApp("cmb.pb"))
        assertTrue(ChinaApps.isChinaApp("com.xingin.xhs"))
    }

    @Test
    fun singleAppEntriesMatch() {
        assertTrue(ChinaApps.isChinaApp("app.podcast.cosmos"))
        assertTrue(ChinaApps.isChinaApp("com.hpbr.bosszhipin"))
        assertTrue(ChinaApps.isChinaApp("com.lucky.luckyclient"))
        assertTrue(ChinaApps.isChinaApp("com.pupumall.customer"))
        assertTrue(ChinaApps.isChinaApp("com.phoenix.read"))
    }

    @Test
    fun unrelatedPackagesDoNotMatch() {
        assertFalse(ChinaApps.isChinaApp("com.google.android.gms"))
        assertFalse(ChinaApps.isChinaApp("com.android.vending"))
        assertFalse(ChinaApps.isChinaApp("com.brave.browser"))
        assertFalse(ChinaApps.isChinaApp("org.mozilla.firefox"))
    }

    @Test
    fun emptyAndSimilarNamesDoNotMatch() {
        assertFalse(ChinaApps.isChinaApp(""))
        // A shared prefix without the package boundary is a different developer.
        assertFalse(ChinaApps.isChinaApp("com.tencentx.mm"))
        assertFalse(ChinaApps.isChinaApp("com.baidupan.client"))
    }
}
