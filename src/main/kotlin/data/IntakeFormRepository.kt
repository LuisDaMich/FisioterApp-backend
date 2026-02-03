package com.logikamobile.fisioterapp.data

import com.logikamobile.fisioterapp.IntakeFormsTable
import com.logikamobile.fisioterapp.model.IntakeFormDTO
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class IntakeFormRepository {
    suspend fun create(dto: IntakeFormDTO): Int = transaction {
        IntakeFormsTable.insertAndGetId {
            it[userId] = dto.userId
            it[fullName] = dto.fullName
            it[birthDate] = java.time.LocalDate.parse(dto.birthDate)
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
            it[patientGoals] = dto.patientGoals?.let { list -> Json.encodeToString(list) }
            it[weightKg] = dto.weightKg
            it[heightM] = dto.heightM
            it[bmi] = dto.bmi
        }.value
    }
    suspend fun findByUserId(userId: Int): IntakeFormDTO? = transaction {
        val row = IntakeFormsTable.select { IntakeFormsTable.userId eq userId }
            .singleOrNull()
        row?.let { toDTO(it) }
    }
    private fun toDTO(row: ResultRow): IntakeFormDTO {
        return IntakeFormDTO(
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
            patientGoals = row[IntakeFormsTable.patientGoals]?.let { jsonStr ->
                try {
                    Json.decodeFromString<List<String>>(jsonStr)
                } catch (_: Exception) { emptyList() }
            },
            weightKg = row[IntakeFormsTable.weightKg],
            heightM = row[IntakeFormsTable.heightM],
            bmi = row[IntakeFormsTable.bmi]
        )
    }
}