package com.example.detection.decision

import com.example.core.models.SensitivityLevel
import com.example.core.models.ThreatCategory
import com.example.core.models.ThreatSeverity
import com.example.detection.classifier.ClassificationResult

data class DecisionOutcome(
    val shouldBlock: Boolean,
    val category: ThreatCategory,
    val severity: ThreatSeverity,
    val confidence: Float,
    val reason: String
)

class ThreatDecisionEngine {

    // Consecutive violation counter to filter out transient single-frame camera/animation glitches
    private var consecutiveSuspiciousCount: Int = 0
    private var lastSuspectCategory: ThreatCategory = ThreatCategory.BENIGN_SAFE

    /**
     * Evaluates a classification result against the active sensitivity policy and strict everything flag.
     */
    fun evaluate(
        result: ClassificationResult,
        sensitivity: SensitivityLevel,
        isStrictEverything: Boolean,
        explicitThresholdOverride: Float? = null,
        suggestiveThresholdOverride: Float? = null,
        consecutiveFramesRequired: Int = 2,
        blockExplicitAdult: Boolean = true,
        blockSuggestiveContent: Boolean = true,
        blockSensitiveGraphic: Boolean = true
    ): DecisionOutcome {
        val explicitThreshold = explicitThresholdOverride ?: sensitivity.threshold
        val suggestiveThreshold = suggestiveThresholdOverride ?: sensitivity.threshold
        val requiredFrames = consecutiveFramesRequired.coerceIn(1, 5)
        val explicit = result.explicitScore
        val suggestive = result.suggestiveScore
        val safe = result.safeScore

        // 1. Explicit Adult Content: Immediate critical threat
        if (blockExplicitAdult && (explicit >= explicitThreshold || (isStrictEverything && explicit >= 0.50f))) {
            consecutiveSuspiciousCount = 0
            return DecisionOutcome(
                shouldBlock = true,
                category = ThreatCategory.EXPLICIT_ADULT,
                severity = ThreatSeverity.CRITICAL,
                confidence = explicit,
                reason = "Explicit adult visual content detected with ${(explicit * 100).toInt()}% confidence"
            )
        }

        // 2. Sensitive / Graphic visual check (if model exposes SENSITIVE_GRAPHIC or graphic score)
        if (blockSensitiveGraphic && result.topCategory == ThreatCategory.SENSITIVE_GRAPHIC &&
            result.confidence >= explicitThreshold
        ) {
            consecutiveSuspiciousCount = 0
            return DecisionOutcome(
                shouldBlock = true,
                category = ThreatCategory.SENSITIVE_GRAPHIC,
                severity = ThreatSeverity.HIGH,
                confidence = result.confidence,
                reason = "Sensitive graphic media detected with ${(result.confidence * 100).toInt()}% confidence"
            )
        }

        // 3. Suggestive Adult / Provocative Content:
        if (blockSuggestiveContent && (suggestive >= suggestiveThreshold || (isStrictEverything && suggestive >= 0.55f))) {
            if (isStrictEverything || suggestive >= 0.85f || requiredFrames <= 1) {
                // High confidence suggestive, Strict Everything enabled, or 1-frame immediate confirmation
                consecutiveSuspiciousCount = 0
                return DecisionOutcome(
                    shouldBlock = true,
                    category = ThreatCategory.SUGGESTIVE_CONTENT,
                    severity = ThreatSeverity.HIGH,
                    confidence = suggestive,
                    reason = "Suggestive visual content identified with ${(suggestive * 100).toInt()}% confidence"
                )
            } else {
                // Borderline suggestive: require configured consecutive frame confirmation
                consecutiveSuspiciousCount++
                if (consecutiveSuspiciousCount >= requiredFrames) {
                    consecutiveSuspiciousCount = 0
                    return DecisionOutcome(
                        shouldBlock = true,
                        category = ThreatCategory.SUGGESTIVE_CONTENT,
                        severity = ThreatSeverity.MEDIUM,
                        confidence = suggestive,
                        reason = "Consecutive suggestive frames ($requiredFrames) confirmed policy violation"
                    )
                }
            }
        } else {
            // Clean or category-exempted frame: reset consecutive counter
            consecutiveSuspiciousCount = 0
        }

        return DecisionOutcome(
            shouldBlock = false,
            category = ThreatCategory.BENIGN_SAFE,
            severity = ThreatSeverity.LOW,
            confidence = safe,
            reason = "Content approved safe (Safe score: ${(safe * 100).toInt()}%)"
        )
    }

    fun isAwaitingConfirmation(): Boolean = consecutiveSuspiciousCount > 0

    fun reset() {
        consecutiveSuspiciousCount = 0
        lastSuspectCategory = ThreatCategory.BENIGN_SAFE
    }
}
