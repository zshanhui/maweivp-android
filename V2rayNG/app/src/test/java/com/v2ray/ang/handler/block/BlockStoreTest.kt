package com.v2ray.ang.handler.block

import com.v2ray.ang.dto.entities.BlockList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BlockStoreTest {

    /** In-memory stand-in for MMKV, so the store logic runs without the native library. */
    private class FakeStore : KeyValueStore {
        val values = mutableMapOf<String, Any>()
        override fun getString(key: String): String? = values[key] as? String
        override fun putString(key: String, value: String): Boolean {
            values[key] = value
            return true
        }

        override fun getBoolean(key: String, default: Boolean): Boolean = values[key] as? Boolean ?: default
        override fun putBoolean(key: String, value: Boolean): Boolean {
            values[key] = value
            return true
        }
    }

    @Test
    fun emptyStoreLoadsAsDisabledAndEmpty() {
        val list = BlockStore(FakeStore()).load()

        assertFalse(list.enabled)
        assertEquals(emptyList<String>(), list.sites)
    }

    @Test
    fun addingSitesKeepsOrderAndCountsThem() {
        val store = BlockStore(FakeStore())
        val result = store.addSites("reddit.com\nfacebook.com\nreddit.com")

        assertEquals(listOf("reddit.com", "facebook.com"), result.list.sites)
        assertEquals(2, result.added)
        assertEquals(0, result.rejected)
    }

    @Test
    fun alreadyBlockedSitesAreNotCountedAsAdded() {
        val store = BlockStore(FakeStore())
        store.addSites("reddit.com")
        val result = store.addSites("reddit.com")

        assertEquals(0, result.added)
        assertEquals(0, result.rejected)
        assertEquals(listOf("reddit.com"), result.list.sites)
    }

    @Test
    fun invalidEntriesAreReportedWithoutTouchingTheList() {
        val store = BlockStore(FakeStore())
        val result = store.addSites("1.2.3.4")

        assertEquals(0, result.added)
        assertEquals(1, result.rejected)
        assertEquals(emptyList<String>(), result.list.sites)
    }

    @Test
    fun removingASiteKeepsTheRest() {
        val store = BlockStore(FakeStore())
        store.addSites("reddit.com\nfacebook.com")

        val list = store.removeSite("reddit.com")

        assertEquals(listOf("facebook.com"), list.sites)
    }

    @Test
    fun removingAnUnknownSiteChangesNothing() {
        val store = BlockStore(FakeStore())
        store.addSites("reddit.com")

        val list = store.removeSite("example.com")

        assertEquals(listOf("reddit.com"), list.sites)
    }

    @Test
    fun toggleIsPersisted() {
        val store = BlockStore(FakeStore())
        store.addSites("reddit.com")

        assertTrue(store.saveEnabled(true).enabled)
        assertTrue(store.load().enabled)
        assertFalse(store.saveEnabled(false).enabled)
        assertFalse(store.load().enabled)
    }

    @Test
    fun aSecondStoreOverTheSameStorageSeesTheSameList() {
        val storage = FakeStore()
        BlockStore(storage).addSites("reddit.com")

        val reopened: BlockList = BlockStore(storage).load()

        assertEquals(listOf("reddit.com"), reopened.sites)
        assertEquals("1", storage.getString("schema"))
    }
}
