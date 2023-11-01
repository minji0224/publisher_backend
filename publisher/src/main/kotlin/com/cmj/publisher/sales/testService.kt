package com.cmj.publisher.sales

import com.cmj.publisher.product.BookSalesResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

// redis는 key-value값으로 저장
@Service
class testService(private val testClient: testClient,
                  private val redisTemplate: RedisTemplate<String, String> ) {

    private val mapper = jacksonObjectMapper()

    @Scheduled(fixedRate = 1000 * 60 * 60)
    fun fetchSales() {
        val result = testClient.getSalesBooks()
        redisTemplate.delete("my-queue")
        redisTemplate.opsForValue().set("my-queue", mapper.writeValueAsString(result))
        println("스케줄로 업뎃된 판매리스폰값: $result")
    }

    fun getCachedSales() : List<BookSalesResponse> {
        val result = redisTemplate.opsForValue().get("my-queue")
        println("스케줄업뎃 이후 큐에서 가져온 값: $result")
        if(result != null) {
            val list: List<BookSalesResponse> = mapper.readValue(result)
            return list
        } else {
            return listOf()
        }
    }


}