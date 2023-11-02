package com.cmj.publisher.sales

import com.cmj.publisher.book.BookSales
import com.cmj.publisher.book.Books
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/test")
class testControllerCopy() {
    @GetMapping("/pieChart")

    fun getPieChart(): List<PieChartResponse>  {


        val result = transaction {
            BookSales.select { BookSales.saleDate greaterEq  LocalDateTime.now().minusMonths(1).toLocalDate().toString() }
                .groupBy(BookSales.bookId)
                    .orderBy(BookSales.count.sum(), SortOrder.DESC)
                        .limit(5).map{
                                r -> PieChartResponse(
                                        title = r[Books.title],
                                        author = r[Books.author],
                                        bookId = r[BookSales.bookId],
                                        isbn = r[BookSales.isbn],
                                        priceSales = r[BookSales.priceSales],
                                        totalCount = r[BookSales.count],
                                        saleDate = r[BookSales.saleDate]
                                )
                            }
        }

        return result
    }
}