package com.logikamobile.fisioterapp

import com.logikamobile.fisioterapp.model.*
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
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("FisioterApp Server, up and running")
        }
        route("/auth") {
            post("/register") {
                try {
                    val req = call.receive<RegisterRequest>()
                    val userExists = transaction {
                        UsersTable.selectAll().where { UsersTable.email eq req.email }.count() > 0
                    }
                    if (userExists) {
                        call.respond(HttpStatusCode.Conflict, "El correo ya está registrado")
                        return@post
                    }
                    val newUserId = transaction {
                        UsersTable.insertAndGetId {
                            it[email] = req.email
                            // TODO: En producción, hashear password (ej. BCrypt)
                            it[passwordHash] = req.password
                            it[role] = req.role
                        }
                    }
                    call.respond(HttpStatusCode.Created, mapOf("message" to "Usuario creado", "userId" to newUserId.value))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error al registrar: ${e.localizedMessage}")
                }
            }

            post("/login") {
                try {
                    val req = call.receive<LoginRequest>()
                    val userRow = transaction {
                        UsersTable.selectAll().where { UsersTable.email eq req.email }.singleOrNull()
                    }
                    if (userRow != null && userRow[UsersTable.passwordHash] == req.password) {
                        val userDto = UserDTO(
                            id = userRow[UsersTable.id].value,
                            email = userRow[UsersTable.email],
                            role = userRow[UsersTable.role]
                        )
                        // TODO: Aquí devolverías un JWT real
                        call.respond(HttpStatusCode.OK, AuthResponse(token = "fake-jwt-token-123", user = userDto))
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, "Credenciales inválidas")
                    }
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error en login")
                }
            }
        }
        route("/intake-forms") {
            post {
                try {
                    val dto = call.receive<IntakeFormDTO>()
                    val id = transaction {
                        IntakeFormsTable.insertAndGetId {
                            it[userId] = dto.userId
                            it[fullName] = dto.fullName
                            it[birthDate] = LocalDate.parse(dto.birthDate)
                            it[phone] = dto.phone
                            it[sysMusculoskeletal] = dto.sysMusculoskeletal
                            it[sysNervous] = dto.sysNervous
                            it[sysCardiovascular] = dto.sysCardiovascular
                            it[sysRespiratory] = dto.sysRespiratory
                            it[sysUrogenital] = dto.sysUrogenital
                            it[sysEndocrine] = dto.sysEndocrine
                            it[sysCutaneous] = dto.sysCutaneous
                            it[otherConditions] = dto.otherConditions
                            it[activityLevel] = dto.activityLevel
                            it[patientGoals] = dto.patientGoals?.let { list -> kotlinx.serialization.json.Json.encodeToString(list) }
                            it[weightKg] = dto.weightKg
                            it[heightM] = dto.heightM
                            it[bmi] = dto.bmi
                        }
                    }
                    call.respond(HttpStatusCode.Created, "Formulario recibido ID: ${id.value}")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error: ${e.localizedMessage}")
                }
            }

            get("/user/{userId}") {
                val uId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

                val form = transaction {
                    IntakeFormsTable.selectAll().where { IntakeFormsTable.userId eq uId }
                        .map { row ->
                            IntakeFormDTO(
                                id = row[IntakeFormsTable.id].value,
                                userId = row[IntakeFormsTable.userId].value,
                                fullName = row[IntakeFormsTable.fullName],
                                birthDate = row[IntakeFormsTable.birthDate].toString(),
                                phone = row[IntakeFormsTable.phone],
                                sysMusculoskeletal = row[IntakeFormsTable.sysMusculoskeletal],
                                sysNervous = row[IntakeFormsTable.sysNervous],
                                sysCardiovascular = row[IntakeFormsTable.sysCardiovascular],
                                sysRespiratory = row[IntakeFormsTable.sysRespiratory],
                                sysUrogenital = row[IntakeFormsTable.sysUrogenital],
                                sysEndocrine = row[IntakeFormsTable.sysEndocrine],
                                sysCutaneous = row[IntakeFormsTable.sysCutaneous],
                                otherConditions = row[IntakeFormsTable.otherConditions],
                                activityLevel = row[IntakeFormsTable.activityLevel],
                                patientGoals = row[IntakeFormsTable.patientGoals]?.let { json ->
                                    try { kotlinx.serialization.json.Json.decodeFromString<List<String>>(json) } catch (_: Exception) { emptyList() }
                                },
                                weightKg = row[IntakeFormsTable.weightKg],
                                heightM = row[IntakeFormsTable.heightM],
                                bmi = row[IntakeFormsTable.bmi]
                            )
                        }.singleOrNull()
                }

                if (form != null) call.respond(HttpStatusCode.OK, form)
                else call.respond(HttpStatusCode.NotFound, "No se encontró formulario para este usuario")
            }
        }
        route("/patients") {
            get {
                try {
                    val list = transaction {
                        PatientsTable.selectAll().map { row ->
                            PatientDTO(
                                id = row[PatientsTable.id].value,
                                userId = row[PatientsTable.userId]?.value,
                                name = row[PatientsTable.name],
                                dob = row[PatientsTable.dob]?.toString(),
                                gender = row[PatientsTable.gender],
                                occupation = row[PatientsTable.occupation],
                                phone = row[PatientsTable.phone],
                                email = row[PatientsTable.email],
                                address = row[PatientsTable.address],
                                emergencyContact = row[PatientsTable.emergencyContact],
                                emergencyPhone = row[PatientsTable.emergencyPhone],
                                registrationDate = row[PatientsTable.registrationDate].toString()
                            )
                        }
                    }
                    call.respond(HttpStatusCode.OK, list)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error: ${e.localizedMessage}")
                }
            }
            post {
                try {
                    val data = call.receive<PatientDTO>()
                    val newPatientId = transaction {
                        PatientsTable.insertAndGetId {
                            it[userId] = data.userId
                            it[name] = data.name
                            it[dob] = data.dob?.let { d -> LocalDate.parse(d) }
                            it[gender] = data.gender
                            it[occupation] = data.occupation
                            it[phone] = data.phone
                            it[email] = data.email
                            it[address] = data.address
                            it[emergencyContact] = data.emergencyContact
                            it[emergencyPhone] = data.emergencyPhone
                        }
                    }
                    if (data.userId != null) {
                        transaction {
                            UsersTable.update({ UsersTable.id eq data.userId }) {
                                it[role] = UserRole.ACTIVE_PATIENT
                            }
                        }
                    }
                    call.respond(HttpStatusCode.Created, "Paciente guardado y vinculado con ID: ${newPatientId.value}")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error: ${e.localizedMessage}")
                }
            }
            post("/import") {
                val multipart = call.receiveMultipart()
                var insertedRows = 0
                multipart.forEachPart { parte ->
                    if (parte is PartData.FileItem) {
                        val content = parte.provider().readRemaining().readText()
                        val rows = content.lines().filter { it.isNotBlank() }.drop(1)

                        if (rows.isNotEmpty()) {
                            transaction {
                                //formato: Nombre|FechaNac|Genero|Ocupacion|Telefono|Email
                                PatientsTable.batchInsert(rows) { rowString ->
                                    val col = rowString.split("|")
                                    if (col.size >= 5) {
                                        this[PatientsTable.name] = col[0].trim()
                                        this[PatientsTable.dob] = try { LocalDate.parse(col[1].trim()) } catch(_:Exception){ null }
                                        this[PatientsTable.gender] = col[2].trim()
                                        this[PatientsTable.occupation] = col[3].trim()
                                        this[PatientsTable.phone] = col[4].trim()
                                        if (col.size > 5) this[PatientsTable.email] = col[5].trim()
                                    }
                                }
                            }
                            insertedRows = rows.size
                        }
                    }
                    parte.dispose()
                }
                call.respondText("Importación exitosa. $insertedRows pacientes procesados.")
            }
            route("/{id}") {
                get {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val patient = transaction {
                        PatientsTable.selectAll().where { PatientsTable.id eq id }.map { row ->
                            PatientDTO(
                                id = row[PatientsTable.id].value,
                                userId = row[PatientsTable.userId]?.value,
                                name = row[PatientsTable.name],
                                dob = row[PatientsTable.dob]?.toString(),
                                gender = row[PatientsTable.gender],
                                occupation = row[PatientsTable.occupation],
                                phone = row[PatientsTable.phone],
                                email = row[PatientsTable.email],
                                address = row[PatientsTable.address],
                                emergencyContact = row[PatientsTable.emergencyContact],
                                emergencyPhone = row[PatientsTable.emergencyPhone],
                                registrationDate = row[PatientsTable.registrationDate].toString()
                            )
                        }.singleOrNull()
                    }
                    if (patient != null) call.respond(patient)
                    else call.respond(HttpStatusCode.NotFound)
                }
                put {
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
                            }
                        }
                        if (rowsAffected > 0) call.respond(HttpStatusCode.OK, "Actualizado")
                        else call.respond(HttpStatusCode.NotFound)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "Error al actualizar: ${e.localizedMessage}")
                    }
                }
                delete {
                    val patientId = call.parameters["id"]?.toIntOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    try {
                        val rowsAffected = transaction {
                            PatientsTable.deleteWhere { PatientsTable.id eq patientId }
                        }
                        if (rowsAffected > 0) call.respond(HttpStatusCode.OK, "Eliminado")
                        else call.respond(HttpStatusCode.NotFound)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, "Error al eliminar: ${e.localizedMessage}")
                    }
                }
                route("/medical-history") {
                    get {
                        val idParam = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val history = transaction {
                            MedicalHistoryTable.selectAll().where { MedicalHistoryTable.patientId eq idParam }.map { row ->
                                val systemsJson = row[MedicalHistoryTable.systemsReview]
                                val systemsList = if (!systemsJson.isNullOrBlank()) {
                                    try { kotlinx.serialization.json.Json.decodeFromString<List<SystemReviewItem>>(systemsJson) } catch (_: Exception) { emptyList() }
                                } else emptyList()

                                MedicalHistoryDTO(
                                    id = row[MedicalHistoryTable.id].value,
                                    patientId = row[MedicalHistoryTable.patientId].value,
                                    ahf = row[MedicalHistoryTable.ahf],
                                    app = row[MedicalHistoryTable.app],
                                    apnp = row[MedicalHistoryTable.apnp],
                                    allergies = row[MedicalHistoryTable.allergies],
                                    medications = row[MedicalHistoryTable.medications],
                                    systemsReview = systemsList
                                )
                            }.singleOrNull()
                        }
                        if (history != null) call.respond(history) else call.respond(HttpStatusCode.NotFound)
                    }

                    post {
                        val idParam = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val dto = call.receive<MedicalHistoryDTO>()
                        val systemsString = if (dto.systemsReview != null) kotlinx.serialization.json.Json.encodeToString(dto.systemsReview) else null

                        transaction {
                            val exists =
                                MedicalHistoryTable.selectAll().where { MedicalHistoryTable.patientId eq idParam }.count() > 0
                            if (!exists) {
                                MedicalHistoryTable.insert {
                                    it[patientId] = idParam
                                    it[ahf] = dto.ahf
                                    it[app] = dto.app
                                    it[apnp] = dto.apnp
                                    it[allergies] = dto.allergies
                                    it[medications] = dto.medications
                                    it[systemsReview] = systemsString
                                }
                            } else {
                                MedicalHistoryTable.update({ MedicalHistoryTable.patientId eq idParam }) {
                                    it[ahf] = dto.ahf
                                    it[app] = dto.app
                                    it[apnp] = dto.apnp
                                    it[allergies] = dto.allergies
                                    it[medications] = dto.medications
                                    it[systemsReview] = systemsString
                                }
                            }
                        }
                        call.respond(HttpStatusCode.Created)
                    }
                }
                route("/evaluations") {
                    post {
                        val idParam = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val dto = call.receive<EvaluationDTO>()
                        transaction {
                            EvaluationsTable.insert {
                                it[patientId] = idParam
                                it[evaluationDate] = LocalDate.parse(dto.evaluationDate)
                                it[reasonForConsultation] = dto.reasonForConsultation
                                it[weight] = dto.weight
                                it[height] = dto.height
                                it[bmi] = dto.bmi
                                it[recentWeightLoss] = dto.recentWeightLoss
                                it[weightLossAmount] = dto.weightLossAmount
                                it[reducedFoodIntake] = dto.reducedFoodIntake
                                it[activityLevel] = dto.activityLevel
                                it[functionalityScore] = dto.functionalityScore
                                it[patientGoals] = dto.patientGoals?.joinToString("|")
                                it[painLocation] = dto.painLocation
                                it[painIntensity] = dto.painIntensity
                                it[painCharacter] = dto.painCharacter
                                it[diagnosis] = dto.diagnosis
                                it[prognosis] = dto.prognosis
                            }
                        }
                        call.respond(HttpStatusCode.Created)
                    }

                    get {
                        val idParam = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val list = transaction {
                            EvaluationsTable.selectAll().where { EvaluationsTable.patientId eq idParam }.map { row ->
                                EvaluationDTO(
                                    id = row[EvaluationsTable.id].value,
                                    patientId = row[EvaluationsTable.patientId].value,
                                    evaluationDate = row[EvaluationsTable.evaluationDate].toString(),
                                    reasonForConsultation = row[EvaluationsTable.reasonForConsultation],
                                    weight = row[EvaluationsTable.weight],
                                    height = row[EvaluationsTable.height],
                                    bmi = row[EvaluationsTable.bmi],
                                    recentWeightLoss = row[EvaluationsTable.recentWeightLoss],
                                    weightLossAmount = row[EvaluationsTable.weightLossAmount],
                                    reducedFoodIntake = row[EvaluationsTable.reducedFoodIntake],
                                    activityLevel = row[EvaluationsTable.activityLevel],
                                    functionalityScore = row[EvaluationsTable.functionalityScore],
                                    patientGoals = row[EvaluationsTable.patientGoals]?.split("|"),
                                    painLocation = row[EvaluationsTable.painLocation],
                                    painIntensity = row[EvaluationsTable.painIntensity],
                                    painCharacter = row[EvaluationsTable.painCharacter],
                                    diagnosis = row[EvaluationsTable.diagnosis],
                                    prognosis = row[EvaluationsTable.prognosis]
                                )
                            }
                        }
                        call.respond(list)
                    }
                }
                route("/sessions") {
                    get {
                        val idParam = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
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
                                        nextAppointment = row[SessionsTable.nextAppointment]?.toString()
                                    )
                                }
                        }
                        call.respond(sessions)
                    }
                    post {
                        val idParam = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val dto = call.receive<SessionDTO>()
                        transaction {
                            SessionsTable.insert {
                                it[patientId] = idParam
                                it[evaluationId] = dto.evaluationId?.let { org.jetbrains.exposed.dao.id.EntityID(it, EvaluationsTable) }
                                it[date] = LocalDate.parse(dto.date)
                                it[sessionNumber] = dto.sessionNumber
                                it[subjective] = dto.subjective
                                it[objective] = dto.objective
                                it[treatmentPlan] = dto.treatmentPlan
                                it[paymentStatus] = dto.paymentStatus
                                it[notes] = dto.notes
                                it[nextAppointment] = dto.nextAppointment?.let { LocalDateTime.parse(it) }
                            }
                        }
                        call.respond(HttpStatusCode.Created)
                    }
                }
                route("/functional-assessments") {
                    post {
                        val patientIdParam = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val dto = call.receive<FunctionalAssessmentDTO>()
                        transaction {
                            FunctionalAssessmentsTable.insert {
                                it[patientId] = patientIdParam
                                it[assessmentDate] = LocalDateTime.parse(dto.assessmentDate)
                                it[totalScore] = dto.totalScore
                                it[feedingScore] = dto.feedingScore
                                it[bathingScore] = dto.bathingScore
                                it[groomingScore] = dto.groomingScore
                                it[dressingScore] = dto.dressingScore
                                it[bowelScore] = dto.bowelScore
                                it[bladderScore] = dto.bladderScore
                                it[toiletUseScore] = dto.toiletUseScore
                                it[transfersScore] = dto.transfersScore
                                it[mobilityScore] = dto.mobilityScore
                                it[stairsScore] = dto.stairsScore
                                it[observations] = dto.observations
                            }
                        }
                        call.respond(HttpStatusCode.Created)
                    }

                    get {
                        val patientIdParam = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val list = transaction {
                            FunctionalAssessmentsTable.selectAll()
                                .where { FunctionalAssessmentsTable.patientId eq patientIdParam }
                                .map { row ->
                                    FunctionalAssessmentDTO(
                                        id = row[FunctionalAssessmentsTable.id].value,
                                        patientId = row[FunctionalAssessmentsTable.patientId].value,
                                        assessmentDate = row[FunctionalAssessmentsTable.assessmentDate].toString(),
                                        totalScore = row[FunctionalAssessmentsTable.totalScore],
                                        feedingScore = row[FunctionalAssessmentsTable.feedingScore],
                                        bathingScore = row[FunctionalAssessmentsTable.bathingScore],
                                        groomingScore = row[FunctionalAssessmentsTable.groomingScore],
                                        dressingScore = row[FunctionalAssessmentsTable.dressingScore],
                                        bowelScore = row[FunctionalAssessmentsTable.bowelScore],
                                        bladderScore = row[FunctionalAssessmentsTable.bladderScore],
                                        toiletUseScore = row[FunctionalAssessmentsTable.toiletUseScore],
                                        transfersScore = row[FunctionalAssessmentsTable.transfersScore],
                                        mobilityScore = row[FunctionalAssessmentsTable.mobilityScore],
                                        stairsScore = row[FunctionalAssessmentsTable.stairsScore],
                                        observations = row[FunctionalAssessmentsTable.observations]
                                    )
                                }
                        }
                        call.respond(list)
                    }
                }

            }
        }
    }
}