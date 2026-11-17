package com.v2ray.ang.handler.block

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BlockEntryTest {

    @Test
    fun plainDomainIsLowercased() {
        assertEquals("reddit.com", BlockEntry.normalize("Reddit.COM"))
    }

    @Test
    fun schemePathAndPortAreStripped() {
        assertEquals("www.reddit.com", BlockEntry.normalize("https://www.reddit.com/r/all?sort=new"))
        assertEquals("reddit.com", BlockEntry.normalize("reddit.com:443"))
        assertEquals("reddit.com", BlockEntry.normalize("http://reddit.com."))
    }

    @Test
    fun wildcardPrefixIsRedundantAndDropped() {
        assertEquals("reddit.com", BlockEntry.normalize("*.reddit.com"))
        assertEquals("reddit.com", BlockEntry.normalize("https://*.reddit.com"))
    }

    @Test
    fun geositeCategoryPassesThrough() {
        assertEquals("geosite:category-ads-all", BlockEntry.normalize("geosite:category-ads-all"))
        assertEquals("geosite:cn", BlockEntry.normalize("GEOSITE:CN"))
    }

    @Test
    fun invalidGeositeCategoryIsRejected() {
        assertNull(BlockEntry.normalize("geosite:"))
        assertNull(BlockEntry.normalize("geosite:Not A Category"))
        assertNull(BlockEntry.normalize("geosite:../../etc"))
    }

    @Test
    fun ipLiteralsAndBareLabelsAreRejected() {
        assertNull(BlockEntry.normalize("1.2.3.4"))
        assertNull(BlockEntry.normalize("localhost"))
        assertNull(BlockEntry.normalize("http://[::1]/"))
        assertNull(BlockEntry.normalize(""))
        assertNull(BlockEntry.normalize("   "))
        assertNull(BlockEntry.normalize("a..b"))
    }

    @Test
    fun wildcardIsAccepted() {
        assertEquals("example.*", BlockEntry.normalize("Example.*"))
        assertEquals("example.*", BlockEntry.normalize("https://example.*/x"))
        assertEquals("ad.*.example.com", BlockEntry.normalize("ad.*.example.com"))
    }

    @Test
    fun wildcardMustBeAWholeLabel() {
        assertNull(BlockEntry.normalize("*"))
        assertNull(BlockEntry.normalize("*."))
        assertNull(BlockEntry.normalize("ads*.example.com"))
        assertNull(BlockEntry.normalize("exa*mple.com"))
    }

    @Test
    fun plainEntriesNeedNoRegex() {
        assertEquals("reddit.com", BlockEntry.toMatcher("reddit.com"))
        assertEquals("geosite:category-ads-all", BlockEntry.toMatcher("geosite:category-ads-all"))
    }

    @Test
    fun wildcardIsCompiledToAnAnchoredRegex() {
        assertEquals("regexp:^(?:[^.]+\\.)*example\\.[^.]+$", BlockEntry.toMatcher("example.*"))
    }

    @Test
    fun wildcardMatchesEveryTldAndItsSubdomains() {
        val pattern = Regex(BlockEntry.toMatcher("example.*").removePrefix("regexp:"))

        assertTrue(pattern.matches("example.com"))
        assertTrue(pattern.matches("example.org"))
        assertTrue(pattern.matches("example.net"))
        assertTrue(pattern.matches("www.example.com"))
        assertTrue(pattern.matches("a.b.example.com"))
        assertFalse(pattern.matches("notexample.com"))
        assertFalse(pattern.matches("example.co.uk"))
    }

    @Test
    fun hostsFileLinesLoseTheirAddress() {
        val result = BlockEntry.parseAll("0.0.0.0 reddit.com\n127.0.0.1 facebook.com")

        assertEquals(listOf("reddit.com", "facebook.com"), result.entries)
        assertEquals(0, result.rejected)
    }

    @Test
    fun duplicatesAreDroppedAndOrderIsKept() {
        val result = BlockEntry.parseAll("a.com, b.com\na.com\nB.com")

        assertEquals(listOf("a.com", "b.com"), result.entries)
        assertEquals(0, result.rejected)
    }

    @Test
    fun unusableTokensAreCounted() {
        val result = BlockEntry.parseAll("reddit.com\nnonsense\n1.2.3.4")

        assertEquals(listOf("reddit.com"), result.entries)
        assertEquals(2, result.rejected)
    }

    @Test
    fun blankInputProducesNothing() {
        val result = BlockEntry.parseAll("\n  \n")

        assertEquals(emptyList<String>(), result.entries)
        assertEquals(0, result.rejected)
    }
}
