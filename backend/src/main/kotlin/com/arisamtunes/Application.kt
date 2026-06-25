package com.arisamtunes

import com.arisamtunes.plugins.configureDatabase
import com.arisamtunes.plugins.configureMonitoring
import com.arisamtunes.plugins.configureRouting
import com.arisamtunes.plugins.configureSerialization
import com.arisamtunes.plugins.configureSecurity
import com.arisamtunes.catalog.configureMediaStreaming
import io.ktor.server.application.Application

fun Application.module() {
    configureMonitoring()
    configureSerialization()
    configureMediaStreaming()
    configureDatabase()
    configureSecurity()
    configureRouting()
}
