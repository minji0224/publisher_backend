package com.cmj.publisher

import com.cmj.publisher.auth.SignupRequest
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
class PublisherApplication

fun main(args: Array<String>) {
	runApplication<PublisherApplication>(*args)
}
