package com.logikamobile.fisioterapp

import com.logikamobile.fisioterapp.data.configureDatabases
import com.logikamobile.fisioterapp.plugins.configureSerialization
import com.logikamobile.fisioterapp.routing.configureDashboardRoutes
import com.logikamobile.fisioterapp.routing.configurePatientRoutes
import com.logikamobile.fisioterapp.routing.configureSessionRoutes
import com.logikamobile.fisioterapp.routing.configureTransactionRoutes
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureTransactionRoutes()
    configureSessionRoutes()
    configurePatientRoutes()
    configureDashboardRoutes()
}
