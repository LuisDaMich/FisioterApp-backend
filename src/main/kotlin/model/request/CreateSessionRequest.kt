package com.logikamobile.fisioterapp.model.request

import com.logikamobile.fisioterapp.model.dto.SessionDTO
import kotlinx.serialization.Serializable

/**
 * Request Body para el endpoint de "Consumir Sesión de Paquete".
 * Se usa para garantizar que el cliente envíe el ID del paquete explícitamente.
 */
@Serializable
data class CreateSessionRequest(
    val patientId: Int,
    val transactionId: Int,
    val sessionData: SessionDTO
)