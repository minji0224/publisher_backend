package com.cmj.publisher.sales

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service


@Service
class RabbitConsumer {
    private val mapper = jacksonObjectMapper()

    @RabbitListener(queues = ["my-queue"]) // 큐이름 정하기
    fun receive(message: String) {
        println("관리자한테 요청받을 곳: $message")
    }
}