package com.v2ray.ang.ui.main

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import com.v2ray.ang.enums.EConfigType

class MainImportMenuTest {

    @Test
    fun regularShareMenuContainsOnlyShareActions() {
        val expected = listOf(
            ServerMenuAction.ShareQRCode,
            ServerMenuAction.ShareClipboard,
            ServerMenuAction.ShareFullContent,
        )
        assertEquals(expected, serverMenuActions(isComplexProfile = false, includeManagementActions = false))
    }

    @Test
    fun regularMoreMenuContainsEveryActionInDisplayOrder() {
        assertEquals(
            ServerMenuAction.entries,
            serverMenuActions(isComplexProfile = false, includeManagementActions = true),
        )
    }

    @Test
    fun complexShareMenuContainsOnlyFullContent() {
        assertEquals(
            listOf(ServerMenuAction.ShareFullContent),
            serverMenuActions(isComplexProfile = true, includeManagementActions = false),
        )
    }

    @Test
    fun complexMoreMenuRetainsManagementActions() {
        val expected = listOf(
            ServerMenuAction.ShareFullContent,
            ServerMenuAction.Edit,
            ServerMenuAction.Delete,
        )
        assertEquals(expected, serverMenuActions(isComplexProfile = true, includeManagementActions = true))
    }

    @Test
    fun addMenuShowsOnlyQrCode() {
        assertEquals(listOf(ImportMenuAction.QRCode), importMenuActions())
    }

    @Test
    fun hiddenAddMenuEntriesKeepTheirActions() {
        val hidden = ImportMenuAction.entries - importMenuActions().toSet()

        assertEquals(12, hidden.size)
        assertEquals(
            listOf(
                MainAction.ImportClipboard,
                MainAction.ImportConfigLocal,
                MainAction.ImportManually(EConfigType.POLICYGROUP.value),
                MainAction.ImportManually(EConfigType.PROXYCHAIN.value),
                MainAction.ImportManually(EConfigType.VMESS.value),
                MainAction.ImportManually(EConfigType.VLESS.value),
                MainAction.ImportManually(EConfigType.SHADOWSOCKS.value),
                MainAction.ImportManually(EConfigType.SOCKS.value),
                MainAction.ImportManually(EConfigType.HTTP.value),
                MainAction.ImportManually(EConfigType.TROJAN.value),
                MainAction.ImportManually(EConfigType.WIREGUARD.value),
                MainAction.ImportManually(EConfigType.HYSTERIA2.value),
            ),
            hidden.map { it.action },
        )
    }
}
