package com.logikamobile.fisioterapp.services

import com.logikamobile.fisioterapp.data.EvaluationsTable
import com.logikamobile.fisioterapp.data.PatientsTable
import com.logikamobile.fisioterapp.data.SessionsTable
import com.logikamobile.fisioterapp.data.TransactionsTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DashboardServiceTest {

    private val dashboardService = DashboardService()
    private lateinit var db: Database

    @BeforeEach
    fun setup() {
        // DB en memoria
        db = Database.connect("jdbc:h2:mem:test_dashboard;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

        transaction {
            // Creamos TODAS las tablas para evitar errores de FK
            SchemaUtils.create(PatientsTable, SessionsTable, TransactionsTable, EvaluationsTable)
        }
    }

    @AfterEach
    fun tearDown() {
        transaction {
            // Borramos TODO al terminar
            SchemaUtils.drop(EvaluationsTable, TransactionsTable, SessionsTable, PatientsTable)
        }
        TransactionManager.closeAndUnregister(db)
    }

    @Test
    fun `should calculate monthly metrics correctly`() {
        val today = LocalDate.now()
        val lastMonth = today.minusMonths(1)

        transaction {
            // 1. PACIENTES
            // 2 Activos, 1 Agendado, 1 Baja (Total Activos Esperados: 2)
            PatientsTable.insert { it[name] = "Activo 1"; it[status] = "Activo"; it[gender]="M"; it[phone]=""; it[occupation]="" }
            PatientsTable.insert { it[name] = "Activo 2"; it[status] = "Activo"; it[gender]="M"; it[phone]=""; it[occupation]="" }
            PatientsTable.insert { it[name] = "Nuevo"; it[status] = "Agendado"; it[gender]="M"; it[phone]=""; it[occupation]="" }
            PatientsTable.insert { it[name] = "Viejo"; it[status] = "Baja"; it[gender]="M"; it[phone]=""; it[occupation]="" }

            // 2. SESIONES
            // 3 sesiones este mes (dummy patient id 1), 2 el mes pasado
            // Nota: Usamos dummy IDs asumiendo que el autoincrement empezó en 1
            val pId = org.jetbrains.exposed.dao.id.EntityID(1, PatientsTable)

            repeat(3) { // 3 de ESTE mes
                SessionsTable.insert {
                    it[patientId] = pId; it[date] = today; it[sessionNumber] = 1
                    it[treatmentPlan]=""; it[paymentStatus]=""; it[cost]=0.0
                }
            }
            repeat(2) { // 2 del mes PASADO (No deben contarse)
                SessionsTable.insert {
                    it[patientId] = pId; it[date] = lastMonth; it[sessionNumber] = 1
                    it[treatmentPlan]=""; it[paymentStatus]=""; it[cost]=0.0
                }
            }

            // 3. FINANZAS (Ingresos y Gastos)
            // Ingreso 1: $1000 (Hoy)
            TransactionsTable.insert { it[amount] = 1000.0; it[type] = "INGRESO"; it[date] = today; it[category]="C"; it[paymentMethod]="" }
            // Ingreso 2: $500 (Hoy)
            TransactionsTable.insert { it[amount] = 500.0; it[type] = "INGRESO"; it[date] = today; it[category]="C"; it[paymentMethod]="" }
            // Gasto 1: $300 (Hoy)
            TransactionsTable.insert { it[amount] = 300.0; it[type] = "GASTO"; it[date] = today; it[category]="C"; it[paymentMethod]="" }

            // Ingreso Viejo: $2000 (Mes pasado - NO debe sumar)
            TransactionsTable.insert { it[amount] = 2000.0; it[type] = "INGRESO"; it[date] = lastMonth; it[category]="C"; it[paymentMethod]="" }
        }

        // EJECUTAR
        val metrics = dashboardService.getMonthlyMetrics()

        // VERIFICAR (Asserts)

        // Pacientes Activos
        assertEquals(2, metrics.activePatients, "Debería haber 2 pacientes activos")

        // Sesiones del mes
        assertEquals(3, metrics.totalSessionsThisMonth, "Debería contar solo las 3 sesiones de este mes")

        // Ingresos ($1000 + $500 = $1500)
        assertEquals(1500.0, metrics.incomeThisMonth, 0.01, "Ingresos incorrectos")

        // Gastos ($300)
        assertEquals(300.0, metrics.expenseThisMonth, 0.01, "Gastos incorrectos")

        // Balance Neto ($1500 - $300 = $1200)
        assertEquals(1200.0, metrics.netBalance, 0.01, "Balance incorrecto")

        // Ticket Promedio ($1500 ingresos / 2 pacientes activos = $750)
        assertEquals(750.0, metrics.averageTicket, 0.01, "Ticket promedio incorrecto")
    }

    @Test
    fun `should handle empty data gracefully`() {
        // Probamos con la DB vacía para asegurar que no truene por división entre cero
        val metrics = dashboardService.getMonthlyMetrics()

        assertEquals(0, metrics.activePatients)
        assertEquals(0.0, metrics.incomeThisMonth)
        assertEquals(0.0, metrics.averageTicket, "Debería ser 0.0 si no hay pacientes (evitar división por cero)")
    }
}