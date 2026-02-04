package com.logikamobile.fisioterapp.services

import com.logikamobile.fisioterapp.SessionsTable
import com.logikamobile.fisioterapp.TransactionsTable
import com.logikamobile.fisioterapp.model.dto.SessionDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

class PackageService {

    /**
     * Registra una sesión consumiendo un crédito de un paquete (Transacción) existente.
     * @param patientId ID del paciente
     * @param transactionId ID de la transacción (Paquete) que "paga" esta sesión
     * @param sessionData Datos de la sesión (SOAP notes, etc.)
     * @throws IllegalArgumentException si el paquete está agotado o no existe.
     */
    fun registerSessionWithPackage(patientId: Int, transactionId: Int, sessionData: SessionDTO) {
        transaction {
            val packageTx = TransactionsTable.selectAll()
                .where { (TransactionsTable.id eq transactionId) and (TransactionsTable.patientId eq patientId) }.singleOrNull() ?: throw IllegalArgumentException("El paquete no existe o no pertenece al paciente.")
            if (packageTx[TransactionsTable.type] != "INGRESO") {
                throw IllegalArgumentException("La transacción seleccionada no es un ingreso válido.")
            }
            val notes = packageTx[TransactionsTable.notes] ?: ""
            val category = packageTx[TransactionsTable.category]
            val totalSessionsInPackage = extractSessionLimit(notes, category)
            val usedSessions = SessionsTable.selectAll().where { SessionsTable.transactionId eq transactionId }.count()
            if (usedSessions >= totalSessionsInPackage) {
                throw IllegalStateException("Paquete agotado. Se han usado $usedSessions de $totalSessionsInPackage sesiones.")
            }
            SessionsTable.insert {
                it[this.patientId] = org.jetbrains.exposed.dao.id.EntityID(patientId, com.logikamobile.fisioterapp.PatientsTable)
                it[this.transactionId] = org.jetbrains.exposed.dao.id.EntityID(transactionId, TransactionsTable) // Vinculación Crítica
                it[evaluationId] = sessionData.evaluationId?.let { id -> org.jetbrains.exposed.dao.id.EntityID(id, com.logikamobile.fisioterapp.EvaluationsTable) }
                it[date] = LocalDate.parse(sessionData.date)
                it[sessionNumber] = (usedSessions + 1).toInt() // Autonumérico basado en el paquete
                it[subjective] = sessionData.subjective
                it[objective] = sessionData.objective
                it[treatmentPlan] = sessionData.treatmentPlan
                val currentSessionNum = usedSessions + 1
                it[paymentStatus] = "Paquete ($currentSessionNum/$totalSessionsInPackage)"
                it[cost] = 0.0 // Costo nominal cero
                it[this.notes] = sessionData.notes
                it[nextAppointment] = sessionData.nextAppointment?.let { d -> java.time.LocalDateTime.parse(d) }
            }
        }
    }

    /**
     * Helper para extraer el número de sesiones del texto.
     * Prioridad: Busca en notas, luego asume por defecto.
     */
    private fun extractSessionLimit(notes: String, category: String): Int {
        val regex = Regex("(\\d+)")
        val matchNotes = regex.find(notes)
        if (matchNotes != null) {
            return matchNotes.groupValues[1].toInt()
        }
        if (category.contains("Paquete", ignoreCase = true)) {
            // OJO: Preguntar esta regla de negocio. Por ahora default a 4.
            return 4
        }
        return 1
    }
}