package com.cmj.publisher.book

import com.cmj.publisher.auth.Auth
import com.cmj.publisher.auth.AuthProfile

import com.cmj.publisher.rabbit.RabbitProducer
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
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
    @GetMapping("/paging")
    fun paging(@RequestParam size: Int, @RequestParam page: Int, @RequestAttribute authProfile: AuthProfile )
    : Page<BookResponse> = transaction (Connection.TRANSACTION_READ_UNCOMMITTED, readOnly = true) {
        val content = Books.select{Books.profileId eq authProfile.id}.orderBy(Books.id to SortOrder.DESC)
            .limit(size, offset = (size * page).toLong()).map {
                r -> BookResponse(
            r[Books.id], r[Books.publisher], r[Books.title], r[Books.author], r[Books.pubDate], r[Books.isbn],
            r[Books.categoryName], r[Books.priceStandard].toString(), r[Books.currentQuantity].toString(), r[Books.createdDate].toLocalDate().toString(),
                    r[Books.isActive]
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
                it[initialQuantity] = bookWithFileCreateRequest.quantity.toInt()
                it[currentQuantity] = bookWithFileCreateRequest.quantity.toInt()
                it[createdDate] = LocalDateTime.now()
                it[profileId] = authProfile.id
                it[isActive] = false
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
                    quantity = insertedBook[Books.initialQuantity].toString(),
                    createdDate = insertedBook[Books.createdDate].toString(),
                    file = fileResponse
            )
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }




    @GetMapping("/file/{uuidFilename}")
    fun downloadFile(@PathVariable uuidFilename: String) : ResponseEntity<Any> {


        val filePath = Paths.get("$BOOK_FILE_PATH/$uuidFilename").toFile()

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
        println("페이징")
        val userBooks = Books.profileId eq authProfile.id
        val keyword = "%${searchRequest.keyword}%"

        val dateToday = Books.createdDate.date() eq LocalDate.now()
        val date6Month = Books.createdDate.date() greaterEq LocalDate.now().minusMonths(6)
        val date1Year = Books.createdDate.date() greaterEq LocalDate.now().minusYears(1)


        val query = Books.select {
            userBooks and when{
                !searchRequest.keyword.isNullOrEmpty() -> {
                    when(searchRequest.option) {
                        "title" -> Books.title like keyword
                        "author" -> Books.author like keyword
                        "isbn" -> Books.isbn like keyword
                        else -> (Books.title like keyword) or (Books.author like keyword) or (Books.isbn like keyword)
                    }
                }
                // keyword가 nullOrEmpty 경우
                else -> Op.TRUE
            } and when(searchRequest.date) {
                "today" -> dateToday
                "sixMonth" -> date6Month
                "oneYear" -> date1Year
                else -> Op.TRUE
            }
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
                    r[Books.currentQuantity].toString(),
                    r[Books.createdDate].toLocalDate().toString(),
                    r[Books.isActive]
                )
            }

        println("셀렉트한 컨텐트: $content")
        PageImpl(content, PageRequest.of(searchRequest.page, searchRequest.size), totalCount)

    }






}