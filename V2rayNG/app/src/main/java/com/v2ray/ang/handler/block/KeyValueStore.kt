package com.v2ray.ang.handler.block

import com.tencent.mmkv.MMKV

/**
 * The slice of MMKV that [BlockStore] needs.
 *
 * Exists so the store can be covered by JVM unit tests: the real implementation is MMKV, which
 * needs the native library, while tests use an in-memory map.
 */
internal interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String): Boolean
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean): Boolean
}

/** MMKV-backed implementation used by the app. */
internal class MmkvKeyValueStore(private val mmkv: MMKV) : KeyValueStore {
    override fun getString(key: String): String? = mmkv.decodeString(key)
    override fun putString(key: String, value: String): Boolean = mmkv.encode(key, value)
    override fun getBoolean(key: String, default: Boolean): Boolean = mmkv.decodeBool(key, default)
    override fun putBoolean(key: String, value: Boolean): Boolean = mmkv.encode(key, value)
}
