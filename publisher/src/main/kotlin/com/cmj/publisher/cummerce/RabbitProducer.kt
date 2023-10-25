package com.cmj.publisher.cummerce

import com.cmj.publisher.book.BookCreateMessage

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


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


