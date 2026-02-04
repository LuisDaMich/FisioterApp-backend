package com.logikamobile.fisioterapp.services

import com.logikamobile.fisioterapp.data.SessionsTable
import com.logikamobile.fisioterapp.data.PatientsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PatientStatusService {

    /**
     * TRIGGER: Llamar a esto cuando se crea una Sesión o Evaluación.
     * Si el paciente estaba "Agendado", lo pasa a "Activo".
     */
    fun activatePatient(patientId: Int) {
        transaction {
            val currentStatus = PatientsTable.selectAll().where { PatientsTable.id eq patientId }
                .map { it[PatientsTable.status] }
                .singleOrNull()

            if (currentStatus == "Agendado") {
                PatientsTable.update({ PatientsTable.id eq patientId }) {
                    it[status] = "Activo"
                }
            }
        }
    }

    /**
     * TAREA BATCH: Analiza pacientes "Activos" y detecta abandonos.
     * Regla: Si (Hoy - Última Sesión) > 21 días Y no tiene próxima cita -> "Interrumpido".
     * Retorna: Número de pacientes actualizados.
     */
    fun detectAndMarkDropouts(): Int {
        var updatedCount = 0
        val today = LocalDate.now()
        val abandonLimitDays = 21
        transaction {
            val activePatients = PatientsTable.selectAll().where { PatientsTable.status eq "Activo" }
                .map { it[PatientsTable.id].value }

            for (pid in activePatients) {
                val lastSessionDate = SessionsTable
                    .selectAll().where { SessionsTable.patientId eq pid }
                    .orderBy(SessionsTable.date to SortOrder.DESC)
                    .limit(1)
                    .map { it[SessionsTable.date] }
                    .singleOrNull()
                val hasFutureAppointment = SessionsTable
                    .selectAll().where {
                        (SessionsTable.patientId eq pid) and
                                (SessionsTable.nextAppointment greaterEq LocalDateTime.now())
                    }
                    .count() > 0
                if (lastSessionDate != null) {
                    val daysSinceLastVisit = ChronoUnit.DAYS.between(lastSessionDate, today)

                    if (daysSinceLastVisit > abandonLimitDays && !hasFutureAppointment) {
                        PatientsTable.update({ PatientsTable.id eq pid }) {
                            it[status] = "Interrumpido"
                        }
                        updatedCount++
                    }
                }
            }
        }
        return updatedCount
    }
}