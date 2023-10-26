package com.cmj.publisher.book

import com.cmj.publisher.auth.Auth
import com.cmj.publisher.auth.AuthProfile

import com.cmj.publisher.cummerce.RabbitProducer
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/books")
class BookController(private val rabbitProducer: RabbitProducer) {
    private val BOOK_FILE_PATH = "file/book";

    @Auth
    @GetMapping
    fun fetch(@RequestAttribute authProfile: AuthProfile) :List<BookResponse> {

        val books = transaction {
            Books.select { Books.profileId eq authProfile.id }.map{ r -> BookResponse(
                    r[Books.id], r[Books.publisher], r[Books.title], r[Books.author], r[Books.pubDate], r[Books.isbn],
                    r[Books.categoryName], r[Books.priceStandard].toString(), r[Books.quantity].toString(), r[Books.createdDate].toString(),
            )}
        }
        return books
    }


    @Auth
    @PostMapping("/with-file")
    fun createWithFile(@RequestPart(name = "createRequest") bookWithFileCreateRequest: BookWithFileCreateRequest,
                       @RequestAttribute authProfile: AuthProfile,
                       @RequestPart ("file") file: MultipartFile) :ResponseEntity<BookWithFileResponse> {


        println("확인")
        println(bookWithFileCreateRequest.title)

        val dirPath = Paths.get(BOOK_FILE_PATH)
        if(!Files.exists(dirPath)) {
            Files.createDirectories(dirPath) //폴더 생성
        }

        val fileList = mutableListOf<Map<String, String?>>()


        val uuidFileName = buildString {
            append(UUID.randomUUID().toString())
            append(".")
            append(file.originalFilename!!.split(".").last())
        }

        val filePath = dirPath.resolve(uuidFileName)

        file.inputStream.use {
            Files.copy(it, filePath, StandardCopyOption.REPLACE_EXISTING) // 파일 저장
        }

        // 파일의 메타데이터를 리스트-맵에 임시 저장
        fileList.add(mapOf(
            "uuidFileName" to uuidFileName,
            "contentType" to file.contentType,
            "originalFileName" to file.originalFilename
        ))

        val result = transaction {
            val insertedBook = Books.insert {
                it[title] = bookWithFileCreateRequest.title
                it[publisher] = bookWithFileCreateRequest.publisher
                it[author] = bookWithFileCreateRequest.author
                it[pubDate] = bookWithFileCreateRequest.pubDate
                it[isbn] = bookWithFileCreateRequest.isbn
                it[categoryName] = bookWithFileCreateRequest.categoryName
                it[priceStandard] = bookWithFileCreateRequest.priceStandard.toInt()
                it[quantity] = bookWithFileCreateRequest.quantity.toInt()
                it[createdDate] = LocalDateTime.now()
                it[profileId] = authProfile.id
            }.resultedValues!!.first()

            BookFiles.batchInsert(fileList) {
                this[BookFiles.bookId] = insertedBook[Books.id]
                this[BookFiles.contentType] = it["contentType"] as String
                this[BookFiles.originalFileName] = it["originalFileName"] as String
                this[BookFiles.uuidFileName] = it["uuidFileName"] as String
            }


            val insertedBookFile = BookFiles.select{BookFiles.bookId eq insertedBook[Books.id]}.map { r ->
                BookFileResponse(
                        id = r[BookFiles.id].value,
                        bookId = insertedBook[Books.id],
                        uuidFileName = r[BookFiles.uuidFileName],
                        originalFileName = r[BookFiles.originalFileName],
                        contentType = r[BookFiles.contentType]
                )
            }

            val fileResponse: BookFileResponse = insertedBookFile.first()


            return@transaction BookWithFileResponse(
                    id = insertedBook[Books.id],
                    title = insertedBook[Books.title],
                    author = insertedBook[Books.author],
                    pubDate = insertedBook[Books.pubDate],
                    isbn = insertedBook[Books.isbn],
                    categoryName = insertedBook[Books.categoryName],
                    priceStandard = insertedBook[Books.priceStandard].toString(),
                    quantity = insertedBook[Books.quantity].toString(),
                    createdDate = insertedBook[Books.createdDate].toString(),
                    file = fileResponse
            )
        }

        // 큐로 줄 객체
        val bookCreateMessage = BookCreateMessage(
            publisher = bookWithFileCreateRequest.publisher,
            title = bookWithFileCreateRequest.title,
            author = bookWithFileCreateRequest.author,
            pubDate = bookWithFileCreateRequest.pubDate,
            isbn = bookWithFileCreateRequest.isbn,
            categoryName = bookWithFileCreateRequest.categoryName,
            priceStandard = bookWithFileCreateRequest.priceStandard,
            quantity = bookWithFileCreateRequest.quantity,
        )

        // 큐로 관리자에게 주기
        // 파일도 같이 줘야됨.
        rabbitProducer.sendCreateBook(bookCreateMessage)
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }


