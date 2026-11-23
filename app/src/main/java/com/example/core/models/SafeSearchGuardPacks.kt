package com.example.core.models

import java.util.Locale

enum class SafeSearchPack(
    val id: String,
    val title: String,
    val subtitle: String,
    val keywords: Set<String>,
    val domains: Set<String>
) {
    ADULT_NSFW(
        id = "adult",
        title = "Adult & NSFW Guard",
        subtitle = "Blocks explicit searches, adult portals & unsafe media sites",
        keywords = setOf(
            "nsfw", "xxx", "porn", "porno", "pornography", "hentai", "rule34",
            "nude", "nudes", "onlyfans", "fansly", "stripchat", "chaturbate",
            "camgirl", "escort", "erotic", "erotica"
        ),
        domains = setOf(
            "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com",
            "redtube.com", "youporn.com", "onlyfans.com", "fansly.com",
            "chaturbate.com", "stripchat.com", "rule34.xxx", "nhentai.net"
        )
    ),
    GAMBLING_BETTING(
        id = "gambling",
        title = "Gambling & Betting",
        subtitle = "Blocks casinos, sportsbooks, odds & wagering portals",
        keywords = setOf(
            "bet365", "1xbet", "draftkings", "fanduel", "pokerstars",
            "888casino", "roobet", "sportsbook", "online casino", "slot machine",
            "betting odds", "blackjack online"
        ),
        domains = setOf(
            "bet365.com", "1xbet.com", "draftkings.com", "fanduel.com",
            "stake.com", "roobet.com", "pokerstars.com", "betway.com"
        )
    ),
    PROXY_VPN_BYPASS(
        id = "bypass_proxy",
        title = "Proxy & Unblocker Guard",
        subtitle = "Stops web proxies, filter bypassers & anonymous mirror searches",
        keywords = setOf(
            "web proxy", "unblock websites", "school unblocker", "croxyproxy",
            "proxy site", "bypass filter", "free vpn proxy", "mirror site",
            "tor browser download"
        ),
        domains = setOf(
            "croxyproxy.com", "proxysite.com", "hidemyass.com", "kproxy.com",
            "whoer.net", "blockaway.net"
        )
    ),
    SHORTS_DISTRACTION(
        id = "shorts_distraction",
        title = "Shorts & Reels Distractions",
        subtitle = "Guards against endless short-video rabbit holes & binge portals",
        keywords = setOf(
            "youtube shorts", "tiktok viral", "instagram reels", "twitch streams",
            "watch tiktok", "short video feed"
        ),
        domains = setOf(
            "tiktok.com", "twitch.tv", "kick.com"
        )
    ),
    VIOLENCE_SELFHARM(
        id = "violence_selfharm",
        title = "Gore & Self-Harm Guard",
        subtitle = "Filters graphic violence, gore sites & harmful query phrases",
        keywords = setOf(
            "bestgore", "liveleak", "gore video", "shock site",
            "self harm", "suicide method", "cutting guide"
        ),
        domains = setOf(
            "kaotic.com", "crazyshit.com", "theync.com"
        )
    );

    companion object {
        val DEFAULT_PACK_IDS: Set<String> = setOf(ADULT_NSFW.id, GAMBLING_BETTING.id)

        fun fromId(id: String): SafeSearchPack? =
            entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }

        fun detectMatchInText(
            rawText: String?,
            activePackIds: Set<String>
        ): Pair<SafeSearchPack, String>? {
            if (rawText.isNullOrBlank() || activePackIds.isEmpty()) return null
            val normalized = rawText.lowercase(Locale.US)
                .replace("%20", " ")
                .replace("+", " ")

            for (pack in entries) {
                if (pack.id !in activePackIds) continue
                for (domain in pack.domains) {
                    val escaped = Regex.escape(domain)
                    val domainRegex = Regex("""(?:^|[^a-z0-9-])$escaped(?=$|[^a-z0-9])""")
                    if (domainRegex.containsMatchIn(normalized)) {
                        return pack to "Smart Safe Search (${pack.title}) blocked '$domain'."
                    }
                }
                for (kw in pack.keywords) {
                    val escaped = Regex.escape(kw)
                    val kwRegex = Regex("""(?:^|[^a-z0-9])$escaped(?=$|[^a-z0-9])""")
                    if (kwRegex.containsMatchIn(normalized)) {
                        return pack to "Smart Safe Search (${pack.title}) blocked search '$kw'."
                    }
                }
            }
            return null
        }

        fun matchesDomainInActivePacks(
            domain: String,
            activePackIds: Set<String>
        ): SafeSearchPack? {
            if (domain.isBlank() || activePackIds.isEmpty()) return null
            val clean = domain.trim().lowercase(Locale.US).removePrefix("www.")
            for (pack in entries) {
                if (pack.id !in activePackIds) continue
                if (pack.domains.any { blocked -> clean == blocked || clean.endsWith(".$blocked") }) {
                    return pack
                }
            }
            return null
        }

        val DEFAULT_STUDY_FRIENDLY_PACKAGES: Set<String> = setOf(
            "com.google.android.calculator",
            "com.android.calculator2",
            "com.google.android.keep",
            "com.google.android.apps.classroom",
            "com.google.android.apps.docs",
            "com.google.android.apps.docs.editors.docs",
            "com.google.android.apps.docs.editors.slides",
            "com.google.android.apps.docs.editors.sheets",
            "com.google.android.calendar",
            "com.microsoft.office.onenote",
            "org.khanacademy.android",
            "com.duolingo"
        )

        val POPULAR_BUDGET_APP_PRESETS: List<Pair<String, String>> = listOf(
            "com.google.android.youtube" to "YouTube",
            "com.zhiliaoapp.musically" to "TikTok",
            "com.instagram.android" to "Instagram",
            "com.snapchat.android" to "Snapchat",
            "com.roblox.client" to "Roblox",
            "com.discord" to "Discord",
            "com.facebook.katana" to "Facebook",
            "com.twitter.android" to "X (Twitter)",
            "com.reddit.frontpage" to "Reddit",
            "com.android.chrome" to "Chrome Browser"
        )
    }
}
