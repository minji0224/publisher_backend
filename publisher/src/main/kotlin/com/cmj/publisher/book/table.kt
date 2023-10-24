package com.cmj.publisher.book

import com.cmj.publisher.auth.Profiles
import jakarta.annotation.PostConstruct
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.context.annotation.Configuration


object Books : Table("book") {
    val id = long("id").autoIncrement()
    val publisher = varchar("publisher",32)
    val title = varchar("title", 255)
    val author = varchar("author", 512)
    val pubDate = varchar("pubDate", 10)
    val isbn = varchar("isbn", 13)
    val categoryName = varchar("category_name", 255)
    val priceStandard = integer("price_standard")
    val quantity = integer("quantity")
//    val initialQuantity = integer("initial_quantity")
//    val currentQuantity = integer("current_quantity")
    val createdDate  = datetime("created_date")
    override val primaryKey = PrimaryKey(id, name = "pk_book_id")
    val profileId = reference("profile_id", Profiles);
}

object BookFiles : LongIdTable("book_file") {
    val bookId = reference("book_id", Books.id)
    val originalFileName = varchar("original_file_name", 200)
    val uuidFileName = varchar("uuid", 50).uniqueIndex()
    val contentType = varchar("content_type", 100)
}



// 테이블 생성 코드
@Configuration
class BookTableSetup(private val database: Database) {
    @PostConstruct
    fun migrateSchema() {
        // expose 라이버리에서는 모든 SQL 처리는
        // transaction 함수의 statement 람다함수 안에서 처리를 해야함
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Books, BookFiles)
        }
    }
}