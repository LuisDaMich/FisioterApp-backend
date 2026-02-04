package com.logikamobile.fisioterapp.model

import kotlinx.serialization.Serializable

@Serializable
data class PatientDTO(
    val id: Int? = null,
    val name: String,
    val dob: String? = null,
    val gender: String,
    val occupation: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val emergencyContact: String? = null,
    val emergencyPhone: String? = null,
    val registrationDate: String? = null,
    val internalId: String? = null,
    val status: String? = null
)