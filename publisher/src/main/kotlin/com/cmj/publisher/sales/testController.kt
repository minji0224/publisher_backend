package com.cmj.publisher.sales

import com.cmj.publisher.auth.Auth
import com.cmj.publisher.auth.AuthProfile
import com.cmj.publisher.book.BookSales
import com.cmj.publisher.book.Books
import com.cmj.publisher.product.BookIdAndDate
import com.cmj.publisher.product.BookSalesResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

//@RestController
//@RequestMapping("/test")
//class testController(private val testService: testService) {
//
//
//    @GetMapping("/sales")
//    fun fetchSales(): List<PieChartResponse?> {
//        println("판매통계디비업뎃: ${testService.getCachedSales()}")
//
//        val result = transaction {
//            try {
//
//                for ( i in testService.getCachedSales()) {
//
//                    val query = (Books innerJoin BookSales).slice(Books.id, BookSales.saleDate).select {
//                            (Books.isbn eq i.isbn)}.map { r -> BookIdAndDate(
//                                    bookId = r[Books.id],
//                                    bookDated = r[BookSales.saleDate]
//                            )
//                    }.singleOrNull()
//
//
//                    if (query != null) {
//                        if(query.bookDated === i.saleDate) {
//
//                            val updatedQuery = BookSales.update({BookSales.saleDate eq query.bookDated}) {
//                                it[priceSales] = i.priceSales
//                                it[count] = i.count
//                                it[saleDate] = i.saleDate
//                            }
//
//                        } else {
//
//                            val insertedQuery = BookSales.insert {
//                                it[bookId] = query.bookId
//                                it[isbn] = i.isbn
//                                it[priceSales] = i.priceSales
//                                it[count] = i.count
//                                it[saleDate] = i.saleDate
//                            }
//                        }
//                    } else {
//
//                        val insertedQuery = BookSales.insert {
//                            it[bookId] = Books.slice(Books.id).select {
//                                Books.isbn eq i.isbn
//                            }.single()[Books.id]
//                            it[isbn] = i.isbn
//                            it[priceSales] = i.priceSales
//                            it[count] = i.count
//                            it[saleDate] = i.saleDate
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                rollback()
//                e.printStackTrace()
//                println("판매통계에러")
//            }
//
//            (BookSales innerJoin Books).selectAll()
//                .having { BookSales.saleDate greaterEq  LocalDateTime.now().minusMonths(1) }
//                .groupBy( BookSales.bookId )
//                    .orderBy(BookSales.count.sum(), SortOrder.DESC).limit(5).map{
//                        r ->
//                        PieChartResponse(
//                            title = r[Books.title],
//                            author = r[Books.author],
//                            bookId = r[BookSales.bookId],
//                            isbn = r[BookSales.isbn],
//                            priceSales = r[BookSales.priceSales],
//                            count = r[BookSales.count],
//                            saleDate = r[BookSales.saleDate]
//                        )
//
//                    }
//        }
//        println(result)
//        return result
//
//    }



    // 만약 패치로 받아서 바로 디비에 저장할거면 이 방식으로 하기
//    @GetMapping("/pieChart")
//    fun getPieChart() : List<PieChartResponse> {
//
//        val result = transaction {
//            (BookSales innerJoin Books).select { BookSales.saleDate greaterEq  LocalDateTime.now().minusMonths(1) }
//                    .orderBy(BookSales.price, SortOrder.DESC).limit(5).map{
//                        r -> PieChartResponse(
//                                id = r[BookSales.id],
//                                title = r[Books.title],
//                                author = r[Books.author],
//                                bookId = r[BookSales.bookId],
//                                isbn = r[BookSales.isbn],
//                                price = r[BookSales.price],
//                                salesQuantity = r[BookSales.salesQuantity],
//                                saleDate = r[BookSales.saleDate]
//                        )
//                    }
//        }
//
//        println(result)
//        return result
//    }
//}