    @GetMapping("/paging/search")
    fun searchPaging(@RequestParam size: Int, @RequestParam page: Int,
                     @RequestParam keyword: String?, @RequestParam option: String?) : Page<BookResponse>
    = transaction(Connection.TRANSACTION_READ_UNCOMMITTED, readOnly = true) {


        val query = when {
            keyword != null -> Books.select {
                Books.title like "%${keyword}"
            }
            else -> Books.selectAll()
        }

        val totalCount = query.count()
        println("토탈카운트값: $totalCount")

        val content = query.orderBy(Books.id to SortOrder.DESC).limit(size, offset = (size * page).toLong())
            .map {r ->
                BookResponse(
                    r[Books.id],
                    r[Books.publisher],
                    r[Books.title],
                    r[Books.author],
                    r[Books.pubDate],
                    r[Books.isbn],
                    r[Books.categoryName],
                    r[Books.priceStandard].toString(),
                    r[Books.quantity].toString(),
                    r[Books.createdDate].toString()
                )
            }

        println(size)
        println(page)
        println(keyword)
        println(option)
        PageImpl(content, PageRequest.of(page, size), totalCount)
    }


    data class SearchRequest(
            val keyword: String?,
            val option: String?,
            val createdDate: String?,
            val page: Int,
            val size: Int
    )

    @PostMapping("/test")
    fun searchItem(@RequestBody searchRequest: SearchRequest) : Page<BookResponse>
    = transaction(Connection.TRANSACTION_READ_UNCOMMITTED, readOnly = true) {

        println(searchRequest)

        val query = when {
            searchRequest.keyword != null && searchRequest.option != null -> {
                when(searchRequest.option) {
                    "title" -> Books.select { Books.title like "%${searchRequest.keyword}" }
                    "author"-> Books.select { Books.author like "%${searchRequest.keyword}" }
                    "isbn" -> Books.select { Books.isbn like "%${searchRequest.keyword}" }
                    else -> error("잘못된 옵션값 : Invalid option")
                }
            }
            searchRequest.keyword != null -> {
                Books.select {
                    (Books.title like "%${searchRequest.keyword}") or
                    (Books.author like "%${searchRequest.keyword}") or
                    (Books.isbn like "%${searchRequest.keyword}")
                }
            }
            else -> Books.selectAll()
        }


        val totalCount = query.count()
        println("토탈카운트: $totalCount")

        val content = query.orderBy(Books.id to SortOrder.DESC).limit(searchRequest.size,
                offset = (searchRequest.size * searchRequest.page).toLong())
                .map {r ->
                    BookResponse(
                            r[Books.id],
                            r[Books.publisher],
                            r[Books.title],
                            r[Books.author],
                            r[Books.pubDate],
                            r[Books.isbn],
                            r[Books.categoryName],
                            r[Books.priceStandard].toString(),
                            r[Books.quantity].toString(),
                            r[Books.createdDate].toString()
                    )
                }

        println("셀렉트한 컨텐트: $content")
        PageImpl(content, PageRequest.of(searchRequest.page, searchRequest.size), totalCount)


}








    // 파일다운메서드 만들기

}