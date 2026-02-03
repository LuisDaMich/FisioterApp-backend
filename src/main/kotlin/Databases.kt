package com.logikamobile.fisioterapp

import com.logikamobile.fisioterapp.model.ActivityLevel
import com.logikamobile.fisioterapp.model.UserRole
import io.ktor.server.application.Application
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
// ... imports ...

fun Application.configureDatabases() {
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/fisioterapp_db",
        driver = "org.postgresql.Driver",
        user = "admin_fisio",
        password = "password_seguro_123"
    )

    transaction {
        val tables = arrayOf(
            UsersTable,
            PatientsTable,
            MedicalHistoryTable,
            EvaluationsTable,
            SessionsTable,
            IntakeFormsTable,
            FunctionalAssessmentsTable
        )
        val statements = SchemaUtils.statementsRequiredToActualizeScheme(*tables)
        if (statements.isEmpty()) {
            println("El esquema no sufrió cambios.")
        } else {
            println("Se detectaron cambios en el esquema.")
            println("   -> Aplicando ${statements.size} cambios SQL...")
            statements.forEach { sql -> println("SQL: $sql\n") }
            SchemaUtils.createMissingTablesAndColumns(*tables)
            println("Esquema creado exitosamente.")
        }
    }
}
object PatientsTable : IntIdTable("patients") {
    val userId = reference("user_id", UsersTable).nullable()
    val name = varchar("name", 100)
    val dob = date("date_of_birth").nullable()
    val gender = varchar("gender", 20)
    val occupation = varchar("occupation", 100)
    val phone = varchar("phone", 20)
    val email = varchar("email", 100).nullable()
    val address = varchar("address", 200).nullable()
    val emergencyContact = varchar("emergency_contact", 100).nullable()
    val emergencyPhone = varchar("emergency_phone", 20).nullable()
    val registrationDate = datetime("registration_date").clientDefault { java.time.LocalDateTime.now() }
}
object MedicalHistoryTable : IntIdTable("medical_history") {
    val patientId = reference("patient_id", PatientsTable)
    val ahf = text("family_history").nullable()
    val app = text("pathological_history").nullable()
    val apnp = text("non_pathological").nullable()
    val allergies = varchar("allergies", 200).nullable()
    val systemsReview = text("systems_review").nullable()
    val medications = text("medications").nullable()
}
object EvaluationsTable : IntIdTable("evaluations") {
    val patientId = reference("patient_id", PatientsTable)
    val evaluationDate = date("evaluation_date")
    val reasonForConsultation = text("reason")

    // SECCIÓN NUEVA: Datos Antropométricos (Del PDF)
    val weight = float("weight").nullable()          // Peso kg
    val height = float("height").nullable()          // Talla m
    val bmi = float("bmi").nullable()                // IMC
    val recentWeightLoss = bool("recent_weight_loss").nullable() // "¿Ha perdido peso?"
    val weightLossAmount = varchar("weight_loss_amount", 50).nullable() // "¿Cuánto?"
    val reducedFoodIntake = bool("reduced_food_intake").nullable() // "¿Ha dejado de comer?"

    // SECCIÓN NUEVA: Funcionalidad y Actividad
    val activityLevel = varchar("activity_level", 50).nullable() // "Sedentario", "Activo", etc.
    val functionalityScore = integer("functionality_score").nullable() // Escala 1-10
    val patientGoals = text("patient_goals").nullable() // Selección múltiple de objetivos

    // Diagnóstico Fisioterapéutico (Dolor y Análisis)
    val painLocation = varchar("pain_location", 100).nullable()
    val painIntensity = integer("pain_intensity").nullable()
    val painCharacter = varchar("pain_character", 100).nullable()
    val diagnosis = text("diagnosis").nullable()
    val prognosis = text("prognosis").nullable()
}
object SessionsTable : IntIdTable("sessions") {
    val patientId = reference("patient_id", PatientsTable)
    val evaluationId = reference("evaluation_id", EvaluationsTable).nullable()
    val date = date("session_date")
    val sessionNumber = integer("session_number")
    val subjective = text("subjective_notes").nullable()
    val objective = text("objective_notes").nullable()
    val treatmentPlan = text("treatment_plan")
    val paymentStatus = varchar("payment_status", 50)
    val notes = text("notes").nullable()
    val nextAppointment = datetime("next_appointment").nullable()
}
object UsersTable : IntIdTable("users") {
    val email = varchar("email", 128).uniqueIndex()
    val passwordHash = varchar("password_hash", 256)
    val role = enumerationByName("role", 20, UserRole::class).default(UserRole.GUEST)
    val fcmToken = varchar("fcm_token", 256).nullable() // Para notificaciones Push
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
}
object IntakeFormsTable : IntIdTable("intake_forms") {
    val userId = reference("user_id", UsersTable)
    val fullName = varchar("full_name", 128)
    val birthDate = date("birth_date")
    val phone = varchar("phone", 20)
    val sysMusculoskeletal = bool("sys_musculoskeletal").default(false)
    val sysNervous = bool("sys_nervous").default(false)
    val sysCardiovascular = bool("sys_cardiovascular").default(false)
    val sysRespiratory = bool("sys_respiratory").default(false)
    val sysUrogenital = bool("sys_urogenital").default(false)
    val sysEndocrine = bool("sys_endocrine").default(false) // Diabetes, etc.
    val sysCutaneous = bool("sys_cutaneous").default(false)
    val otherConditions = text("other_conditions").nullable()
    val activityLevel = enumerationByName("activity_level", 20, ActivityLevel::class).nullable()
    val patientGoals = text("patient_goals").nullable()
    val weightKg = float("weight_kg").nullable()
    val heightM = float("height_m").nullable()
    val bmi = float("bmi").nullable()
}
object FunctionalAssessmentsTable : IntIdTable("functional_assessments") {
    val patientId = reference("patient_id", PatientsTable)
    val assessmentDate = datetime("assessment_date").clientDefault { LocalDateTime.now() }
    val feedingScore = integer("feeding_score").default(0)
    val bathingScore = integer("bathing_score").default(0)
    val groomingScore = integer("grooming_score").default(0)
    val dressingScore = integer("dressing_score").default(0)
    val bowelScore = integer("bowel_score").default(0)
    val bladderScore = integer("bladder_score").default(0)
    val toiletUseScore = integer("toilet_use_score").default(0)
    val transfersScore = integer("transfers_score").default(0)
    val mobilityScore = integer("mobility_score").default(0)
    val stairsScore = integer("stairs_score").default(0)
    val totalScore = integer("total_score").default(0)
    val observations = text("observations").nullable()
}