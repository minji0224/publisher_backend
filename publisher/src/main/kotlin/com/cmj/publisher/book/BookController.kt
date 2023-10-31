package com.cmj.publisher.book

import com.cmj.publisher.auth.Auth
import com.cmj.publisher.auth.AuthProfile

import com.cmj.publisher.sales.RabbitProducer
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import org.springframework.core.io.ResourceLoader

@RestController
@RequestMapping("/books")
class BookController(private val rabbitProducer: RabbitProducer, private val resourceLoader: ResourceLoader, ) {
    private val BOOK_FILE_PATH = "file/book";

    @Auth
    @GetMapping
    fun fetch(@RequestAttribute authProfile: AuthProfile) :List<BookResponse> {

        println("북패처")
        val books = transaction {
            Books.select { Books.profileId eq authProfile.id }.map{ r -> BookResponse(
                    r[Books.id], r[Books.publisher], r[Books.title], r[Books.author], r[Books.pubDate], r[Books.isbn],
                    r[Books.categoryName], r[Books.priceStandard].toString(), r[Books.quantity].toString(), r[Books.createdDate].toString(),
            )}
        }
        println(books.size)
        return books
    }


    @Auth
    @GetMapping("/paging")
    fun paging(@RequestParam size: Int, @RequestParam page: Int, @RequestAttribute authProfile: AuthProfile )
    : Page<BookResponse> = transaction (Connection.TRANSACTION_READ_UNCOMMITTED, readOnly = true) {
        val content = Books.select{Books.profileId eq authProfile.id}.orderBy(Books.id to SortOrder.DESC)
            .limit(size, offset = (size * page).toLong()).map {
                r -> BookResponse(
            r[Books.id], r[Books.publisher], r[Books.title], r[Books.author], r[Books.pubDate], r[Books.isbn],
            r[Books.categoryName], r[Books.priceStandard].toString(), r[Books.quantity].toString(), r[Books.createdDate].toString(),
                )
        }
        val totalCount = Books.select{Books.profileId eq authProfile.id}.count()
        return@transaction PageImpl(content, PageRequest.of(page, size), totalCount)
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

            // 큐로 줄 객체
            val bookCreateMessage = BookCreateMessage(
                id = insertedBook[Books.id],
                publisher = bookWithFileCreateRequest.publisher,
                title = bookWithFileCreateRequest.title,
                author = bookWithFileCreateRequest.author,
                pubDate = bookWithFileCreateRequest.pubDate,
                isbn = bookWithFileCreateRequest.isbn,
                categoryName = bookWithFileCreateRequest.categoryName,
                priceStandard = bookWithFileCreateRequest.priceStandard,
                quantity = bookWithFileCreateRequest.quantity,
                imageUuidName = uuidFileName
            )
            // 큐로 보내기
            rabbitProducer.sendCreateBook(bookCreateMessage)


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

        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }


    @Auth
    @GetMapping("/file/{bookId}/{uuidFilename}")
    fun downloadFile(@PathVariable bookId: Long, @PathVariable uuidFilename: String,
                     @RequestAttribute authProfile: AuthProfile) : ResponseEntity<Any> {
        println(authProfile)

        val bookId = transaction {
            (Books innerJoin BookFiles).select { (Books.id eq bookId) and (BookFiles.uuidFileName eq uuidFilename) }
                .map { r ->
                    BookFileResponse (
                        id = r[BookFiles.id].value,
                        bookId = r[BookFiles.bookId],
                        uuidFileName = r[BookFiles.uuidFileName],
                        originalFileName = r[BookFiles.originalFileName],
                        contentType = r[BookFiles.contentType]
                    )
                }.firstOrNull()
        }
        bookId?.let { println(it.bookId) }

        if(bookId == null) {
            return ResponseEntity.status(404).build()
        }

        val filePath = Paths.get("$BOOK_FILE_PATH/${bookId.uuidFileName}").toFile()

        if(!filePath.exists()) {
            return ResponseEntity.status(404).build()
        }

        val mimeType = Files.probeContentType(filePath.toPath())
        val mediaType = MediaType.parseMediaType(mimeType)
        val resourceLoader = resourceLoader.getResource("file:$filePath")

        return ResponseEntity.ok().contentType(mediaType).body(resourceLoader)

    }


