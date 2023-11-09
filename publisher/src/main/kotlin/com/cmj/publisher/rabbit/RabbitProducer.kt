package com.cmj.publisher.rabbit

import com.cmj.publisher.book.BookCreateMessage

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service


@Service
class RabbitProducer(private val rabbitTemplate: RabbitTemplate) {

    private val mapper = jacksonObjectMapper()

    // 큐로 관리자에게 신간 도서 내보내기
    fun sendCreateBook(bookCreateMessage: BookCreateMessage) {
        rabbitTemplate.convertAndSend("create-book", mapper.writeValueAsString(bookCreateMessage))
    }

}


//@RestController
//@RequestMapping("/rabbit")
//class RabbitController(private val rabbitProducer: RabbitProducer) {
////    @PostMapping("/message")
////    fun sendMessage(@RequestBody bookCreateRequest: BookCreateRequest) {
////        println(bookCreateRequest)
////        rabbitProducer.sendCreateBook(bookCreateRequest)
////    }
//}


