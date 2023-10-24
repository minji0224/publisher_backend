package com.cmj.publisher.product

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping

// 응답받을 관리자 url 작성하기
// 외부 관리자 서비스 통신 객체
@FeignClient(name = "productClient", url="http://192.168.100.155:8082/test")
interface ProductClient {
    @GetMapping("/inventories")
    fun getInventories() : List<InventoryResponse>
}