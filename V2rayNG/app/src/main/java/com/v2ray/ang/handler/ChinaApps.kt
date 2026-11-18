package com.v2ray.ang.handler

/**
 * Which apps count as China-based, for `China Mode`.
 *
 * The catalogue is code, not a table: adding a family is a reviewed commit, so the set cannot
 * drift between devices and a new entry shows up in review. Matching is on package name only —
 * installer source and certificates are deliberately not consulted, because a Chinese app
 * installed from another store (or a Play copy of WeChat) is still a Chinese app, and certificate
 * pins would need a hash list that rots every time an app is republished.
 *
 * Only user-facing apps matter here: [com.v2ray.ang.util.AppManagerUtil] intersects this with the
 * packages that hold INTERNET and expose a launcher entry, so vendor system services
 * (`com.xiaomi.xmsf`, `joyose`, keyboards, providers) are never bypassed through this list alone.
 */
internal object ChinaApps {

    /**
     * Package families. An entry matches a package that equals it or continues it after a dot, so
     * `com.tencent` matches both `com.tencent` and `com.tencent.mm`.
     */
    private val families = listOf(
        // Tencent: WeChat, QQ, news, browsers, video, music, app store, game store
        "com.tencent",
        "com.qq",
        // Alibaba: Taobao, Tmall, AliExpress, Alipay, cloud, DingTalk
        "com.taobao",
        "com.tmall",
        "com.alibaba",
        "com.aliexpress",
        "com.alipay",
        "com.aliyun",
        "com.alicloud",
        "com.ant",
        "com.eg.android",
        // ByteDance: Douyin/TikTok, Toutiao, Xigua, Feishu
        "com.ss.android",
        "com.zhiliaoapp",
        // Meituan / Dianping / takeaway / groceries
        "com.sankuai",
        "com.meituan",
        "com.dianping",
        "com.pupumall",
        // Baidu
        "com.baidu",
        // Shopping
        "com.jd",
        "com.jingdong",
        "com.pinduoduo",
        "com.achievo",
        // Kuaishou
        "com.smile.gifmaker",
        "com.kuaishou",
        // Video / audio / reading / podcast
        "com.qiyi",
        "com.youku",
        "com.bilibili",
        "tv.danmaku",
        "com.lemon.lv",
        "com.ximalaya",
        "com.dragon.read",
        "com.phoenix",
        "com.qidian",
        "app.podcast.cosmos",
        // Recruiting / local services / coffee / social commerce
        "com.hpbr",
        "com.lucky.luckyclient",
        "com.xingin",
        // News / social / tools
        "com.sina",
        "com.sohu",
        "com.ifeng",
        "com.zhihu",
        "com.netease",
        "com.xunlei",
        "com.android36kr",
        // Travel / maps / ride-hailing / local services / delivery
        "com.ctrip",
        "com.trip",
        "com.qunar",
        "com.elong",
        "com.autonavi",
        "com.didichuxing",
        "com.sdu.didi",
        "com.xiaojukeji",
        "com.eleme",
        "me.ele",
        "com.wuba",
        "com.ganji",
        "com.lianjia",
        // A Chinese app store or vendor shop installs Chinese apps
        "com.xiaomi.market",
        "com.xiaomi.shop",
        "com.xiaomi.gamecenter",
        "com.xiaomi.scanner",
        "com.xiaomi.youpin",
        "com.xiaomi.vipaccount",
        "com.huawei.appmarket",
        "com.huawei.hwid",
        "com.oppo.market",
        "com.heytap",
        "com.bbk.appstore",
        "com.vivo.appstore",
        "com.coolapk.market",
        // Banking, payments, state services — the apps most likely to refuse a proxy
        "com.icbc",
        "com.ccb",
        "com.abchina",
        "com.bankcomm",
        "com.chinamworld",
        "com.cmb",
        "com.cmbchina",
        "cmb.pb",
        "com.spdb",
        "com.cgbchina",
        "com.psbc",
        "com.boc",
        "com.citic",
        "com.cebbank",
        "com.unionpay",
        "com.gov",
        "com.chsi",
    )

    /**
     * Whether [packageName] belongs to a China-based app family.
     *
     * @param packageName Package name as reported by the package manager.
     */
    fun isChinaApp(packageName: String): Boolean {
        if (packageName.isEmpty()) return false
        return families.any { family ->
            packageName == family || packageName.startsWith("$family.")
        }
    }
}
