package com.v2ray.ang.handler.block

import com.tencent.mmkv.MMKV
import com.v2ray.ang.dto.entities.BlockList
import com.v2ray.ang.util.JsonUtil

/**
 * Persists the blocked-site list in its own MMKV instance.
 *
 * The instance runs in `MULTI_PROCESS_MODE` because the list is written from the UI process and read
 * by the `:daemon` process, which builds the core configuration. The whole state is two keys, so a
 * write is atomic per key and there is no ledger or history to keep consistent.
 */
class BlockStore internal constructor(private val storage: KeyValueStore) {

    /** Outcome of an import, so the caller can explain what happened instead of a generic toast. */
    data class AddResult(val list: BlockList, val added: Int, val rejected: Int)

    fun load(): BlockList {
        ensureSchema()
        val sites = storage.getString(KEY_SITES)
            ?.let { JsonUtil.fromJsonSafe(it, Array<String>::class.java)?.toList() }
            .orEmpty()
        return BlockList(
            enabled = storage.getBoolean(KEY_ENABLED, false),
            sites = sites,
        )
    }

    fun saveEnabled(enabled: Boolean): BlockList {
        storage.putBoolean(KEY_ENABLED, enabled)
        return load()
    }

    /** Adds every valid entry of [text]; already-present entries are ignored, not counted as errors. */
    fun addSites(text: String): AddResult {
        val current = load()
        val parsed = BlockEntry.parseAll(text)
        val existing = current.sites.toSet()
        val added = parsed.entries.filterNot { it in existing }
        val sites = if (added.isEmpty()) current.sites else current.sites + added
        if (added.isNotEmpty()) {
            storage.putString(KEY_SITES, JsonUtil.toJson(sites))
        }
        return AddResult(current.copy(sites = sites), added.size, parsed.rejected)
    }

    fun removeSite(site: String): BlockList {
        val current = load()
        val sites = current.sites - site
        if (sites.size == current.sites.size) return current
        storage.putString(KEY_SITES, JsonUtil.toJson(sites))
        return current.copy(sites = sites)
    }

    /** Writes the layout version once; future versions repair on read through this same call. */
    private fun ensureSchema() {
        if (storage.getString(KEY_SCHEMA) != SCHEMA_VERSION.toString()) {
            storage.putString(KEY_SCHEMA, SCHEMA_VERSION.toString())
        }
    }

    companion object {
        private const val ID_BLOCK = "BLOCK"
        private const val KEY_SCHEMA = "schema"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SITES = "sites"
        private const val SCHEMA_VERSION = 1

        /** The app-wide instance: the same store in every process. */
        val instance: BlockStore by lazy {
            BlockStore(MmkvKeyValueStore(MMKV.mmkvWithID(ID_BLOCK, MMKV.MULTI_PROCESS_MODE)))
        }
    }
}
