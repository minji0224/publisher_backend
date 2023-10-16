package com.cmj.publisher.configuration

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DatabaseConfiguration (val dataSource: DataSource) {
    // dataSourcerorcp : db와의 연결을 유지 및 관리
    @Bean
    fun databaseConfig() : DatabaseConfig {
        return DatabaseConfig{useNestedTransactions = true}
    }

    @Bean
    fun database() : Database {
        return Database.connect(dataSource)
    }
}