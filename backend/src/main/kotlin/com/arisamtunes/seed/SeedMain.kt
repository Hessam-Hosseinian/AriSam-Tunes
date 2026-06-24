package com.arisamtunes.seed

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway

fun main() {
    val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://${env("DB_HOST", "localhost")}:${env("DB_PORT", "5432")}/${env("DB_NAME", "arisam_tunes_db")}"
        username = env("DB_USER", "arisam")
        password = env("DB_PASSWORD", "arisam_dev_password")
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 4
        isAutoCommit = false
    })
    try {
        Flyway.configure().dataSource(dataSource).cleanDisabled(true).load().migrate()
        MusicSeeder(dataSource, env("PUBLIC_BASE_URL", "http://localhost:8080").trimEnd('/')).seed()
    } finally {
        dataSource.close()
    }
}

private fun env(name: String, fallback: String) = System.getenv(name)?.takeIf(String::isNotBlank) ?: fallback
