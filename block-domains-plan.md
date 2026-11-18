# Block domains

Status: implemented and verified on device (CPH2459, Android 12). Owner: TBD.

## Scope

One list of sites and one switch. While the switch is on, every entry in the list is blocked outright,
all the time, for every app. Everything stays on the device: no account, no server, no time budget.

## Why it is this small

An earlier draft tracked per-domain browsing time with daily limits and a 06:00 reset. Measuring
attention from packets turned out to be an inference with too many holes — HTTP/2 hides a whole video
behind one connection, `youtube.com` is not where the video bytes come from, and every parameter
(idle window, poll rate, byte attribution) was a guess. Blocking needs none of that: it is a routing
decision, and the routing engine already has a `block` outbound.

## How it works

Three facts about this app make the feature almost free:

1. `block` is a builtin outbound tag (`AppConfig.TAG_BLOCKED`), materialised from
   `assets/v2ray_config.json` as `{"protocol": "blackhole"}`.
2. The configuration builder already turns any `block` rule's domains into a DNS sinkhole:
   `CoreConfigManager.kt:943` and `:806` map them into `dns.hosts` → `127.0.0.1`
   (`AppConfig.LOOPBACK`), so a name resolves to loopback and apps fail fast instead of hanging.
3. `CoreConfigManager.configureRouting()` emits rules in list order, so prepending wins against
   `geosite:google → proxy` and `geosite:cn → direct` from the China-whitelist preset.

So blocking is one generated, non-persistent rule:

```kotlin
RulesetItem(
    id = "blocked-sites", remarks = "blocked-sites",
    domain = list.sites.map(BlockEntry::toMatcher),   // wildcards become regexp:, the rest pass through
    outboundTag = AppConfig.TAG_BLOCKED,
    enabled = true, locked = true,   // locked: never editable from the routing screen
)
```

It is injected at two read sites, both through `BlockRuleProvider.effectiveRulesets()`:

| Site | Why |
|---|---|
| `core/CoreConfigManager.kt` `configureRouting()` | the routing rule itself, prepended ahead of the user's rules |
| `core/CoreConfigContextBuilder.kt` `collectRoutingDomainRulesForDns()` | feeds `dns.hosts`, which produces the DNS sinkhole |

`CoreConfigContextBuilder.resolveRoutingOutbounds()` deliberately keeps reading the persisted rules
only: the generated rule targets a builtin tag, so it contributes no outbound.

The rule is never written to MMKV, so it cannot be edited, exported, or duplicated, and removing the
last site makes it disappear at the next configuration build.

## Storage

One MMKV instance, `BLOCK`, opened like the others in `MmkvManager` with `MULTI_PROCESS_MODE` — the
list is written from the UI process and read by the `:daemon` process that builds the config.

| Key | Value |
|---|---|
| `schema` | `1`; layout version, rewritten on read for future repairs |
| `enabled` | Boolean; the switch |
| `sites` | JSON array of entries, in the order the user added them |

```kotlin
data class BlockList(val enabled: Boolean = false, val sites: List<String> = emptyList())
```

A fresh install is `enabled = false` with an empty list, so nothing changes until the user acts.

`BlockStore` is a class over a `KeyValueStore` interface (`handler/block/KeyValueStore.kt`) with two
implementations: `MmkvKeyValueStore` for the app and an in-memory map for JVM tests, because MMKV
needs the native library and the repository has no instrumented-test harness.

## Entries

`BlockEntry.normalize()` accepts three shapes and rejects everything else:

- a bare domain — `reddit.com`, `www.reddit.com`, `*.reddit.com` (the `*.` is dropped; a bare domain
  already covers every subdomain, which is xray's `domain:` semantics);
- a wildcard — `example.*`, where `*` must be a **whole label**, so `ads*.example.com` stays invalid;
- a `geosite:` category — `geosite:category-ads-all`.

Normalisation strips the scheme, path, query, fragment, port and a trailing dot, lowercases, and runs
`IDN.toASCII` per label so an internationalised name becomes its punycode form. Rejected: IP literals
(`1.2.3.4`, `[::1]`), single labels (`localhost`), empty labels, and malformed `geosite:` categories.

### Wildcards

xray's domain matcher has no `*` syntax — it has prefixes (`domain:`, `full:`, `regexp:`, `geosite:`,
`ext:`). So the list stores the entry exactly as typed (`example.*`, which is what the UI shows and
exports) and `BlockRuleProvider` compiles it to an anchored regex when the configuration is built:

