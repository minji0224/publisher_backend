package com.cmj.publisher.sales

import com.cmj.publisher.auth.Auth
import com.cmj.publisher.auth.AuthProfile
import com.cmj.publisher.book.BookFiles
import com.cmj.publisher.book.BookSales
import com.cmj.publisher.book.Books
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.times
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.DayOfWeek
import java.time.LocalDateTime

@RestController
@RequestMapping("/chart")
class ChartController() {
    @Auth
    @GetMapping("/pieChart") // 오늘기준으로 이번달
    fun getPieChart(@RequestAttribute authProfile: AuthProfile): List<PieChartResponse>  {
        println(authProfile)

        val result = transaction {
            BookSales.innerJoin(Books).leftJoin(BookFiles).slice(
                BookSales.bookId,
                BookSales.isbn,
                BookSales.priceSales,
                BookSales.count.sum().alias("total_count"),
                Books.title,
                Books.author,
                BookFiles.uuidFileName
            )
                .select { (BookSales.saleDate greaterEq LocalDateTime.now().withDayOfMonth(1).toLocalDate()) and
                        (BookSales.saleDate lessEq LocalDateTime.now().toLocalDate()) and
                        (Books.profileId eq authProfile.id)}
                .groupBy(BookSales.bookId, BookSales.isbn, BookSales.priceSales, Books.title, Books.author, BookFiles.uuidFileName)
                    .orderBy(BookSales.count.sum(), SortOrder.DESC)
                        .limit(5).map{ r ->
                            PieChartResponse(
                                        title = r[Books.title],
                                        author = r[Books.author],
                                        bookId = r[BookSales.bookId],
                                        isbn = r[BookSales.isbn],
                                        priceSales = r[BookSales.priceSales],
                                        totalCount = r[BookSales.count.sum()]?: 0,
                                        uuidFilename = r[BookFiles.uuidFileName]?: ""
                                )
                            }
        }

        println(result)
        return result
    }

    @Auth
    @GetMapping("/lineChart") // 오늘기준으로 어제날짜부터 7일
    fun getLineChart(@RequestAttribute authProfile: AuthProfile): List<LineChartResponse> {
        println("라인차트")
        println(authProfile)

        val result = transaction {
            BookSales.innerJoin(Books).slice(BookSales.saleDate,
                (BookSales.priceSales * BookSales.count).sum().alias("total_price"),
                BookSales.count.sum().alias("total_count"),)
                .select { BookSales.saleDate greaterEq LocalDateTime.now().minusDays(7).toLocalDate() and
                        (BookSales.saleDate lessEq LocalDateTime.now().minusDays(1).toLocalDate()) and
                        (Books.profileId eq authProfile.id)}
                .groupBy(BookSales.saleDate)
                .orderBy(BookSales.saleDate, SortOrder.ASC)
                .limit(7)
                .map { r ->
                    LineChartResponse(
                        saleDate = r[BookSales.saleDate],
                        totalCount = r[BookSales.count.sum()]?: 0,
                        totalPrice = r[(BookSales.priceSales * BookSales.count).sum()]?: 0
                    )
                }
        }

        println(result)
        return result

    }

}