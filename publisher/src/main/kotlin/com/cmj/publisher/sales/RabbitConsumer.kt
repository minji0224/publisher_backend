package com.cmj.publisher.sales

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

import org.springframework.amqp.core.MessageProperties

data class BookActiveMessageRes(
    val id: Long,
    val isActive: Boolean
)

data class BookStocksMessageRes(
    val id: Long,
    val stocks: Long,
)
@Service
class RabbitConsumer {
    private val mapper = jacksonObjectMapper()

    @RabbitListener(queues = ["my-queue"]) // 큐 정하기
    fun bookStocksReceive(message: String) {
        println("관리자가 큐로 보낸 도서 재고: $message")
        val bookStocks :BookStocksMessageRes = mapper.readValue(message)
        println(bookStocks)



    }


//    @RabbitListener(queues = [""]) // 도서몰 등록 여부 받을 곳 정하기
//    fun bookStocksReceive(message: String) {
//        println("관리자가 큐로 보낸 도서몰 등록 여부: $message")
//        val bookActive : BookActiveMessageRes = mapper.readValue(message)
//        println(bookActive)
//        // 제대로 오면 Book테이블의 isActive 칼럼 update하기
//    }

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
}