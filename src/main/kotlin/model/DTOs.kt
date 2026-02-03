package com.logikamobile.fisioterapp.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    GUEST,
    ACTIVE_PATIENT,
    PHYSIO,
    ADMIN
}

@Serializable
enum class ActivityLevel {
    SEDENTARY,
    MINIMALLY_ACTIVE,
    MODERATELY_ACTIVE,
    ACTIVE,
    VERY_ACTIVE
}
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val role: UserRole = UserRole.GUEST
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserDTO
)

@Serializable
data class UserDTO(
    val id: Int,
    val email: String,
    val role: UserRole
)
@Serializable
data class IntakeFormDTO(
    val id: Int? = null,
    val userId: Int,
    val fullName: String,
    val birthDate: String,
    val phone: String,
    val sysMusculoskeletal: Boolean = false,
    val sysNervous: Boolean = false,
    val sysCardiovascular: Boolean = false,
    val sysRespiratory: Boolean = false,
    val sysUrogenital: Boolean = false,
    val sysEndocrine: Boolean = false,
    val sysCutaneous: Boolean = false,
    val otherConditions: String? = null,
    val activityLevel: ActivityLevel? = null,
    val patientGoals: List<String>? = null,
    val weightKg: Float? = null,
    val heightM: Float? = null,
    val bmi: Float? = null
)
@Serializable
data class PatientDTO(
    val id: Int? = null,
    val userId: Int? = null,
    val name: String,
    val dob: String? = null,
    val gender: String,
    val occupation: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val emergencyContact: String? = null,
    val emergencyPhone: String? = null,
    val registrationDate: String? = null
)

@Serializable
data class SessionDTO(
    val id: Int? = null,
    val patientId: Int,
    val evaluationId: Int? = null,
    val date: String,
    val sessionNumber: Int,
    val subjective: String? = null,
    val objective: String? = null,
    val treatmentPlan: String,
    val paymentStatus: String,
    val notes: String? = null,
    val nextAppointment: String? = null
)
@Serializable
data class SystemReviewItem(
    val system: String,
    val hasCondition: Boolean,
    val inTreatment: Boolean
)

@Serializable
data class MedicalHistoryDTO(
    val id: Int? = null,
    val patientId: Int,
    val ahf: String? = null,
    val app: String? = null,
    val apnp: String? = null,
    val allergies: String? = null,
    val medications: String? = null,
    val systemsReview: List<SystemReviewItem>? = null
)

@Serializable
data class EvaluationDTO(
    val id: Int? = null,
    val patientId: Int,
    val evaluationDate: String,
    val reasonForConsultation: String,
    val weight: Float? = null,
    val height: Float? = null,
    val bmi: Float? = null,
    val recentWeightLoss: Boolean? = null,
    val weightLossAmount: String? = null,
    val reducedFoodIntake: Boolean? = null,
    val activityLevel: String? = null,
    val functionalityScore: Int? = null,
    val patientGoals: List<String>? = null,
    val painLocation: String? = null,
    val painIntensity: Int? = null,
    val painCharacter: String? = null,
    val diagnosis: String? = null,
    val prognosis: String? = null
)
@Serializable
data class FunctionalAssessmentDTO(
    val id: Int? = null,
    val patientId: Int,
    val assessmentDate: String,
    val feedingScore: Int = 0,
    val bathingScore: Int = 0,
    val groomingScore: Int = 0,
    val dressingScore: Int = 0,
    val bowelScore: Int = 0,
    val bladderScore: Int = 0,
    val toiletUseScore: Int = 0,
    val transfersScore: Int = 0,
    val mobilityScore: Int = 0,
    val stairsScore: Int = 0,
    val totalScore: Int = 0,
    val observations: String? = null
)