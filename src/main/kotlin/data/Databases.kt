package com.logikamobile.fisioterapp.data

import io.ktor.server.application.Application
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

fun Application.configureDatabases() {
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/fisioterapp_db",
        driver = "org.postgresql.Driver",
        user = "admin_fisio",
        password = "password_seguro_123"
    )
    transaction {
        SchemaUtils.createMissingTablesAndColumns(
            PatientsTable,
            MedicalHistoryTable,
            EvaluationsTable,
            SessionsTable,
            TransactionsTable
        )
    }
}

object PatientsTable : IntIdTable("patients") {
    val name = varchar("name", 100)
    val dob = date("date_of_birth").nullable()
    val gender = varchar("gender", 20)
    val occupation = varchar("occupation", 100)
    val phone = varchar("phone", 20)
    val email = varchar("email", 100).nullable()
    val address = varchar("address", 200).nullable()
    val emergencyContact = varchar("emergency_contact", 100).nullable()
    val emergencyPhone = varchar("emergency_phone", 20).nullable()
    val registrationDate = datetime("registration_date").clientDefault { LocalDateTime.now() }
    val internalId = varchar("internal_id", 20).nullable()
    val status = varchar("status", 50).default("Agendado")
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
    val weight = float("weight").nullable()
    val height = float("height").nullable()
    val bmi = float("bmi").nullable()
    val recentWeightLoss = bool("recent_weight_loss").nullable()
    val weightLossAmount = varchar("weight_loss_amount", 50).nullable()
    val reducedFoodIntake = bool("reduced_food_intake").nullable()
    val activityLevel = varchar("activity_level", 50).nullable()
    val functionalityScore = integer("functionality_score").nullable()
    val patientGoals = text("patient_goals").nullable()
    val painLocation = varchar("pain_location", 100).nullable()
    val painIntensity = integer("pain_intensity").nullable()
    val painCharacter = varchar("pain_character", 100).nullable()
    val diagnosis = text("diagnosis").nullable()
    val prognosis = text("prognosis").nullable()
}

object TransactionsTable : IntIdTable("transactions") {
    val date = date("transaction_date")
    val type = varchar("type", 20)
    val category = varchar("category", 100)
    val amount = double("amount")
    val paymentMethod = varchar("payment_method", 50)
    val notes = text("notes").nullable()
    val patientId = reference("patient_id", PatientsTable).nullable()
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
    val cost = double("cost").default(0.0)
    val transactionId = reference("transaction_id", TransactionsTable).nullable()
}