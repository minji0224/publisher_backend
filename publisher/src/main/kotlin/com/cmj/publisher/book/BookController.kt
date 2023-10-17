package com.cmj.publisher.book

import com.cmj.publisher.auth.AuthProfile
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/books")
class BookController {

    // 어스 붙이기
    @PostMapping
    fun create(@RequestBody bookCreateRequest: BookCreateRequest, @RequestAttribute authProfile: AuthProfile)
        : ResponseEntity<Map<String, Any?>> {

        println(
            "${bookCreateRequest.title}, ${bookCreateRequest.author},${bookCreateRequest.pubDate}," +
                "${bookCreateRequest.isbn}, ${bookCreateRequest.categoryName}, ${bookCreateRequest.priceStandard}," +
                bookCreateRequest.quantity
        )

        // 널체크하기

        val (result, response) = transaction {
            val result = Books.insert {
                it[title] = bookCreateRequest.title
                it[author] = bookCreateRequest.author
                it[pubDate] = bookCreateRequest.pubDate
                it[isbn] = bookCreateRequest.isbn
                it[categoryName] = bookCreateRequest.categoryName
                it[priceStandard] = bookCreateRequest.priceStandard.toInt()
                it[quantity] = bookCreateRequest.quantity.toInt()
                it[createdDate] = LocalDateTime.now()
                it[profileId] = authProfile.id
            }.resultedValues ?: return@transaction Pair(false, null)

            val record = result.first()

            return@transaction Pair(true, BookResponse(
                record[Books.id],
                record[Books.title],
                record[Books.author],
                record[Books.pubDate],
                record[Books.isbn],
                record[Books.categoryName],
                record[Books.priceStandard].toString(),
                record[Books.quantity].toString(),
                record[Books.createdDate].toString(),
            ))
        }

        if(result) {
            return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("data" to response))
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("data" to response, "error" to "conflict"))
    }



}