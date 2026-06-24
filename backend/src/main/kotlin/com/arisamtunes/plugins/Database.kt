package com.arisamtunes.plugins

import com.arisamtunes.config.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import javax.sql.DataSource

object DatabaseProvider {
    lateinit var dataSource: DataSource
        private set

    fun initialize(dataSource: DataSource) { this.dataSource = dataSource }
}

fun Application.configureDatabase() {
    val databaseEnabled = environment.config
        .propertyOrNull("database.enabled")
        ?.getString()
        ?.toBooleanStrictOrNull()
        ?: true
    if (!databaseEnabled) return

    val databaseConfig = AppConfig.from(environment.config).database
    val dataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = databaseConfig.jdbcUrl
            username = databaseConfig.user
            password = databaseConfig.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            minimumIdle = 2
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        },
    )

    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate()

    Database.connect(dataSource)
    DatabaseProvider.initialize(dataSource)
    monitor.subscribe(ApplicationStopped) { dataSource.close() }
}
