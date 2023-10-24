package com.cmj.publisher.auth

import com.cmj.publisher.book.Books
import jakarta.annotation.PostConstruct
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.context.annotation.Configuration

object Identities : LongIdTable("identity") {
    val secret = varchar("secret", 200)
    val publisherName = varchar("publisherName", length = 100)
}

object Profiles : LongIdTable("profile") {
    val publisherName = varchar("publisherName", length = 100)
    val email = varchar("email", 200)
    val businessRegistrationNumber = varchar("businessRegistrationNumber", 100)
    val identityId = reference("identity_id", Identities)
//    val bookId = reference("book_id", Books)
}

@Configuration
class AuthTableSetup(private val database: Database) {
    @PostConstruct // 클래스 초기화 단계 (이 함수는 빈이 초기화된 후 한 번 실행됨)
    fun migrateSchema() {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Identities, Profiles)
            // 테이블이 db에 존재하지 않는 경우 해당 테이블을 생성
            // 스키마 내의 누락된 테이블 및 열을 자동으로 생성하는 스키마 유틸리티
        }
    }
}