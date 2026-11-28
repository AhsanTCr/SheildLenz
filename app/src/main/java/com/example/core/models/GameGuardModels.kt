package com.example.core.models

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

enum class GameLockReason {
    NONE,
    CURFEW,
    DAILY_CAP,
    SESSION_COOLDOWN
}

data class GameGuardDecision(
    val isTrackedGame: Boolean,
    val shouldBlock: Boolean,
    val lockReason: GameLockReason = GameLockReason.NONE,
    val remainingUnlockMs: Long = 0L,
    val shouldShowSoftWarning: Boolean = false,
    val remainingSessionSeconds: Int = 0,
    val dailyGameSecondsUsed: Int = 0,
    val dailyCapSeconds: Int = 0
)

object GameAppDetector {

    private val KNOWN_GAME_PACKAGE_PREFIXES = listOf(
        "com.supercell.",
        "com.tencent.ig",
        "com.pubg.",
        "com.dts.freefire",
        "com.activision.callofduty",
        "com.garena.game",
        "com.roblox.",
        "com.mojang.",
        "com.kiloo.",
        "com.sybo.",
        "com.imangi.",
        "com.king.",
        "com.innersloth.",
        "com.mihoyo.",
        "com.cognosphere.",
        "com.riotgames.",
        "com.ea.game.",
        "com.ea.gp.",
        "com.gameloft.",
        "com.miniclip.",
        "com.playrix.",
        "com.zynga.",
        "com.rovio.",
        "com.halfbrick.",
        "com.epicgames.",
        "com.voodoo.",
        "com.outfit7.",
        "com.mobile.legends",
        "com.netease.",
        "com.wb.goog.",
        "com.rockstargames.",
        "com.fingersoft.",
        "com.nekki.",
        "com.madfingergames.",
        "com.firsttouchgames.",
        "com.nintendo.",
        "com.nianticlabs.",
        "jp.konami.",
        "com.bandainamcoent."
    )

    private val KNOWN_GAME_TOKENS = listOf(
        ".game.",
        ".games.",
        "ludo",
        "chess",
        "sudoku",
        "solitaire",
        "minecraft",
        "fortnite",
        "valorant",
        "freefire",
        "brawlstars",
        "clashroyale",
        "clashofclans",
        "subwaysurf",
        "candycrush",
        "amongus",
        "genshin"
    )

    fun isKnownGamePackageHeuristic(packageName: String?): Boolean {
        val pkg = packageName?.trim()?.lowercase().orEmpty()
        if (pkg.isBlank()) return false
        if (pkg.startsWith("com.android.") || pkg.startsWith("com.google.android.gms")) return false
        if (KNOWN_GAME_PACKAGE_PREFIXES.any { pkg.startsWith(it) }) return true
        return KNOWN_GAME_TOKENS.any { pkg.contains(it) }
    }

    @Suppress("DEPRECATION")
    fun isApplicationInfoGame(info: ApplicationInfo): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && info.category == ApplicationInfo.CATEGORY_GAME) {
            return true
        }
        if ((info.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
            return true
        }
        return isKnownGamePackageHeuristic(info.packageName)
    }

    fun isAndroidGamePackage(context: Context, packageName: String?): Boolean {
        val clean = packageName?.trim().orEmpty()
        if (clean.isBlank() || clean == context.packageName) return false
        if (isKnownGamePackageHeuristic(clean)) return true
        return try {
            val info = context.packageManager.getApplicationInfo(clean, 0)
            isApplicationInfoGame(info)
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun discoverInstalledGamePackages(context: Context): Set<String> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching { pm.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
        val detected = linkedSetOf<String>()
        for (item in resolved) {
            val appInfo = item.activityInfo?.applicationInfo ?: continue
            val pkg = appInfo.packageName ?: continue
            if (pkg == context.packageName) continue
            if (isApplicationInfoGame(appInfo)) {
                detected.add(pkg)
            }
        }
        return detected
    }
}
