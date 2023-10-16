package com.cmj.publisher

import com.cmj.publisher.auth.SignupRequest
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PublisherApplication

fun main(args: Array<String>) {
	runApplication<PublisherApplication>(*args)
}
