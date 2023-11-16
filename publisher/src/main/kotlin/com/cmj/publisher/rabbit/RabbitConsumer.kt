package com.cmj.publisher.rabbit

import com.cmj.publisher.book.Books
import com.cmj.publisher.rabbit.BookActiveMessageRes
import com.cmj.publisher.rabbit.BookStocksMessageRes
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.support.AmqpHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service

// 여러 상대방 서버의 큐를 읽어오려면 레빗컨피그를 만들어서 호스트주소 및 패스워드 설정하기
// 지금은 내 서버에서 다른 포트로만 주고 받고 있음.
// 리슨 할 상대방 서버주소 필요!
@Service
class RabbitConsumer {
    private val mapper = jacksonObjectMapper()

    // 실시간 도서재고 업데이트
    @RabbitListener(queues = ["book-stocks"])
    fun bookStocksReceive(message: String) {
        println("관리자가 큐로 보낸 도서 재고: $message")
        val bookStocks : BookStocksMessageRes = mapper.readValue(message)
        println(bookStocks)
        transaction {
            Books.update ({ Books.id eq bookStocks.id }){
                it[Books.currentQuantity] = bookStocks.stocks.toInt()
            }
        }
    }

    @RabbitListener(queues = ["book-active"])
    fun bookActiveReceive(message: String) {
        println("관리자가 큐로 보낸 도서몰 등록 여부: $message")
        val bookActive : BookActiveMessageRes = mapper.readValue(message)
        println(bookActive)
        transaction {
            Books.update({Books.id eq bookActive.id }) {
                it[Books.isActive] = bookActive.isActive
            }
        }
    }


}

// 동일한 큐에 여러 객체 보낼 때 헤더에 추가해서 구분 하는 법
//    @RabbitListener(queues = ["my-queue"]) // 도서몰 등록 여부 받을 곳 정하기
//    fun bookStocksReceive(message: Message) {
//        println(message)
//        println(message.messageProperties.headers)
//
//        val messageType = message.messageProperties.headers["messageType"]
//        if(messageType == "bookStocks") {
//
//            println("관리자가 큐로 보낸 도서 재고: $message")
//            val bookStocks : BookStocksMessageRes = mapper.readValue(message.body,BookStocksMessageRes::class.java)
//            println(bookStocks)
//        }
//    }