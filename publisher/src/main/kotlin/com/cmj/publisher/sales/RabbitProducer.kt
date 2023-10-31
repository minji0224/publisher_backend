package com.cmj.publisher.sales

import com.cmj.publisher.book.BookCreateMessage

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service


@Service
class RabbitProducer(private val rabbitTemplate: RabbitTemplate) {

    private val mapper = jacksonObjectMapper()

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


