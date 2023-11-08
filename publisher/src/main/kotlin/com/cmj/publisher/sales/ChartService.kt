package com.cmj.publisher.sales

import com.cmj.publisher.book.BookSales
import com.cmj.publisher.book.Books
import com.cmj.publisher.product.BookSalesResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

// redis는 key-value값으로 저장
@Service
class ChartService(private val ChartClient: ChartClient,
                   private val redisTemplate: RedisTemplate<String, String> ) {

    private val mapper = jacksonObjectMapper()

    // 관리자쪽에서 날짜를 LocalDate값으로 보내줘야 디비안꼬임
    // 하루에 한번씩 돌게 만들기
    // 업데이트될때는 count합산되게 하기?
    @Scheduled(fixedRate = 1000 * 60 * 60)
    fun fetchSales() {

        redisTemplate.delete("my-queue")
        redisTemplate.opsForValue().set("my-queue", mapper.writeValueAsString(ChartClient.getSalesBooks()))

        val result = redisTemplate.opsForValue().get("my-queue")
        println("스케줄업뎃 이후 큐에서 가져온 값: $result")
        // 1. 레디스 큐에 있는 값이 존재하면 디비 작업 시작
        if (result != null) {

            val list: List<BookSalesResponse> = mapper.readValue(result)

            transaction {
                try {

                    list.forEach{ i ->
                        val isbookId = Books.select(Books.isbn eq i.isbn).singleOrNull()?.get(Books.id)
                        // 2. Book테이블에 해당 book.id가 존재하면
                        println("-------------------------")
                        println(isbookId)
                        println(i.saleDate)
                        println("-------------------------")

                        if(isbookId != null) {
                            val test = BookSales.select {
                                (BookSales.bookId eq isbookId)}.singleOrNull()


                            val test22 = BookSales.select {
                                (BookSales.saleDate eq i.saleDate)}.singleOrNull()?.get(BookSales.saleDate)
                            println("!!!!!!!!!!!!!!!")
                            println(test)
                            println(test22)


                            val differentSaleDate = BookSales.select {
                                (BookSales.bookId eq isbookId) and ((BookSales.saleDate) neq (i.saleDate))
                            }.singleOrNull()
                            println("판매날짜 다름: $differentSaleDate")

                            val sameSaleDate =  BookSales.select {
                                (BookSales.bookId eq isbookId) and (BookSales.saleDate eq i.saleDate)
                            }.singleOrNull()
                            println("판매날짜 같음: $sameSaleDate")

                            if(differentSaleDate != null ) {
                                println("판매날짜 달라서 디비 인설트")
                                BookSales.insert {
                                    it[bookId] = isbookId
                                    it[isbn] = i.isbn
                                    it[priceSales] = i.priceSales
                                    it[count] = i.count
                                    it[saleDate] = i.saleDate
                                }
                            } else if(sameSaleDate != null) {
                                    println("판매날짜 같아서 디비 업뎃")
                                    BookSales.update({(BookSales.bookId eq isbookId) and (BookSales.saleDate eq i.saleDate)}) {
                                        it[priceSales] = i.priceSales
                                        it[count] = i.count
                                        it[saleDate] = i.saleDate
                                    }
                            } else {
                                println("데이터가 아예 없어서 인설트")
                                BookSales.insert {
                                    it[bookId] = isbookId
                                    it[isbn] = i.isbn
                                    it[priceSales] = i.priceSales
                                    it[count] = i.count
                                    it[saleDate] = i.saleDate
                                }
                            }
                        }
                    }

                } catch (e: Exception) {
                    rollback()
                    e.printStackTrace()
                    println("판매통계 디비업뎃에러")
                }
            }
        } else {
            println("판매통계 레디스큐값 없음")
        }

    }
}









