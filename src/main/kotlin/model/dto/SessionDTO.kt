package com.logikamobile.fisioterapp.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class SessionDTO(
    val id: Int? = null,
    val patientId: Int,
    val evaluationId: Int? = null,
    val date: String,
    val sessionNumber: Int,          // Consecutivo: 1, 2, 3..
    val subjective: String? = null,  // "S" - Lo que dice el paciente
    val objective: String? = null,   // "O" - Lo que mides/observas
    val treatmentPlan: String,       // "P" - Plan de tratamiento (Obligatorio)
    val paymentStatus: String,
    val notes: String? = null,       // Notas libres
    val nextAppointment: String? = null,
    val cost: Double? = 0.0,         // Costo individual de esta sesión (para rentabilidad)
    val transactionId: Int? = null   // ID del pago vinculado (si aplica)
)