```
example.*   →   regexp:^(?:[^.]+\.)*example\.[^.]+$
```

That matches `example.com`, `example.org` **and** `www.example.com` (the leading group is the
subdomain allowance, consistent with how a bare domain behaves), but not `notexample.com` (labels
match whole) and not `example.co.uk` (a wildcard is one label, so a two-label TLD needs `example.*.*`
or a plain `example.co.uk` entry).

Labels are restricted to `[a-z0-9_-]` or `*`, so no regex metacharacter can survive normalisation and
the generated expression is always valid for xray's RE2 engine. The user never types a regex: an
invalid one would make xray reject the whole config and the tunnel would fail to start, with nothing in
the UI able to explain it.

`BlockEntry.parseAll()` parses pasted text: one entry per line, commas/spaces/semicolons also
accepted, duplicates dropped with order preserved, and `hosts`-file lines tolerated by dropping the
leading address (`0.0.0.0 reddit.com` → `reddit.com`). A line that is *only* an address counts as
rejected rather than vanishing silently.

## Applying a change

The config is static, so a change lands when the tunnel restarts:

| Event | What happens |
|---|---|
| Toggle, add, or remove while connected | store write, then `SettingsChangeManager.makeRestartService()` — the same flag the routing editor sets; one restart, brief drop |
| Same edit while disconnected | applied at the next start |
| Switch turned off | the rule disappears at the next build; sites become reachable |

The screen states this next to the switch instead of pretending the change is instant.

## UI

Compose, Material 3, port theme. Drawer entry **Blocked sites**, directly under *Per-app settings*
(`MainDestination.BlockedSites`, icon `ic_block_24dp`).

- **Top bar**: title, back, and a `+` action that opens an `InputDialog` with one multi-line field.
- **Switch row**: `SettingsSwitchItem` — "Block these sites", summary "Sites blocked: N. Applies to
  every app; changes take effect when the connection restarts."
- **Turning it off confirms first**: "Turn off blocking?" / "The N sites in the list will be reachable
  again." / *Cancel* · *Turn off*. Turning it **on** is immediate — blocking more is never the
  destructive direction — and an empty list skips the dialog because there is nothing to unblock.
- **List**: one row per site with a delete button whose content description is "Remove <site>".
- **Removal confirms first**: a `ConfirmDialog` titled "Remove site", message "<site> will no longer be
  blocked.", actions *Cancel* / *Remove*. Deleting an entry silently restores access to that site, so a
  mis-tap on the row's trash icon would otherwise undo a block without feedback.
- **Empty state**: "No sites blocked yet".

Adds report what happened rather than a generic failure: "%d sites added", "%d entries were not a
valid domain", or "Those sites are already blocked" when every input was a duplicate.

## Verified

Unit tests (`app/src/test/java/com/v2ray/ang/handler/block/`) cover entry normalisation, wildcard
acceptance and whole-label rejection, hosts-file tolerance, rejection counting, de-duplication, store
round-trip, the schema key, toggle persistence, and rule generation (disabled/empty list produce no
rule; an enabled list produces one locked `block` rule with a stable id; a wildcard is compiled to
`regexp:` while plain entries pass through). The full JVM suite is green (168 tests).

On device, both confirmations were checked on all their branches. Removal: *Cancel* leaves the entry
and the count unchanged, *Remove* deletes it (`Sites blocked: 6` → `5`) and the row disappears.
Disabling: the dialog names the count, *Cancel* leaves the switch on, *Turn off* flips it to off
(`checked=false`), and switching back on happens immediately with no dialog.

On the CPH2459 with the Finland node connected (`mawei-reality-fin-3`), one entry `reddit.com`:

| Blocking | `curl https://www.reddit.com` from `adb shell` | Control: `curl https://www.bing.com` |
|---|---|---|
| on | `SSL_ERROR_SYSCALL`, no response (`http=000`, remote `151.101.65.140`) | `http=200` |
| off | `http=403` — a real response from reddit's edge | `http=200` |

That is the rule doing the blocking: the name resolves, the connection is dropped, and the rest of
the tunnel is unaffected. The list also survives an app restart (it is in `files/mmkv/BLOCK`, next to
`BLOCK.crc`).

