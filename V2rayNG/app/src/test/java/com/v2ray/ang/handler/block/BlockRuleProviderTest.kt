package com.v2ray.ang.handler.block

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.BlockList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BlockRuleProviderTest {

    @Test
    fun disabledListGeneratesNoRule() {
        assertNull(BlockRuleProvider.generatedRuleset(BlockList(enabled = false, sites = listOf("reddit.com"))))
    }

    @Test
    fun emptyListGeneratesNoRule() {
        assertNull(BlockRuleProvider.generatedRuleset(BlockList(enabled = true)))
    }

    @Test
    fun enabledListBecomesOneBlockRule() {
        val rule = BlockRuleProvider.generatedRuleset(
            BlockList(enabled = true, sites = listOf("reddit.com", "geosite:category-ads-all"))
        )

        assertEquals(listOf("reddit.com", "geosite:category-ads-all"), rule?.domain)
        assertEquals(AppConfig.TAG_BLOCKED, rule?.outboundTag)
        assertTrue(rule?.enabled == true)
    }

    @Test
    fun wildcardEntriesAreCompiledForTheRoutingEngine() {
        val rule = BlockRuleProvider.generatedRuleset(
            BlockList(enabled = true, sites = listOf("example.*", "reddit.com"))
        )

        assertEquals(
            listOf("regexp:^(?:[^.]+\\.)*example\\.[^.]+$", "reddit.com"),
            rule?.domain,
        )
    }

    @Test
    fun generatedRuleIsLockedAndKeepsItsId() {
        val list = BlockList(enabled = true, sites = listOf("reddit.com"))

        val first = BlockRuleProvider.generatedRuleset(list)
        val second = BlockRuleProvider.generatedRuleset(list)

        assertTrue(first?.locked == true)
        assertEquals(BlockRuleProvider.RULE_ID, first?.id)
        assertEquals(first?.id, second?.id)
    }
}
