package com.cmj.publisher.sales

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(name="inventoryClient", url = "http://192.168.100.155:8082/inventories")
// name=현재함수이름 url= 요청을 보낼 쪽의 주소
interface SalesClient {
    @GetMapping("/{productId}")
    // url로 가서 getMapping으로 맵핑됨
    fun fetchProductStocks(@PathVariable productId: Int) : Int?
}