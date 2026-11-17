package com.v2ray.ang.dto.entities

/**
 * The user's blocked-site list.
 *
 * A single list with one switch: while [enabled] is true every entry in [sites] is blocked, all the
 * time. Entries are bare domains (which cover their subdomains) or `geosite:` categories.
 */
data class BlockList(
    val enabled: Boolean = false,
    val sites: List<String> = emptyList(),
)
