package com.v2ray.ang.handler.block

import java.net.IDN

/**
 * Turns what a user types into an entry the routing engine can match.
 *
 * An entry is either a bare domain — `reddit.com`, which also covers every subdomain — a wildcard
 * such as `example.*`, or a `geosite:` category. IP addresses are rejected on purpose: the block
 * list matches names, and one address can host unrelated sites.
 */
object BlockEntry {

    private const val GEOSITE_PREFIX = "geosite:"

    /** The one label the user may wildcard; it matches exactly one label, never several. */
    private const val WILDCARD_LABEL = "*"

    /** Anchored prefix that lets a pattern match the domain itself and every subdomain of it. */
    private const val SUBDOMAIN_PREFIX = """(?:[^.]+\.)*"""

    /** geosite categories are lower-case words joined by hyphens. */
    private val GEOSITE_CATEGORY = Regex("^[a-z0-9][a-z0-9-]*$")

    /** A DNS label we are willing to hand to the core: letters, digits, hyphen and underscore. */
    private val HOST_LABEL = Regex("^[a-z0-9_]([a-z0-9_-]*[a-z0-9_])?$")

    /** Leading address of a `hosts`-file line, which paste-imports should tolerate. */
    private val HOSTS_ADDRESS = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")

    /** Result of parsing pasted text. */
    data class ParseResult(val entries: List<String>, val rejected: Int)

    /**
     * Normalises one entry.
     *
     * @return the normalised entry, or null when [input] cannot be matched as written.
     */
    fun normalize(input: String): String? {
        var value = input.trim().lowercase()
        if (value.isEmpty()) return null

        if (value.startsWith(GEOSITE_PREFIX)) {
            val category = value.removePrefix(GEOSITE_PREFIX)
            return if (GEOSITE_CATEGORY.matches(category)) "$GEOSITE_PREFIX$category" else null
        }

        value = value.substringAfter("://", value)
        // "*." is how people write "everything under", which is already what a bare domain does.
        value = value.removePrefix("*.")
        value = value.substringBefore('/').substringBefore('?').substringBefore('#')
        value = value.substringBefore(':')
        value = value.trimEnd('.')
        if (!value.contains('.')) return null

        val rawLabels = value.split('.')
        val ascii = try {
            // IDN conversion is per label so a wildcard label is not rejected by nameprep.
            rawLabels.joinToString(".") { label ->
                if (label == WILDCARD_LABEL) label else IDN.toASCII(label)
            }
        } catch (e: IllegalArgumentException) {
            return null
        }
        val labels = ascii.split('.')
        if (labels.size < 2) return null
        // Every label is a plain label or the single wildcard label; anything else is not matchable.
        if (labels.any { it != WILDCARD_LABEL && !HOST_LABEL.matches(it) }) return null
        // An all-digit last label means an IPv4 literal, not a name.
        if (labels.last() != WILDCARD_LABEL && labels.last().all { it.isDigit() }) return null

        return ascii
    }

    /** True when [entry] contains a wildcard label and therefore needs a regex matcher. */
    fun isWildcard(entry: String): Boolean =
        !entry.startsWith(GEOSITE_PREFIX) && entry.split('.').contains(WILDCARD_LABEL)

    /**
     * The form the routing engine matches on.
     *
     * Plain domains and `geosite:` entries pass through: xray already treats a bare domain as a suffix
     * match. A wildcard has no such shorthand, so it becomes an anchored `regexp:` matching the
     * pattern and every subdomain of it — `example.*` covers `example.com`, `example.org` and
     * `www.example.com`, but not `example.co.uk` (the wildcard is one label) and not
     * `notexample.com` (labels match whole).
     *
     * Labels are restricted to `[a-z0-9_-]` or `*`, so no regex metacharacter can survive
     * normalisation and the generated expression is always valid for xray's RE2 engine.
     */
    fun toMatcher(entry: String): String {
        if (!isWildcard(entry)) return entry
        val body = entry.split('.').joinToString("\\.") { label ->
            if (label == WILDCARD_LABEL) "[^.]+" else label
        }
        return "regexp:^" + SUBDOMAIN_PREFIX + body + "$"
    }

    /**
     * Parses pasted text: one entry per line, commas and spaces also accepted, `hosts`-file lines
     * tolerated, duplicates dropped and order preserved.
     */
    fun parseAll(text: String): ParseResult {
        val entries = LinkedHashSet<String>()
        var rejected = 0

        text.split('\n', '\r').forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return@forEach

            // A line that is only an address is not a name: drop it, but tell the user.
            val line = dropHostsAddress(trimmed)
            if (line.isEmpty()) {
                rejected++
                return@forEach
            }

            line.split(',', ';', ' ', '\t').forEach { token ->
                if (token.isBlank()) return@forEach
                val normalized = normalize(token)
                if (normalized == null) rejected++ else entries.add(normalized)
            }
        }

        return ParseResult(entries.toList(), rejected)
    }

    /** Drops the `0.0.0.0` (or any address) a `hosts` file puts in front of the name. */
    private fun dropHostsAddress(line: String): String {
        val trimmed = line.trim()
        val first = trimmed.substringBefore(' ').substringBefore('\t')
        return if (HOSTS_ADDRESS.matches(first)) trimmed.substring(first.length).trim() else trimmed
    }
}
