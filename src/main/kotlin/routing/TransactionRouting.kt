package com.logikamobile.fisioterapp.routing

import com.logikamobile.fisioterapp.model.dto.BalanceDTO
import com.logikamobile.fisioterapp.model.dto.TransactionDTO
import com.logikamobile.fisioterapp.data.PatientsTable
import com.logikamobile.fisioterapp.data.TransactionsTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

fun Application.configureTransactionRoutes() {
    routing {
        route("/transactions") {
            get {
                try {
                    val transactions = transaction {
                        (TransactionsTable leftJoin PatientsTable)
                            .selectAll()
                            .orderBy(TransactionsTable.date to SortOrder.DESC)
                            .map { row ->
                                TransactionDTO(
                                    id = row[TransactionsTable.id].value,
                                    date = row[TransactionsTable.date].toString(),
                                    type = row[TransactionsTable.type], // "INGRESO" o "GASTO"
                                    category = row[TransactionsTable.category],
                                    amount = row[TransactionsTable.amount],
                                    paymentMethod = row[TransactionsTable.paymentMethod],
                                    notes = row[TransactionsTable.notes],
                                    patientId = row[TransactionsTable.patientId]?.value,
                                    patientName = row.getOrNull(PatientsTable.name)
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, transactions)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error al listar: ${e.localizedMessage}")
                }
            }

            get("/balance") {
                try {
                    val balance = transaction {
                        val income = TransactionsTable.selectAll().where { TransactionsTable.type eq "INGRESO" }
                            .sumOf { it[TransactionsTable.amount] }

                        val expenses = TransactionsTable.selectAll().where { TransactionsTable.type eq "GASTO" }
                            .sumOf { it[TransactionsTable.amount] }

                        BalanceDTO(
                            totalIncome = income,
                            totalExpenses = expenses,
                            netBalance = income - expenses
                        )
                    }
                    call.respond(HttpStatusCode.OK, balance)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error calculando balance: ${e.localizedMessage}")
                }
            }

            post {
                try {
                    val dto = call.receive<TransactionDTO>()
                    transaction {
                        TransactionsTable.insert {
                            it[date] = LocalDate.parse(dto.date)
                            it[type] = dto.type.uppercase() // Forzamos mayúsculas para estandarizar
                            it[category] = dto.category
                            it[amount] = dto.amount
                            it[paymentMethod] = dto.paymentMethod
                            it[notes] = dto.notes
                            // Vinculamos al paciente solo si viene el ID
                            it[patientId] = dto.patientId?.let { id ->
                                EntityID(id, PatientsTable)
                            }
                        }
                    }
                    call.respond(HttpStatusCode.Created, "Transacción registrada")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error al guardar: ${e.localizedMessage}")
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)
                try {
                    transaction {
                        TransactionsTable.deleteWhere { TransactionsTable.id eq id }
                    }
                    call.respond(HttpStatusCode.OK, "Transacción eliminada")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error al eliminar: ${e.localizedMessage}")
                }
            }
        }

        route("/patients/{id}/transactions") {
            get {
                val idParam = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

                try {
                    val patientTx = transaction {
                        (TransactionsTable innerJoin PatientsTable)
                            .selectAll()
                            .where { TransactionsTable.patientId eq idParam }
                            .orderBy(TransactionsTable.date to SortOrder.DESC)
                            .map { row ->
                                TransactionDTO(
                                    id = row[TransactionsTable.id].value,
                                    date = row[TransactionsTable.date].toString(),
                                    type = row[TransactionsTable.type],
                                    category = row[TransactionsTable.category],
                                    amount = row[TransactionsTable.amount],
                                    paymentMethod = row[TransactionsTable.paymentMethod],
                                    notes = row[TransactionsTable.notes],
                                    patientId = row[TransactionsTable.patientId]?.value,
                                    patientName = row[PatientsTable.name]
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, patientTx)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error: ${e.localizedMessage}")
                }
            }
        }
    }
}