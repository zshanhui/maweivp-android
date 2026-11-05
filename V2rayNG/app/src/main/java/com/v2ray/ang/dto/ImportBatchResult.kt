package com.v2ray.ang.dto

/**
 * Outcome of importing text the user supplied (QR scan, clipboard, shared link, deep link).
 *
 * @property configCount profiles stored
 * @property subscriptionCount subscriptions stored
 * @property unsupportedCount profiles that parsed but were dropped because this build imports VLESS only
 */
data class ImportBatchResult(
    val configCount: Int = 0,
    val subscriptionCount: Int = 0,
    val unsupportedCount: Int = 0,
) {
    val importedCount: Int get() = configCount + subscriptionCount
}
