package com.logikamobile.fisioterapp.services

import com.logikamobile.fisioterapp.data.PatientsTable
import com.logikamobile.fisioterapp.data.SessionsTable
import com.logikamobile.fisioterapp.data.TransactionsTable
import com.logikamobile.fisioterapp.model.dto.DashboardDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.month
import org.jetbrains.exposed.sql.javatime.year
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

class DashboardService {

    fun getMonthlyMetrics(): DashboardDTO {
        val today = LocalDate.now()
        val currentMonth = today.monthValue
        val currentYear = today.year

        return transaction {
            // 1. Métricas de Pacientes
            val activeCount = PatientsTable.selectAll().where { PatientsTable.status eq "Activo" }.count()

            // 2. Métricas de Sesiones (Este mes)
            val sessionsCount = SessionsTable.selectAll()
                .where { (SessionsTable.date.month() eq currentMonth) and (SessionsTable.date.year() eq currentYear) }.count()

            // 3. Métricas Financieras (Este mes)
            // Suma de Ingresos
            val income = TransactionsTable.selectAll().where {
                (TransactionsTable.type eq "INGRESO") and
                        (TransactionsTable.date.month() eq currentMonth) and
                        (TransactionsTable.date.year() eq currentYear)
            }.sumOf { it[TransactionsTable.amount] }

            // Suma de Gastos
            val expenses = TransactionsTable.selectAll().where {
                (TransactionsTable.type eq "GASTO") and
                        (TransactionsTable.date.month() eq currentMonth) and
                        (TransactionsTable.date.year() eq currentYear)
            }.sumOf { it[TransactionsTable.amount] }

            // 4. Cálculos derivados
            // Ticket Promedio: Si hay pacientes activos, ¿cuánto paga cada uno en promedio?
            // (Evitamos división entre cero)
            val avgTicket = if (activeCount > 0) income / activeCount else 0.0

            DashboardDTO(
                currentMonth = "${today.month.name} $currentYear",
                activePatients = activeCount,
                totalSessionsThisMonth = sessionsCount,
                incomeThisMonth = income,
                expenseThisMonth = expenses,
                netBalance = income - expenses,
                averageTicket = avgTicket
            )
        }
    }
}