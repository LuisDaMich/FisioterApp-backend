package com.logikamobile.fisioterapp.routing

import com.logikamobile.fisioterapp.data.EvaluationsTable
import com.logikamobile.fisioterapp.data.SessionsTable
import com.logikamobile.fisioterapp.model.dto.SessionDTO
import com.logikamobile.fisioterapp.model.request.CreateSessionRequest
import com.logikamobile.fisioterapp.services.PackageService
import com.logikamobile.fisioterapp.services.PatientStatusService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime

fun Application.configureSessionRoutes() {
    val packageService = PackageService()
    val statusService = PatientStatusService()

    routing {
        route("/patients/{id}/sessions") {
            get {
                val idParam = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

                try {
                    val sessions = transaction {
                        SessionsTable.selectAll().where { SessionsTable.patientId eq idParam }
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
                                    transactionId = row[SessionsTable.transactionId]?.value
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, sessions)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error al leer sesiones: ${e.localizedMessage}")
                }
            }

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
                            it[nextAppointment] = dto.nextAppointment?.let { d -> LocalDateTime.parse(d) }
                            it[cost] = dto.cost ?: 0.0
                        }
                    }
                    statusService.activatePatient(idParam)
                    call.respond(HttpStatusCode.Created, "Sesión registrada correctamente")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error al guardar sesión: ${e.localizedMessage}")
                }
            }
        }

        route("/sessions/consume-package") {
            post {
                try {
                    val request = call.receive<CreateSessionRequest>()
                    packageService.registerSessionWithPackage(
                        patientId = request.patientId,
                        transactionId = request.transactionId,
                        sessionData = request.sessionData
                    )
                    statusService.activatePatient(request.patientId)
                    call.respond(HttpStatusCode.Created, "Sesión de paquete registrada y descontada")
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Datos inválidos")
                } catch (e: IllegalStateException) {
                    call.respond(HttpStatusCode.Conflict, e.message ?: "El paquete está agotado")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error interno: ${e.localizedMessage}")
                }
            }
        }
    }
}