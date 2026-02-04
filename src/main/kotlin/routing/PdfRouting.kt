package com.logikamobile.fisioterapp.routing

import com.logikamobile.fisioterapp.data.PatientsTable
import com.logikamobile.fisioterapp.data.SessionsTable
import com.logikamobile.fisioterapp.services.PdfService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configurePdfRoutes() {
    val pdfService = PdfService()

    routing {
        get("/sessions/{id}/pdf") {
            val sessionId = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "ID inválido")
            try {
                val pdfData = transaction {
                    (SessionsTable innerJoin PatientsTable)
                        .selectAll().where { SessionsTable.id eq sessionId }
                        .map {
                            Triple(
                                it[PatientsTable.name],
                                it[SessionsTable.date].toString(),
                                it[SessionsTable.treatmentPlan]
                            )
                        }.singleOrNull()
                }
                if (pdfData == null) {
                    call.respond(HttpStatusCode.NotFound, "Sesión no encontrada")
                    return@get
                }
                val (patientName, date, plan) = pdfData
                val pdfBytes = pdfService.generateSessionPdf(patientName, date, plan)
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "Plan_$date.pdf").toString()
                )
                call.respondBytes(pdfBytes, ContentType.Application.Pdf)

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error generando PDF: ${e.localizedMessage}")
            }
        }
    }
}