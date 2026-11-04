package com.v2ray.ang.ui.main

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MainServerRemovalTest {

    @Test
    fun removingSelectedProfileClearsSelection() {
        assertNull(selectionAfterServerRemoval(removedGuid = "guid-a", selectedGuid = "guid-a"))
    }

    @Test
    fun removingAnotherProfileKeepsSelection() {
        assertEquals(
            "guid-b",
            selectionAfterServerRemoval(removedGuid = "guid-a", selectedGuid = "guid-b"),
        )
    }

    @Test
    fun removingWithNoSelectionKeepsNoSelection() {
        assertNull(selectionAfterServerRemoval(removedGuid = "guid-a", selectedGuid = null))
    }

    @Test
    fun lastProfileCanBeRemoved() {
        // The single remaining profile is also the selected one; the removal must not be refused.
        val selected = "only-guid"
        assertNull(selectionAfterServerRemoval(removedGuid = selected, selectedGuid = selected))
    }
}
