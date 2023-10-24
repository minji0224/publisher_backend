package com.cmj.publisher.product

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.scheduling.annotation.Scheduled

@Service
class ProductService(
    private val productClient: ProductClient,
    private val redisTemplate: RedisTemplate<String, String>) {

    private val mapper = jacksonObjectMapper()

    // 스케줄 정해서 스케줄마다 관리자쪽에 요청해서 업뎃되는 함수
    // 실시간이 아니라서 스케줄마다만 관리자쪽에 요청되는 것. 큐에서 가져오는 것
    @Scheduled(fixedRate = 1000 * 60 * 60)
    fun fetchInventories() {
        // 1. 관리자쪽에 요청 보내기
        val result = productClient.getInventories()
        // 2. 응답받는 큐의 기존 캐시데이터 먼저 삭제하고
        redisTemplate.delete("my-queue") // 큐이름 지정하기
        // 2. 스케줄로 업뎃된 새로운 캐시데이터 저장
        redisTemplate.opsForValue().set("my-queue", mapper.writeValueAsString(result))
        println("관리자쪽에 요청보내서 캐시 업뎃 된 값: $result")
    }

    fun getCachedInventories() : List<InventoryResponse> {
        val result = redisTemplate.opsForValue().get("my-queue")
        println("스케줄함수 이후 큐에서 업뎃된 값: $result")
        if (result != null) {
            // 1. JSON문자열을 객체형식으로 변환
            val list : List<InventoryResponse> = mapper.readValue(result)
            return list
        } else {
            // 빈배열 반환
            return listOf()
        }
    }

}