package com.v2ray.ang.handler

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VlessImportFilterTest {

    private fun profile(type: EConfigType) = ProfileItem.create(type)

    @Test
    fun vlessProfilesAreImported() {
        val (accepted, rejected) = partitionBySupportedTypes(
            listOf(profile(EConfigType.VLESS)),
            setOf(EConfigType.VLESS),
        )

        assertEquals(1, accepted.size)
        assertEquals(0, rejected)
    }

    @Test
    fun otherProtocolsAreRejectedAndCounted() {
        val configs = listOf(
            profile(EConfigType.VLESS),
            profile(EConfigType.VMESS),
            profile(EConfigType.TROJAN),
            profile(EConfigType.WIREGUARD),
            profile(EConfigType.SHADOWSOCKS),
            profile(EConfigType.HYSTERIA2),
        )

        val (accepted, rejected) = partitionBySupportedTypes(configs, setOf(EConfigType.VLESS))

        assertEquals(listOf(EConfigType.VLESS), accepted.map { it.configType })
        assertEquals(5, rejected)
    }

    @Test
    fun onlyUnsupportedProtocolsYieldsNothingImported() {
        val (accepted, rejected) = partitionBySupportedTypes(
            listOf(profile(EConfigType.VMESS)),
            setOf(EConfigType.VLESS),
        )

        assertEquals(emptyList<ProfileItem>(), accepted)
        assertEquals(1, rejected)
    }

    @Test
    fun emptyInputImportsNothing() {
        val (accepted, rejected) = partitionBySupportedTypes(emptyList(), setOf(EConfigType.VLESS))

        assertEquals(emptyList<ProfileItem>(), accepted)
        assertEquals(0, rejected)
    }

    @Test
    fun noRestrictionAcceptsEveryProtocol() {
        // Subscription updates pass null and keep importing every protocol the panel serves.
        val configs = listOf(profile(EConfigType.VLESS), profile(EConfigType.VMESS))

        val (accepted, rejected) = partitionBySupportedTypes(configs, null)

        assertEquals(2, accepted.size)
        assertEquals(0, rejected)
    }
}
