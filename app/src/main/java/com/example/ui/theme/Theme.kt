package com.example.ui.theme

import android.content.Context
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.example.core.audio.AegisSoundEffects
import kotlinx.coroutines.launch

private val SheildlenzColorScheme = lightColorScheme(
    primary = AegisPrimaryCyan,
    onPrimary = AegisOnPrimaryWhite,
    primaryContainer = AegisSecondaryBackground,
    onPrimaryContainer = AegisPrimaryCyan,
    secondary = AegisPurple,
    onSecondary = AegisOnPrimaryWhite,
    secondaryContainer = AegisLavenderSoft,
    onSecondaryContainer = AegisPurple,
    tertiary = AegisChampagneGold,
    onTertiary = AegisOnPrimaryWhite,
    tertiaryContainer = AegisChampagneSoft,
    onTertiaryContainer = AegisWarningAmber,
    background = AegisVoidBackground,
    onBackground = AegisTextPrimary,
    surface = AegisCardBackground,
    onSurface = AegisTextPrimary,
    surfaceVariant = AegisLavenderSoft,
    onSurfaceVariant = AegisTextSecondary,
    outline = AegisCardBorder,
    error = AegisWarningAmber,
    onError = AegisOnPrimaryWhite,
    errorContainer = AegisChampagneSoft,
    onErrorContainer = AegisWarningAmber
)

private val SheildlenzShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private class WaterDropIndicationNodeFactory(
    private val context: Context,
    private val baseRipple: IndicationNodeFactory = ripple()
) : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return WaterDropIndicationNode(
            context = context,
            interactionSource = interactionSource,
            rippleDelegate = baseRipple.create(interactionSource)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WaterDropIndicationNodeFactory) return false
        return baseRipple == other.baseRipple
    }

    override fun hashCode(): Int = baseRipple.hashCode()
}

private class WaterDropIndicationNode(
    private val context: Context,
    private val interactionSource: InteractionSource,
    rippleDelegate: DelegatableNode
) : DelegatingNode() {

    init {
        delegate(rippleDelegate)
    }

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Press) {
                    AegisSoundEffects.playTap(context)
                }
            }
        }
    }
}

internal fun Modifier.captureTactileButtonPress(context: Context): Modifier =
    this.pointerInput(context) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                if (event.changes.any { it.changedToDownIgnoreConsumed() && it.isConsumed }) {
                    AegisSoundEffects.playTap(context)
                }
            }
        }
    }

@Composable
fun AegisShieldTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    SideEffect {
        view.isSoundEffectsEnabled = false
    }
    LaunchedEffect(context) {
        AegisSoundEffects.prewarm(context)
    }
    val waterDropIndication = remember(context) {
        WaterDropIndicationNodeFactory(context)
    }

    MaterialTheme(
        colorScheme = SheildlenzColorScheme,
        typography = Typography,
        shapes = SheildlenzShapes
    ) {
        CompositionLocalProvider(
            LocalIndication provides waterDropIndication
        ) {
            Box(modifier = Modifier.captureTactileButtonPress(context)) {
                content()
            }
        }
    }
}
