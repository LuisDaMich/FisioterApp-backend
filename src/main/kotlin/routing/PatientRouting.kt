package com.logikamobile.fisioterapp.routing

import com.logikamobile.fisioterapp.model.dto.PatientDTO
import com.logikamobile.fisioterapp.data.PatientsTable
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.core.readText
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

fun Application.configurePatientRoutes() {
    routing {
        route("/patients") {
            get {
                try {
                    val patientList = transaction {
                        PatientsTable.selectAll().orderBy(PatientsTable.id to SortOrder.ASC).map { row ->
                            PatientDTO(
                                id = row[PatientsTable.id].value,
                                name = row[PatientsTable.name],
                                dob = row[PatientsTable.dob]?.toString(),
                                gender = row[PatientsTable.gender],
                                occupation = row[PatientsTable.occupation],
                                phone = row[PatientsTable.phone],
                                email = row[PatientsTable.email],
                                address = row[PatientsTable.address],
                                emergencyContact = row[PatientsTable.emergencyContact],
                                emergencyPhone = row[PatientsTable.emergencyPhone],
                                registrationDate = row[PatientsTable.registrationDate].toString(),
                                internalId = row[PatientsTable.internalId],
                                status = row[PatientsTable.status]
                            )
                        }
                    }
                    call.respond(HttpStatusCode.OK, patientList)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error al leer: ${e.localizedMessage}")
                }
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                try {
                    val patient = transaction {
                        PatientsTable.selectAll().where(PatientsTable.id.eq(id)).map { row ->
                            PatientDTO(
                                id = row[PatientsTable.id].value,
                                name = row[PatientsTable.name],
                                dob = row[PatientsTable.dob]?.toString(),
                                gender = row[PatientsTable.gender],
                                occupation = row[PatientsTable.occupation],
                                phone = row[PatientsTable.phone],
                                email = row[PatientsTable.email],
                                address = row[PatientsTable.address],
                                emergencyContact = row[PatientsTable.emergencyContact],
                                emergencyPhone = row[PatientsTable.emergencyPhone],
                                registrationDate = row[PatientsTable.registrationDate].toString(),
                                // NUEVOS CAMPOS
                                internalId = row[PatientsTable.internalId],
                                status = row[PatientsTable.status]
                            )
                        }.singleOrNull()
                    }
                    if (patient != null)
                        call.respond(HttpStatusCode.OK, patient)
                    else
                        call.respond(HttpStatusCode.NotFound, "Paciente no encontrado")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error: ${e.localizedMessage}")
                }
            }

            post {
                try {
                    val data = call.receive<PatientDTO>()

                    val newDetails = transaction {
                        val newId = PatientsTable.insertAndGetId {
                            it[name] = data.name
                            it[dob] = data.dob?.let { d -> LocalDate.parse(d) }
                            it[gender] = data.gender
                            it[occupation] = data.occupation
                            it[phone] = data.phone
                            it[email] = data.email
                            it[address] = data.address
                            it[emergencyContact] = data.emergencyContact
                            it[emergencyPhone] = data.emergencyPhone
                            it[status] = data.status ?: "Agendado"
                        }
                        val generatedKey = "FT%03d".format(newId.value)
                        PatientsTable.update({ PatientsTable.id eq newId }) {
                            it[internalId] = generatedKey
                        }
                        Pair(newId.value, generatedKey)
                    }

                    call.respond(HttpStatusCode.Created, "Paciente creado. Clave asignada: ${newDetails.second}")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error al guardar (Verifique formato de fecha AAAA-MM-DD): ${e.localizedMessage}")
                }
            }

            post("/import") {
                val multipart = call.receiveMultipart()
                var insertedRows = 0
                multipart.forEachPart { parte ->
                    if (parte is PartData.FileItem) {
                        val content = parte.provider().readRemaining().readText()
                        val rows = content.lines().filter { it.isNotBlank() }.drop(1) // Saltar header

                        if (rows.isNotEmpty()) {
                            transaction {
                                rows.forEach { rowString ->
                                    val col = rowString.split("|")
                                    if (col.size >= 5) {
                                        val newId = PatientsTable.insertAndGetId {
                                            it[name] = col[0].trim()
                                            it[dob] = try { LocalDate.parse(col[1].trim()) } catch(_:Exception){ null }
                                            it[gender] = col[2].trim()
                                            it[occupation] = col[3].trim()
                                            it[phone] = col[4].trim()
                                            if (col.size > 5) it[email] = col[5].trim()
                                            it[status] = "Agendado"
                                        }
                                        val generatedKey = "FT%03d".format(newId.value)
                                        PatientsTable.update({ PatientsTable.id eq newId }) {
                                            it[internalId] = generatedKey
                                        }
                                    }
                                }
                                insertedRows = rows.size
                            }
                        }
                    }
                    parte.dispose()
                }
                call.respondText("Importación exitosa. $insertedRows pacientes procesados y claves generadas.")
            }

            put("/{id}") {
                val patientId = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
                try {
                    val patientToUpdate = call.receive<PatientDTO>()
                    val rowsAffected = transaction {
                        PatientsTable.update({ PatientsTable.id eq patientId }) { row ->
                            row[name] = patientToUpdate.name
                            row[dob] = patientToUpdate.dob?.let { d -> LocalDate.parse(d) }
                            row[gender] = patientToUpdate.gender
                            row[occupation] = patientToUpdate.occupation
                            row[phone] = patientToUpdate.phone
                            row[email] = patientToUpdate.email
                            row[address] = patientToUpdate.address
                            row[emergencyContact] = patientToUpdate.emergencyContact
                            row[emergencyPhone] = patientToUpdate.emergencyPhone
                            if (patientToUpdate.status != null) {
                                row[status] = patientToUpdate.status
                            }
                        }
                    }
                    if (rowsAffected > 0)
                        call.respond(HttpStatusCode.OK, "Paciente actualizado")
                    else
                        call.respond(HttpStatusCode.NotFound, "No encontrado")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error al actualizar: ${e.localizedMessage}")
                }
            }

            delete("/{id}") {
                val patientId = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)
                try {
                    val rowsAffected = transaction {
                        PatientsTable.deleteWhere { PatientsTable.id eq patientId }
                    }
                    if (rowsAffected > 0)
                        call.respond(HttpStatusCode.OK, "Paciente borrado")
                    else
                        call.respond(HttpStatusCode.NotFound, "No encontrado")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error: ${e.localizedMessage}")
                }
            }
        }
    }
}