    @Auth
    @PostMapping("/paging/search") // 빈값 체크해야됨!!!!!
    fun searchPaging(@RequestBody searchRequest: SearchRequest,
                   @RequestAttribute authProfile: AuthProfile): Page<BookResponse>
            = transaction(Connection.TRANSACTION_READ_UNCOMMITTED, readOnly = true) {

        println(searchRequest)
//        val userBooks = Books.select{Books.profileId eq authProfile.id}.map{it[Books.id]}
        val userBooks = Books.profileId eq authProfile.id
        val titleQuery = Books.title like "%${searchRequest.keyword}"
        val authorQuery = Books.author like "%${searchRequest.keyword}"
        val isbnQuery= Books.isbn like "%${searchRequest.keyword}"
        val dateToday = Books.createdDate.date() eq LocalDate.now()
        val date6Month = Books.createdDate.date() greaterEq LocalDate.now().minusMonths(6)
        val date1Year = Books.createdDate.date() greaterEq LocalDate.now().minusYears(1)


        val query = when {

            searchRequest.keyword != null && searchRequest.option != null  && searchRequest.date != null -> {

                when(searchRequest.option) {
                    "title" -> {
                        when(searchRequest.date) {
                            "today" -> Books.select { (titleQuery) and (dateToday) and(userBooks) }
                            "sixMonth" -> Books.select { (titleQuery) and (date6Month) and(userBooks) }
                            "oneYear" -> Books.select { (titleQuery) and (date1Year) and(userBooks) }
                            "all" -> Books.select { (titleQuery) and(userBooks)  }
                            else -> error("잘못된 날짜값: ")
                        }
                    }
                    "author"-> {
                        when(searchRequest.date) {
                            "today" -> Books.select { (authorQuery) and (dateToday) and(userBooks)  }
                            "sixMonth" -> Books.select { (authorQuery) and (date6Month) and(userBooks) }
                            "oneYear" -> Books.select { (authorQuery) and (date1Year) and(userBooks) }
                            "all" -> Books.select { (authorQuery) and(userBooks) }
                            else -> error("잘못된 날짜값: ")
                        }
                    }
                    "isbn" -> {
                        when(searchRequest.date) {
                            "today" -> Books.select { (isbnQuery) and (dateToday) }
                            "sixMonth" -> Books.select { (isbnQuery) and (date6Month)}
                            "oneYear" -> Books.select { (isbnQuery) and (date1Year)}
                            "all" -> Books.select { (isbnQuery) and(userBooks) }
                            else -> error("잘못된 날짜값: ")
                        }
                    }
                    else -> error("잘못된 옵션값 : Invalid option")
                }
            }

            searchRequest.keyword != null && searchRequest.date != null -> {
                when(searchRequest.date) {
                    "today" -> Books.select { ((titleQuery) or (authorQuery) or (isbnQuery)) and (dateToday) and(userBooks) }
                    "sixMonth" -> Books.select { ((titleQuery) or (authorQuery) or (isbnQuery)) and (date6Month) and(userBooks) }
                    "oneYear" -> Books.select { ((titleQuery) or (authorQuery) or (isbnQuery)) and (date1Year) and(userBooks)  }
                    "all" -> Books.select { (titleQuery) or (authorQuery) or (isbnQuery) and(userBooks)  }
                    else -> error("잘못된 날짜값: ")
                }
            }

            // keyword 빈값 넣기

            searchRequest.keyword != null -> {
                Books.select {(titleQuery) or (authorQuery) or (isbnQuery) and(userBooks) }
            }
            else -> Books.select { userBooks }
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














}