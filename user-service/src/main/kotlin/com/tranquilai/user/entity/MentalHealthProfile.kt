package com.tranquilai.user.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "mental_health_profiles")
class MentalHealthProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    // Questionnaire answers (comma-separated lists stored as TEXT)
    @Column(name = "current_feeling_level")
    var currentFeelingLevel: String? = null,

    @Column(name = "stress_causes", columnDefinition = "TEXT")
    var stressCauses: String? = null,

    @Column(name = "current_concerns", columnDefinition = "TEXT")
    var currentConcerns: String? = null,

    @Column(name = "mental_process_preferences", columnDefinition = "TEXT")
    var mentalProcessPreferences: String? = null,

    @Column(name = "personal_goals", columnDefinition = "TEXT")
    var personalGoals: String? = null,

    @Column(name = "identified_triggers", columnDefinition = "TEXT")
    var identifiedTriggers: String? = null,

    // AI-generated insights
    @Column(name = "personality_analysis", columnDefinition = "TEXT")
    var personalityAnalysis: String? = null,

    @Column(name = "emotional_patterns", columnDefinition = "TEXT")
    var emotionalPatterns: String? = null,

    @Column(name = "risk_factors", columnDefinition = "TEXT")
    var riskFactors: String? = null,

    @Column(name = "identified_strengths", columnDefinition = "TEXT")
    var identifiedStrengths: String? = null,

    @Column(name = "recommended_approach", columnDefinition = "TEXT")
    var recommendedApproach: String? = null,

    @Column(name = "ai_coping_strategies", columnDefinition = "TEXT")
    var aiCopingStrategies: String? = null,

    @Column(name = "ai_focus_areas", columnDefinition = "TEXT")
    var aiFocusAreas: String? = null,

    // AI assessment levels
    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_level", nullable = false)
    var urgencyLevel: UrgencyLevel = UrgencyLevel.LOW,

    @Enumerated(EnumType.STRING)
    @Column(name = "support_intensity", nullable = false)
    var supportIntensity: SupportIntensity = SupportIntensity.LIGHT,

    @Enumerated(EnumType.STRING)
    @Column(name = "communication_style")
    var communicationStyle: CommunicationStyle? = null,

    // Baseline metrics (1-10)
    @Column(name = "baseline_anxiety_level")
    var baselineAnxietyLevel: Int? = null,

    @Column(name = "baseline_depression_level")
    var baselineDepressionLevel: Int? = null,

    @Column(name = "baseline_stress_level")
    var baselineStressLevel: Int? = null,

    @Column(name = "baseline_wellbeing_level")
    var baselineWellbeingLevel: Int? = null,

    @Column(name = "baseline_coping_ability")
    var baselineCopingAbility: Int? = null,

    // Analysis metadata
    @Column(name = "ai_analysis_version")
    var aiAnalysisVersion: String? = null,

    @Column(name = "ai_confidence_score")
    var aiConfidenceScore: Double? = null,

    @Column(name = "last_ai_analysis_at")
    var lastAiAnalysisAt: Long? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis(),
)

enum class UrgencyLevel { LOW, MEDIUM, HIGH, CRISIS }
enum class SupportIntensity { LIGHT, MODERATE, INTENSIVE, CLINICAL_REFERRAL }
enum class CommunicationStyle { DIRECT, GENTLE, ENCOURAGING, ANALYTICAL, EMPATHETIC }
