package com.v2ray.ang.handler.block

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.BlockList
import com.v2ray.ang.dto.entities.RulesetItem
import com.v2ray.ang.handler.MmkvManager

/**
 * Feeds the blocked-site list into the configuration the core is built from.
 *
 * The generated rule is never persisted: it cannot be edited, exported or duplicated from the
 * routing screen, and removing the last site makes it disappear at the next configuration build.
 */
object BlockRuleProvider {

    /** Stable id and remarks, so the rule is recognisable in logs and does not change between builds. */
    const val RULE_ID = "blocked-sites"

    /**
     * The user's routing rules with the generated block rule prepended.
     *
     * Prepending is what makes blocking work: a blocked site has to beat the `geosite:google → proxy`
     * and `geosite:cn → direct` rules of the China-whitelist preset.
     */
    fun effectiveRulesets(list: BlockList = BlockStore.instance.load()): MutableList<RulesetItem> {
        val userRules = MmkvManager.decodeRoutingRulesets() ?: mutableListOf()
        val generated = generatedRuleset(list) ?: return userRules
        return (mutableListOf(generated) + userRules).toMutableList()
    }

    /** The rule for [list], or null when blocking is off or there is nothing to block. */
    internal fun generatedRuleset(list: BlockList): RulesetItem? {
        if (!list.enabled || list.sites.isEmpty()) return null
        return RulesetItem(
            id = RULE_ID,
            remarks = RULE_ID,
            // Wildcards have no routing shorthand, so they are compiled to anchored regexps here.
            domain = list.sites.map(BlockEntry::toMatcher),
            outboundTag = AppConfig.TAG_BLOCKED,
            enabled = true,
            locked = true,
        )
    }
}
