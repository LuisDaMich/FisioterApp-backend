package com.logikamobile.fisioterapp.services

import com.logikamobile.fisioterapp.data.EvaluationsTable
import com.logikamobile.fisioterapp.data.PatientsTable
import com.logikamobile.fisioterapp.data.SessionsTable
import com.logikamobile.fisioterapp.data.TransactionsTable
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
import java.time.LocalDateTime

class PatientStatusServiceTest {

    private val statusService = PatientStatusService()
    private lateinit var db: Database

    @BeforeEach
    fun setup() {
        // Conectamos a la DB en memoria
        db = Database.connect("jdbc:h2:mem:test_status;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

        transaction {
            // CORRECCIÓN: Creamos TODAS las tablas para evitar errores de dependencias cruzadas
            SchemaUtils.create(PatientsTable, SessionsTable, TransactionsTable, EvaluationsTable)
        }
    }

    @AfterEach
    fun tearDown() {
        transaction {
            // CORRECCIÓN CRÍTICA: Borramos TODAS las tablas.
            // Al ponerlas todas aquí, Exposed se encarga de borrarlas.
            SchemaUtils.drop(EvaluationsTable, TransactionsTable, SessionsTable, PatientsTable)
        }
        // Cerramos la conexión correctamente usando la variable 'db'
        TransactionManager.closeAndUnregister(db)
    }

    // --- TUS PRUEBAS ---

    @Test
    fun `should activate patient when currently Agendado`() {
        var patientId = 0

        transaction {
            patientId = PatientsTable.insertAndGetId {
                it[name] = "Nuevo Paciente"
                it[status] = "Agendado"
                it[gender] = "M"; it[phone] = "555"; it[occupation] = "Test"
            }.value
        }

        statusService.activatePatient(patientId)

        transaction {
            val status = PatientsTable.selectAll().where { PatientsTable.id eq patientId }
                .single()[PatientsTable.status]
            assertEquals("Activo", status)
        }
    }

    @Test
    fun `should NOT change status if patient is already Baja or Concluido`() {
        var patientId = 0
        transaction {
            patientId = PatientsTable.insertAndGetId {
                it[name] = "Paciente Alta"
                it[status] = "Concluido"
                it[gender] = "F"; it[phone] = "555"; it[occupation] = "Test"
            }.value
        }

        statusService.activatePatient(patientId)

        transaction {
            val status = PatientsTable.selectAll().where { PatientsTable.id eq patientId }
                .single()[PatientsTable.status]
            assertEquals("Concluido", status)
        }
    }

    @Test
    fun `should mark as Interrumpido if last session was long ago and NO future appointment`() {
        transaction {
            val pId = PatientsTable.insertAndGetId {
                it[name] = "Fantasma"; it[status] = "Activo"; it[gender] = "M"; it[phone] = ""; it[occupation] = ""
            }.value

            SessionsTable.insert {
                it[patientId] = org.jetbrains.exposed.dao.id.EntityID(pId, PatientsTable)
                it[date] = LocalDate.now().minusDays(25)
                it[sessionNumber] = 1; it[treatmentPlan] = ""; it[paymentStatus] = ""; it[cost] = 0.0
            }
        }

        val updatedCount = statusService.detectAndMarkDropouts()

        assertEquals(1, updatedCount)
        transaction {
            val status = PatientsTable.selectAll().where { PatientsTable.name eq "Fantasma" }
                .single()[PatientsTable.status]
            assertEquals("Interrumpido", status)
        }
    }

    @Test
    fun `should KEEP Active if patient has future appointment despite long absence`() {
        transaction {
            val pId = PatientsTable.insertAndGetId {
                it[name] = "Viajero"; it[status] = "Activo"; it[gender] = "M"; it[phone] = ""; it[occupation] = ""
            }.value

            SessionsTable.insert {
                it[patientId] = org.jetbrains.exposed.dao.id.EntityID(pId, PatientsTable)
                it[date] = LocalDate.now().minusDays(30)
                it[sessionNumber] = 1; it[treatmentPlan] = ""; it[paymentStatus] = ""; it[cost] = 0.0
                it[nextAppointment] = LocalDateTime.now().plusDays(1)
            }
        }

        val updatedCount = statusService.detectAndMarkDropouts()

        assertEquals(0, updatedCount)
        transaction {
            val status = PatientsTable.selectAll().where { PatientsTable.name eq "Viajero" }
                .single()[PatientsTable.status]
            assertEquals("Activo", status)
        }
    }

    @Test
    fun `should KEEP Active if last session was recent`() {
        transaction {
            val pId = PatientsTable.insertAndGetId {
                it[name] = "Cumplido"; it[status] = "Activo"; it[gender] = "M"; it[phone] = ""; it[occupation] = ""
            }.value

            SessionsTable.insert {
                it[patientId] = org.jetbrains.exposed.dao.id.EntityID(pId, PatientsTable)
                it[date] = LocalDate.now().minusDays(1)
                it[sessionNumber] = 1; it[treatmentPlan] = ""; it[paymentStatus] = ""; it[cost] = 0.0
            }
        }

        val updatedCount = statusService.detectAndMarkDropouts()
        assertEquals(0, updatedCount)
    }
}