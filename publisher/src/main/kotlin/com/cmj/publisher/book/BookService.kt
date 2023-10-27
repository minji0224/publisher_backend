package com.cmj.publisher.book

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service

//@Service
//class BookService {
//
//    fun findPublisher(profileId: Long) : ResultRow? {
//        val publisher = transaction { Books.select(where = (Books.profileId eq profileId )).firstOrNull() }
//        return publisher
//    }
//
//}