package com.logikamobile.fisioterapp.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class DashboardDTO(
    val currentMonth: String,        // Ej: "FEBRERO 2026"
    val activePatients: Long,        // Pacientes en tratamiento hoy
    val totalSessionsThisMonth: Long,// Volumen de trabajo
    val incomeThisMonth: Double,     // Dinero que entró
    val expenseThisMonth: Double,    // Dinero que salió
    val netBalance: Double,          // Utilidad (Ingreso - Gasto)
    val averageTicket: Double        // Promedio de venta por paciente (Rentabilidad)
)