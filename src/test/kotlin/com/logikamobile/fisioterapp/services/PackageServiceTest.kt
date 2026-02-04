package com.logikamobile.fisioterapp.services

import com.logikamobile.fisioterapp.data.EvaluationsTable
import com.logikamobile.fisioterapp.data.PatientsTable
import com.logikamobile.fisioterapp.data.SessionsTable
import com.logikamobile.fisioterapp.data.TransactionsTable
import com.logikamobile.fisioterapp.model.dto.SessionDTO
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PackageServiceTest {

    private val packageService = PackageService()
    private lateinit var db: Database

    @BeforeEach
    fun setup() {
        // 1. Conectamos a una DB en memoria (H2) que se borra al cerrar
        db = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

        // 2. Creamos las tablas necesarias para la prueba
        transaction {
            SchemaUtils.create(PatientsTable, TransactionsTable, SessionsTable, EvaluationsTable)
        }
    }

    @AfterEach
    fun tearDown() {
        // 3. Borramos las tablas después de cada test para empezar limpio
        transaction {
            SchemaUtils.drop(PatientsTable, TransactionsTable, SessionsTable, EvaluationsTable)
        }
        TransactionManager.closeAndUnregister(db)
    }

    @Test
    fun `should register session successfully when package has credits`() {
        var patientId = 0
        var transactionId = 0

        // ARRANGE (Preparar datos)
        transaction {
            // Crear Paciente
            patientId = PatientsTable.insertAndGetId {
                it[name] = "Juan Pérez"
                it[gender] = "M"
                it[occupation] = "Test"
                it[phone] = "123"
            }.value

            // Crear Transacción (Paquete de 5 sesiones)
            transactionId = TransactionsTable.insertAndGetId {
                it[date] = LocalDate.now()
                it[type] = "INGRESO"
                it[category] = "Paquete"
                it[amount] = 2000.0
                it[paymentMethod] = "Efectivo"
                it[notes] = "Pago Paquete 5 sesiones" // El parser buscará el "5" aquí
                it[this.patientId] = org.jetbrains.exposed.dao.id.EntityID(patientId, PatientsTable)
            }.value
        }

        val sessionDto = createDummySession(patientId)

        // ACT (Ejecutar la lógica)
        packageService.registerSessionWithPackage(patientId, transactionId, sessionDto)

        // ASSERT (Verificar resultados)
        transaction {
            val sessions = SessionsTable.selectAll().toList()
            assertEquals(1, sessions.size, "Debería haber 1 sesión registrada")

            val session = sessions.first()
            assertEquals("Paquete (1/5)", session[SessionsTable.paymentStatus])
            assertEquals(0.0, session[SessionsTable.cost]) // Costo cero porque es paquete
        }
    }

    @Test
    fun `should throw exception when package is exhausted`() {
        var patientId = 0
        var transactionId = 0

        // ARRANGE
        transaction {
            patientId = PatientsTable.insertAndGetId {
                it[name] = "Maria Test"; it[gender] = "F"; it[occupation] = "Test"; it[phone] = "555"
            }.value

            // Paquete pequeño de solo 2 sesiones
            transactionId = TransactionsTable.insertAndGetId {
                it[date] = LocalDate.now(); it[type] = "INGRESO"; it[category] = "Paquete"; it[amount] = 800.0
                it[notes] = "Paquete 2 sesiones"
                it[this.patientId] = org.jetbrains.exposed.dao.id.EntityID(patientId, PatientsTable)
                it[paymentMethod] = "Efectivo"
            }.value

            // Insertamos manualmente 2 sesiones previas consumiendo el paquete
            repeat(2) { num ->
                SessionsTable.insert {
                    it[this.patientId] = org.jetbrains.exposed.dao.id.EntityID(patientId, PatientsTable)
                    it[this.transactionId] = org.jetbrains.exposed.dao.id.EntityID(transactionId, TransactionsTable)
                    it[date] = LocalDate.now()
                    it[sessionNumber] = num + 1
                    it[treatmentPlan] = "Plan"
                    it[paymentStatus] = "Paquete"
                }
            }
        }

        val newSession = createDummySession(patientId)

        // ACT & ASSERT
        val exception = assertThrows(IllegalStateException::class.java) {
            // Intentamos registrar la 3ra sesión en un paquete de 2
            packageService.registerSessionWithPackage(patientId, transactionId, newSession)
        }

        assertTrue(exception.message!!.contains("Paquete agotado"))
    }

    @Test
    fun `should fail if transaction does not belong to patient`() {
        var patientId1 = 0
        var patientId2 = 0
        var transactionIdP1 = 0

        transaction {
            // Paciente 1 (Dueño del paquete)
            patientId1 = PatientsTable.insertAndGetId { it[name] = "Dueño"; it[gender]="M"; it[occupation]=""; it[phone]="" }.value
            // Paciente 2 (Intruso)
            patientId2 = PatientsTable.insertAndGetId { it[name] = "Intruso"; it[gender]="M"; it[occupation]=""; it[phone]="" }.value

            transactionIdP1 = TransactionsTable.insertAndGetId {
                it[date] = LocalDate.now(); it[type] = "INGRESO"; it[category] = "Paquete"; it[amount] = 500.0
                it[notes] = "Paquete 5"; it[paymentMethod] = "Efectivo"
                it[this.patientId] = org.jetbrains.exposed.dao.id.EntityID(patientId1, PatientsTable)
            }.value
        }

        val sessionForIntruso = createDummySession(patientId2)

        // ACT & ASSERT
        assertThrows(IllegalArgumentException::class.java) {
            // Intentamos usar el paquete del Paciente 1 para el Paciente 2
            packageService.registerSessionWithPackage(patientId2, transactionIdP1, sessionForIntruso)
        }
    }

    // Helper para crear DTOs rápido
    private fun createDummySession(patientId: Int): SessionDTO {
        return SessionDTO(
            patientId = patientId,
            date = "2026-02-03",
            sessionNumber = 0, // El servicio lo calcula
            subjective = "Dolor leve",
            objective = "Mejor movilidad",
            treatmentPlan = "Ultrasonido",
            paymentStatus = "PENDIENTE", // El servicio lo sobreescribe
            notes = "Test Note"
        )
    }
}