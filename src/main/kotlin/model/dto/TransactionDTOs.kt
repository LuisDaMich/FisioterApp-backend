package com.logikamobile.fisioterapp.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDTO(
    val id: Int? = null,
    val date: String,
    val type: String,           // "INGRESO" o "GASTO"
    val category: String,       // "Paquete", "Sesión", "Material", "Servicios"
    val amount: Double,
    val paymentMethod: String,
    val notes: String? = null,
    val patientId: Int? = null,
    val patientName: String? = null
)

@Serializable
data class BalanceDTO(
    val totalIncome: Double,
    val totalExpenses: Double,
    val netBalance: Double
)