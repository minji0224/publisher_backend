package com.cmj.publisher.book

import com.cmj.publisher.auth.Profiles
import com.cmj.publisher.book.Books.autoIncrement
import org.jetbrains.exposed.sql.javatime.datetime

// 클라이언트에서 들어오는 검색객체
data class SearchRequest(
    val keyword: String?,
    val option: String?,
    val date: String?,
    val page: Int,
    val size: Int
)

// 클라이언트에 줄 응답객체
data class BookResponse(
    val id : Long,
    val publisher : String,
    val title : String,
    val author : String,
    val pubDate : String,
    val isbn : String,
    val categoryName : String,
    val priceStandard : String,
    val quantity : String,
    val createdDate : String
)

// 큐로 관리자에게 보낼 객체
data class BookCreateMessage(
        val id : Long,
        val title : String,
        val publisher : String,
        val author : String,
        val pubDate : String,
        val isbn : String,
        val categoryName : String,
        val priceStandard : String,
        val quantity : String,
        val imageUuidName: String,
)


// 클라이언트에서 신간도서등록 요청 객체
data class BookWithFileCreateRequest(
        val publisher : String,
        val categoryName : String,
        val title : String,
        val author : String,
        val pubDate : String,
        val priceStandard : String,
        val quantity : String,
        val isbn : String,
)


// 클라이언트에 보내줄 응답객체
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
    val file: BookFileResponse
)

data class BookFileResponse(
    val id : Long,
    val bookId : Long,
    var uuidFileName : String,
    val originalFileName : String,
    val contentType: String,
)



// 북 생성 요청 데이터를 검증하는 메서드 만들기