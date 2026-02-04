package com.logikamobile.fisioterapp.routing

import com.logikamobile.fisioterapp.services.DashboardService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureDashboardRoutes() {
    val dashboardService = DashboardService()

    routing {
        route("/dashboard") {
            get("/metrics") {
                try {
                    val metrics = dashboardService.getMonthlyMetrics()
                    call.respond(HttpStatusCode.OK, metrics)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error calculando métricas: ${e.localizedMessage}")
                }
            }
        }
    }
}