Wildcards were checked the same way, with one `example.*` entry in the list:

| URL | Result | Why |
|---|---|---|
| `https://example.com` | blocked (`http=000`) | the wildcard's own TLD |
| `https://example.org` | blocked | a different TLD, same name |
| `https://www.example.com` | blocked | subdomain allowance |
| `https://notexample.com` | `http=204` — allowed | labels match whole, so a prefix is not enough |
| `https://www.bing.com` | `http=200` — allowed | control |

## Known limits

| Situation | Behaviour |
|---|---|
| ECH / Encrypted ClientHello | no name in the handshake, so a name-based rule cannot match. Verified on device: `missav.ws` publishes `ech=…` in its HTTPS RR (Cloudflare, public name `cloudflare-ech.com`), is blocked for `curl` (which does not implement ECH) and **not** blocked in a browser that does. `apple.com` and `facebook.com` publish no ECH and stay blocked. No mitigation at this layer: the alternatives are blocking Cloudflare's shared anycast addresses or the shared public name, both of which take out unrelated sites |
| QUIC | blocked: the template sniffs `quic`, so the Initial's ClientHello yields the name |
| IP-literal clients | not blocked; entries are names by design |
| DoH in the browser | DNS sinkhole bypassed, but the routing rule still matches the sniffed name |
| The user's own `direct`/custom rule for the same domain | block wins, because the generated rule is prepended |
| A service spread over many domains (`youtube.com` vs `googlevideo.com`) | partial block until every domain is listed |
| A wildcard entry | blocked by the routing rule only: the DNS sinkhole is built from literal domains, so a `regexp:` entry lands in `dns.hosts` as a never-matching key. The site still fails, just after DNS rather than instantly |
| Many wildcards | each is a linear regex instead of a trie lookup; fine for a handful, worth a warning well before a thousand |
| App excluded in per-app proxy, or proxy-only mode with a manually configured proxy | not blocked |
| Tunnel switched off | nothing is blocked; the switch is a routing decision, not a system filter |
| App data cleared | the list is gone; nothing exists on a server by design |

## Privacy

- The list lives in the app's private `BLOCK` store; no network code touches it.
- Nothing is logged about entries: no `log.access`, no counters.
- "Backup & Restore" is `MMKV.backupAllToDirectory` (`ui/backup/BackupViewModel.kt:174`), which copies
  every store into the exported zip, so the block list travels with the profiles.
- `android:allowBackup="true"` also sweeps it into Google device backup; exclude via
  `android:dataExtractionRules` (API 31+) if that is unwanted.

## Change list

| Path | What |
|---|---|
| `dto/entities/BlockList.kt` | model |
| `handler/block/BlockEntry.kt` | normalise, parse, reject |
| `handler/block/KeyValueStore.kt` | storage seam + MMKV implementation |
| `handler/block/BlockStore.kt` | load/save/toggle/add/remove over the `BLOCK` instance |
| `handler/block/BlockRuleProvider.kt` | generated, locked `block` rule, prepended |
| `core/CoreConfigManager.kt` | `configureRouting()` uses the provider |
| `core/CoreConfigContextBuilder.kt` | DNS rule collection uses the provider |
| `ui/block/BlockSitesActivity.kt`, `BlockSitesViewModel.kt` | screen and state |
| `ui/main/MainDrawer.kt`, `MainActivity.kt` | drawer entry and navigation |
| `AndroidManifest.xml` | activity registration |
| `res/drawable/ic_block_24dp.xml` | drawer icon |
| `res/values*/strings.xml` | 11 keys in all 10 locales (zh-rCN/zh-rTW translated, others English) |
| `app/src/test/java/com/v2ray/ang/handler/block/*` | three test classes |

## Follow-ups worth considering

1. Preset chips for `geosite:category-ads-all` and a short "Social"/"Video" bundle, so users do not
   have to know the domains.
2. An "import from clipboard" action on the top bar, reusing `Utils.getClipboard`.
3. A per-entry warning when an entry equals the selected profile's `host`/`sni` or server address —
   blocking the REALITY camouflage hostname breaks the tunnel's disguise.
4. Should the switch default to on once at least one site is added?
5. Do we want several lists (Ads / Social / Video) after all, now that the plumbing is one provider?
