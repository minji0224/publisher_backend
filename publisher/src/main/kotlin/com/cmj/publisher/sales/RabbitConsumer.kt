package com.cmj.publisher.sales

import com.cmj.publisher.book.Books
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

import org.springframework.amqp.core.MessageProperties


@Service
class RabbitConsumer {
    private val mapper = jacksonObjectMapper()

    // 실시간 도서재고 업데이트
    @RabbitListener(queues = ["my-queue"]) // 큐 정하기
    fun bookStocksReceive(message: String) {
        println("관리자가 큐로 보낸 도서 재고: $message")
        val bookStocks :BookStocksMessageRes = mapper.readValue(message)
        println(bookStocks)

        transaction {
            Books.update ({ Books.id eq bookStocks.id }){
                it[Books.currentQuantity] = bookStocks.stocks.toInt()
            }
        }
    }


    @RabbitListener(queues = ["my-queue"]) // 도서몰 등록 여부 받을 곳 정하기
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

    @RabbitListener(queues = ["create-order"])
    fun test(message: String) {
        println("레빗테스트: $message")
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