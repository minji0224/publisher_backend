package com.cmj.publisher.book

import com.cmj.publisher.auth.Profiles
import com.cmj.publisher.book.Books.autoIncrement
import org.jetbrains.exposed.sql.javatime.datetime

data class BookResponse(
    val id : Long,
    val title : String,
    val author : String,
    val pubDate : String,
    val isbn : String,
    val categoryName : String,
    val priceStandard : String,
    val quantity : String,
    val createdDate : String,
)

data class BookWithFileResponse(
    val id : Long,
    val title : String,
    val author : String,
    val pubDate : String,
    val isbn : String,
    val categoryName : String,
    val priceStandard : String,
    val quantity : String,
    val createdDate : String,
    val files : List<BookFileResponse>
)

data class BookFileResponse(
    val id : Long,
    val postId : Long,
    var uuidFileName : String,
    val originalFileName : String,
    val contentType: String,
)

data class BookCreateRequest(
    val title : String,
    val author : String,
    val pubDate : String,
    val isbn : String,
    val categoryName : String,
    val priceStandard : String,
    val quantity : String
)

// 북 생성 요청 데이터를 검증하는 메서드 만들기