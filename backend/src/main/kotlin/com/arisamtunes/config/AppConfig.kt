package com.arisamtunes.config

import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val database: DatabaseConfig,
    val publicBaseUrl: String,
    val jwt: JwtConfig,
) {
    companion object {
        fun from(config: ApplicationConfig): AppConfig = AppConfig(
            database = DatabaseConfig(
                host = config.property("database.host").getString(),
                port = config.property("database.port").getString().toInt(),
                name = config.property("database.name").getString(),
                user = config.property("database.user").getString(),
                password = config.property("database.password").getString(),
            ),
            publicBaseUrl = config.property("app.publicBaseUrl").getString().trimEnd('/'),
            jwt = JwtConfig(
                secret = config.property("jwt.secret").getString(),
                issuer = config.property("jwt.issuer").getString(),
                audience = config.property("jwt.audience").getString(),
                realm = config.property("jwt.realm").getString(),
            ),
        )
    }
}

data class JwtConfig(val secret: String, val issuer: String, val audience: String, val realm: String)

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String,
) {
    val jdbcUrl: String = "jdbc:postgresql://$host:$port/$name"
}
