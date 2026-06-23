package com.arisamtunes

import com.arisamtunes.plugins.configureMonitoring
import com.arisamtunes.plugins.configureRouting
import io.ktor.server.application.Application

fun Application.module() {
    configureMonitoring()
    configureRouting()
}
