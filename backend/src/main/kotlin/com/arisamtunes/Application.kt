package com.arisamtunes

import com.arisamtunes.plugins.configureDatabase
import com.arisamtunes.plugins.configureMonitoring
import com.arisamtunes.plugins.configureRouting
import com.arisamtunes.plugins.configureSerialization
import io.ktor.server.application.Application

fun Application.module() {
    configureMonitoring()
    configureSerialization()
    configureDatabase()
    configureRouting()
}
