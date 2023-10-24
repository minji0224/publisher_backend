package com.cmj.publisher.book

import com.cmj.publisher.auth.Auth
import com.cmj.publisher.auth.AuthProfile
import com.cmj.publisher.cummerce.RabbitProducer
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
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
                it[publisher] = bookCreateRequest.publisher
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
                    record[Books.publisher],
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
//                    file = insertedBookFile
                    file = fileResponse
            )
        }

        // 큐로 줄 객체
        val bookCreateRequest = BookCreateRequest(
            publisher = bookWithFileCreateRequest.publisher,
            title = bookWithFileCreateRequest.title,
            author = bookWithFileCreateRequest.author,
            pubDate = bookWithFileCreateRequest.pubDate,
            isbn = bookWithFileCreateRequest.isbn,
            categoryName = bookWithFileCreateRequest.categoryName,
            priceStandard = bookWithFileCreateRequest.priceStandard,
            quantity = bookWithFileCreateRequest.quantity
        )

        // 큐로 관리자에게 주기
        // 파일도 같이 줘야됨.
        rabbitProducer.sendCreateBook(bookCreateRequest)
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    // 파일다운메서드 만들기
    // 페이지만들기
    // 서치만들기

}