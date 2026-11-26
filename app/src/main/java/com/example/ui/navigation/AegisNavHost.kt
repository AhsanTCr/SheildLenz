package com.example.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.core.audio.AegisSoundEffects
import com.example.di.AegisContainer
import com.example.ui.about.AboutScreen
import com.example.ui.auth.GuardianGateScreen
import com.example.ui.blocking.BlockOverlayScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.focus.FocusSafetyHubScreen
import com.example.ui.history.SecurityHistoryScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.parents.ParentsScreen
import com.example.ui.protection.LiveProtectionScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.WellbeingHubScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisGoldBright
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSecondaryBackground
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.ClassicalBodyFont

private data class MainTabItem(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val MainTabItems = listOf(
    MainTabItem(
        route = AegisDestinations.HOME,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    MainTabItem(
        route = AegisDestinations.STATUS,
        labelRes = R.string.nav_status,
        selectedIcon = Icons.Filled.MonitorHeart,
        unselectedIcon = Icons.Outlined.MonitorHeart
    ),
    MainTabItem(
        route = AegisDestinations.EDITS,
        labelRes = R.string.nav_edits,
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune
    ),
    MainTabItem(
        route = AegisDestinations.PARENTS,
        labelRes = R.string.nav_parents,
        selectedIcon = Icons.Filled.SupervisorAccount,
        unselectedIcon = Icons.Outlined.SupervisorAccount
    ),
    MainTabItem(
        route = AegisDestinations.ABOUT,
        labelRes = R.string.nav_about,
        selectedIcon = Icons.Filled.Info,
        unselectedIcon = Icons.Outlined.Info
    )
)

internal fun isStartupGuardianAuthorized(
    hasGuardianPin: Boolean,
    isSessionUnlocked: Boolean
): Boolean = hasGuardianPin && isSessionUnlocked

@Composable
fun AegisNavHost(
    container: AegisContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onRequestStartProtection: () -> Unit = {},
    onRequestStopProtection: () -> Unit = {},
    onRequestGoogleSignIn: ((
        onSuccess: () -> Unit,
        onCancelled: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit)? = null,
    onRequestGoogleSignOut: ((onComplete: () -> Unit) -> Unit)? = null
) {
    val isOnboardingDone by container.preferences.isOnboardingCompleted.collectAsState()
    val pendingPairingCode by container.pendingPairingCode.collectAsState()
    val pendingBrowserUrl by container.pendingBrowserUrl.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showMainNavigation = remember(currentRoute) { MainTabItems.any { it.route == currentRoute } }

    var previousRoute by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(currentRoute) {
        if (previousRoute == AegisDestinations.EDITS && currentRoute != null && currentRoute != AegisDestinations.EDITS) {
            container.preferences.lockGuardianSession()
        }
        previousRoute = currentRoute
    }

    LaunchedEffect(pendingPairingCode) {
        if (!pendingPairingCode.isNullOrBlank()) {
            navController.navigate(AegisDestinations.PARENTS) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(pendingBrowserUrl) {
        if (!pendingBrowserUrl.isNullOrBlank()) {
            navController.navigate(AegisDestinations.SAFE_BROWSER) {
                launchSingleTop = true
            }
        }
    }

    val context = LocalContext.current

    val navigateToTab: (String) -> Unit = remember(navController, context) {
        { targetRoute ->
            val activeRoute = navController.currentBackStackEntry?.destination?.route
            if (activeRoute != targetRoute) {
                AegisSoundEffects.playTap(context)
                navController.navigate(targetRoute) {
                    popUpTo(AegisDestinations.HOME) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavigationRail = showMainNavigation && maxWidth >= 720.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                NavigationRail(
                    containerColor = AegisLavenderSoft,
                    contentColor = AegisTextPrimary,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    MainTabItems.forEach { tab ->
                        val selected = currentRoute == tab.route
                        val tabLabel = stringResource(tab.labelRes)
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tabLabel
                                )
                            },
                            label = {
                                Text(
                                    text = tabLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = ClassicalBodyFont,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = AegisPrimaryCyan,
                                selectedTextColor = AegisPrimaryCyan,
                                indicatorColor = AegisPurple.copy(alpha = 0.18f),
                                unselectedIconColor = AegisTextMuted,
                                unselectedTextColor = AegisTextMuted
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
                VerticalDivider(color = AegisCardBorder)
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showMainNavigation && !useNavigationRail) {
                        VipAnimatedBottomBar(
                            tabs = MainTabItems,
                            currentRoute = currentRoute,
                            onSelectTab = navigateToTab
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = AegisDestinations.SPLASH,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            bottom = if (showMainNavigation && !useNavigationRail) {
                                innerPadding.calculateBottomPadding()
                            } else {
                                0.dp
                            }
                        ),
                    enterTransition = { fadeIn(animationSpec = tween(75)) },
                    exitTransition = { fadeOut(animationSpec = tween(55)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(75)) },
                    popExitTransition = { fadeOut(animationSpec = tween(55)) }
                ) {
                    composable(AegisDestinations.SPLASH) {
                        SplashScreen(
                            isOnboardingCompleted = isOnboardingDone,
                            onNavigateNext = { dest ->
                                navController.navigate(dest) {
                                    popUpTo(AegisDestinations.SPLASH) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(AegisDestinations.ONBOARDING) {
                        OnboardingScreen(
                            preferences = container.preferences,
                            onComplete = {
                                container.preferences.setOnboardingCompleted(true)
                                navController.navigate(AegisDestinations.HOME) {
                                    popUpTo(AegisDestinations.ONBOARDING) { inclusive = true }
                                }
                            },
                            onNavigateToLinkDevice = {
                                container.preferences.setOnboardingCompleted(true)
                                navController.navigate(AegisDestinations.PARENTS) {
                                    popUpTo(AegisDestinations.ONBOARDING) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(AegisDestinations.HOME) {
                        DashboardScreen(
                            protectionRepo = container.protectionRepository,
                            modelManager = container.modelManager,
                            preferences = container.preferences,
                            onModelUpdated = { container.refreshModelTelemetry() },
                            onRequestStartProtection = onRequestStartProtection,
                            onRequestStopProtection = onRequestStopProtection,
                            onNavigateToStatus = {
                                navigateToTab(AegisDestinations.STATUS)
                            },
                            onNavigateToProgress = {
                                navController.navigate(AegisDestinations.PROGRESS) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToWellbeing = {
                                navController.navigate(AegisDestinations.WELLBEING) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToFocusHub = {
                                navController.navigate(AegisDestinations.FOCUS_HUB) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToSafeBrowser = {
                                navController.navigate(AegisDestinations.SAFE_BROWSER) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(AegisDestinations.EDITS) {
                        SettingsScreen(
                            preferences = container.preferences,
                            modelManager = container.modelManager,
                            protectionRepo = container.protectionRepository,
                            onModelUpdated = { container.refreshModelTelemetry() },
                            onNavigateToStatus = { navigateToTab(AegisDestinations.STATUS) },
                            onNavigateToParents = { navigateToTab(AegisDestinations.PARENTS) }
                        )
                    }

                    composable(AegisDestinations.STATUS) {
                        LiveProtectionScreen(
                            protectionRepo = container.protectionRepository,
                            preferences = container.preferences,
                            blockedDnsLogRepo = container.blockedDnsLogRepository,
                            onRequestArmShield = onRequestStartProtection,
                            onNavigateToFocusHub = {
                                navController.navigate(AegisDestinations.FOCUS_HUB) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToProgress = {
                                navController.navigate(AegisDestinations.PROGRESS) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToWellbeing = {
                                navController.navigate(AegisDestinations.WELLBEING) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToParents = {
                                navigateToTab(AegisDestinations.PARENTS)
                            },
                            onNavigateToEdits = {
                                navigateToTab(AegisDestinations.EDITS)
                            }
                        )
                    }

                    composable(AegisDestinations.PROGRESS) {
                        SecurityHistoryScreen(
                            historyRepo = container.securityHistoryRepository,
                            preferences = container.preferences,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(AegisDestinations.WELLBEING) {
                        WellbeingHubScreen(
                            preferences = container.preferences,
                            blockedDnsLogRepo = container.blockedDnsLogRepository,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(AegisDestinations.FOCUS_HUB) {
                        FocusSafetyHubScreen(
                            preferences = container.preferences,
                            onNavigateToSafeBrowser = { navController.navigate(AegisDestinations.SAFE_BROWSER) },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(AegisDestinations.SAFE_BROWSER) {
                        com.example.ui.browser.SafeBrowserScreen(
                            preferences = container.preferences,
                            protectionRepo = container.protectionRepository,
                            blockedDnsLogRepo = container.blockedDnsLogRepository,
                            overlayController = container.overlayController,
                            pendingExternalUrl = pendingBrowserUrl,
                            onConsumePendingUrl = { container.setPendingBrowserUrl(null) },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(AegisDestinations.PARENTS) {
                        ParentsScreen(
                            repository = container.parentChildRepository,
                            preferences = container.preferences,
                            syncController = container.parentChildSyncController,
                            pendingPairingCode = pendingPairingCode,
                            onConsumePendingPairingCode = { container.setPendingPairingCode(null) },
                            onRequestGoogleSignIn = onRequestGoogleSignIn,
                            onRequestGoogleSignOut = onRequestGoogleSignOut
                        )
                    }

                    composable(AegisDestinations.ABOUT) {
                        AboutScreen(
                            preferences = container.preferences
                        )
                    }

                    composable(AegisDestinations.GUARDIAN_GATE) {
                        GuardianGateScreen(
                            preferences = container.preferences,
                            protectionRepo = container.protectionRepository,
                            onUnlocked = { navController.popBackStack() }
                        )
                    }

                    composable(AegisDestinations.PARENT_CHILD_LINK) {
                        com.example.ui.auth.ParentChildLinkScreen(
                            repository = container.parentChildRepository,
                            preferences = container.preferences,
                            syncController = container.parentChildSyncController,
                            onBack = { navController.popBackStack() },
                            onRequestGoogleSignIn = onRequestGoogleSignIn,
                            onRequestGoogleSignOut = onRequestGoogleSignOut
                        )
                    }

                    composable(AegisDestinations.BLOCK_SCREEN) {
                        BlockOverlayScreen(
                            preferences = container.preferences,
                            onDismiss = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VipAnimatedBottomBar(
    tabs: List<MainTabItem>,
    currentRoute: String?,
    onSelectTab: (String) -> Unit
) {
    val activeIndex = remember(currentRoute, tabs) {
        tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    }

    val vipTransition = rememberInfiniteTransition(label = "VipBottomBarContinuous")
    val auroraPhase by vipTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VipAuroraSweep"
    )
    val glowPulse by vipTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "VipLineGlowPulse"
    )

    val barBgBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFF5FAFF),
                AegisSecondaryBackground,
                AegisLavenderSoft
            )
        )
    }
    val pillBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                AegisPrimaryCyan.copy(alpha = 0.17f),
                AegisPurple.copy(alpha = 0.17f)
            )
        )
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                ambientColor = AegisPurple.copy(alpha = 0.16f),
                spotColor = AegisPrimaryCyan.copy(alpha = 0.16f)
            )
            .background(barBgBrush)
            .drawBehind {
                // Continuous soft aurora shimmer sweeping across the whole bottom bar
                val sweepWidth = size.width * 0.65f
                val startX = (size.width + sweepWidth) * auroraPhase - sweepWidth
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            AegisPrimaryCyan.copy(alpha = 0.08f * glowPulse),
                            AegisPurple.copy(alpha = 0.09f * glowPulse),
                            AegisChampagneGold.copy(alpha = 0.07f * glowPulse),
                            Color.Transparent
                        ),
                        startX = startX,
                        endX = startX + sweepWidth
                    )
                )
            }
            .testTag("vip_animated_bottom_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Continuously flowing luminous top hairline across the bottom bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .drawBehind {
                        val travel = size.width
                        val shift = auroraPhase * travel
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    AegisElectricBlue.copy(alpha = 0.30f),
                                    AegisPrimaryCyan.copy(alpha = 0.72f * glowPulse),
                                    AegisPurple.copy(alpha = 0.65f),
                                    AegisGoldBright.copy(alpha = 0.85f * glowPulse),
                                    AegisElectricBlue.copy(alpha = 0.30f)
                                ),
                                startX = shift - travel,
                                endX = shift + travel
                            )
                        )
                    }
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            ) {
                val tabCount = tabs.size.coerceAtLeast(1)
                val tabWidth = maxWidth / tabCount
                val pillWidth = (tabWidth * 0.72f).coerceIn(48.dp, 68.dp)
                val lineWidth = (tabWidth * 0.54f).coerceIn(30.dp, 48.dp)

                val animatedTabOffset by animateDpAsState(
                    targetValue = tabWidth * activeIndex,
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "VipNavIndicatorSlide"
                )

                // Gliding soft active pill behind the selected icon with subtle continuous pulse
                Box(
                    modifier = Modifier
                        .offset(x = animatedTabOffset + (tabWidth - pillWidth) / 2f, y = 8.dp)
                        .width(pillWidth)
                        .height(30.dp)
                        .clip(RoundedCornerShape(50))
                        .background(pillBrush)
                        .drawBehind {
                            drawRoundRect(
                                color = AegisPrimaryCyan.copy(alpha = 0.12f * glowPulse),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                            )
                        }
                        .border(
                            width = 1.dp,
                            color = AegisPrimaryCyan.copy(alpha = 0.26f),
                            shape = RoundedCornerShape(50)
                        )
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { idx, tab ->
                        val selected = currentRoute == tab.route
                        val tabLabel = stringResource(tab.labelRes)
                        val interactionSource = remember(tab.route) { MutableInteractionSource() }
                        val itemTint by animateColorAsState(
                            targetValue = if (selected) AegisPrimaryCyan else AegisTextMuted,
                            animationSpec = tween(durationMillis = 160),
                            label = "NavItemTint_${tab.route}"
                        )
                        val baseScale by animateFloatAsState(
                            targetValue = if (selected) 1.15f else 1f,
                            animationSpec = spring(
                                dampingRatio = 0.65f,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "NavIconScale_${tab.route}"
                        )
                        // Continuous per-icon breathing & micro-tilt directly on each switcher icon
                        val phaseAngle = (auroraPhase * 2f * Math.PI + idx * 1.1).toFloat()
                        val continuousBob = kotlin.math.sin(phaseAngle)
                        val iconLiftDp = if (selected) (-1.8f + continuousBob * 1.6f) else (continuousBob * 0.9f)
                        val iconBreath = 1f + (if (selected) 0.06f else 0.025f) * continuousBob
                        val iconTilt = if (selected) kotlin.math.cos(phaseAngle) * 5.5f else kotlin.math.cos(phaseAngle) * 2.2f

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 48.dp)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = ripple(bounded = false, radius = 34.dp, color = AegisPrimaryCyan),
                                    role = Role.Tab,
                                    onClick = { onSelectTab(tab.route) }
                                )
                                .testTag("nav_tab_${tab.route}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .testTag("vip_animated_nav_icon_${tab.route}")
                                    .size(32.dp)
                                    .graphicsLayer {
                                        translationY = iconLiftDp.dp.toPx()
                                        val s = baseScale * iconBreath
                                        scaleX = s
                                        scaleY = s
                                        rotationZ = iconTilt
                                    }
                                    .drawBehind {
                                        if (selected) {
                                            drawCircle(
                                                brush = Brush.sweepGradient(
                                                    colors = listOf(
                                                        AegisPrimaryCyan.copy(alpha = 0.45f * glowPulse),
                                                        AegisPurple.copy(alpha = 0.45f * glowPulse),
                                                        AegisGoldBright.copy(alpha = 0.40f * glowPulse),
                                                        AegisPrimaryCyan.copy(alpha = 0.45f * glowPulse)
                                                    )
                                                ),
                                                radius = size.minDimension * 0.56f,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6.dp.toPx())
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tabLabel,
                                    tint = itemTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = tabLabel,
                                color = if (selected) AegisTextPrimary else AegisTextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = ClassicalBodyFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
