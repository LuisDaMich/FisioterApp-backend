package com.logikamobile.fisioterapp.routing

import com.logikamobile.fisioterapp.data.EvaluationsTable
import com.logikamobile.fisioterapp.data.SessionsTable
import com.logikamobile.fisioterapp.model.dto.SessionDTO
import com.logikamobile.fisioterapp.model.request.CreateSessionRequest
import com.logikamobile.fisioterapp.services.PackageService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime

fun Application.configureSessionRoutes() { // Sugiero usar este nombre estándar

    val packageService = PackageService()

    routing {

        // --- PARTE 1: GESTIÓN NORMAL (Historial y Sesiones Sueltas) ---
        route("/patients/{id}/sessions") {

            // 1. Obtener historial (GET)
            get {
                val idParam = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

                try {
                    val sessions = transaction {
                        SessionsTable.select { SessionsTable.patientId eq idParam }
                            .orderBy(SessionsTable.date to SortOrder.DESC)
                            .map { row ->
                                SessionDTO(
                                    id = row[SessionsTable.id].value,
                                    patientId = row[SessionsTable.patientId].value,
                                    evaluationId = row[SessionsTable.evaluationId]?.value,
                                    date = row[SessionsTable.date].toString(),
                                    sessionNumber = row[SessionsTable.sessionNumber],
                                    subjective = row[SessionsTable.subjective],
                                    objective = row[SessionsTable.objective],
                                    treatmentPlan = row[SessionsTable.treatmentPlan],
                                    paymentStatus = row[SessionsTable.paymentStatus],
                                    notes = row[SessionsTable.notes],
                                    nextAppointment = row[SessionsTable.nextAppointment]?.toString(),
                                    cost = row[SessionsTable.cost],
                                    transactionId =row[SessionsTable.transactionId]?.value
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, sessions)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error: ${e.localizedMessage}")
                }
            }

            // 2. Registrar sesión suelta/normal (POST)
            post {
                val idParam = call.parameters["id"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest)
                try {
                    val dto = call.receive<SessionDTO>()
                    transaction {
                        SessionsTable.insert {
                            it[patientId] = idParam
                            it[evaluationId] = dto.evaluationId?.let { id -> org.jetbrains.exposed.dao.id.EntityID(id, EvaluationsTable) }
                            it[date] = LocalDate.parse(dto.date)
                            it[sessionNumber] = dto.sessionNumber
                            it[subjective] = dto.subjective
                            it[objective] = dto.objective
                            it[treatmentPlan] = dto.treatmentPlan
                            it[paymentStatus] = dto.paymentStatus
                            it[notes] = dto.notes
                            it[nextAppointment] = dto.nextAppointment?.let { LocalDateTime.parse(it) }

                            // Campos Financieros opcionales
                            it[cost] = dto.cost ?: 0.0
                            // transactionId se queda nulo en sesión suelta por defecto
                        }
                    }
                    call.respond(HttpStatusCode.Created, "Sesión registrada")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error al guardar: ${e.localizedMessage}")
                }
            }
        }

        // --- PARTE 2: GESTIÓN DE PAQUETES (Tu código) ---
        post("/sessions/consume-package") {
            val request = call.receive<CreateSessionRequest>()
            try {
                packageService.registerSessionWithPackage(
                    patientId = request.patientId,
                    transactionId = request.transactionId,
                    sessionData = request.sessionData
                )
                call.respond(HttpStatusCode.Created, "Sesión de paquete registrada y descontada")
            } catch (e: IllegalArgumentException) {
                // Error de validación (ej. paquete no existe)
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Datos inválidos")
            } catch (e: IllegalStateException) {
                // Error de negocio (ej. paquete agotado)
                call.respond(HttpStatusCode.Conflict, e.message ?: "Paquete agotado")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error interno: ${e.localizedMessage}")
            }
        }
    }
}