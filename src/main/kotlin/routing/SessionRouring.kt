package com.logikamobile.fisioterapp.routing

import com.logikamobile.fisioterapp.model.request.CreateSessionRequest
import com.logikamobile.fisioterapp.services.PackageService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.sessionRouting() {
    routing {
        val packageService = PackageService()
        post("/sessions/consume-package") {
            val request = call.receive<CreateSessionRequest>() // DTO con sessionData y transactionId
            try {
                packageService.registerSessionWithPackage(
                    patientId = request.patientId,
                    transactionId = request.transactionId,
                    sessionData = request.sessionData
                )
                call.respond(HttpStatusCode.Created, "Sesión de paquete registrada")
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.Conflict, e.message ?: "Error de paquete")
            }
        }
    }
}