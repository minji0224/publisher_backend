package com.cmj.publisher.sales

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/inventories")
class SalesController(private val salesClient: SalesClient) {

    @GetMapping("/{productId}")
    fun getProductStock(@PathVariable productId : Int) : Int? {
        /*
            inventoryClient 객체는 인터페이스에서
            @FeignClient(name="inventoryClient", url = "http://192.168.100.155:8082/inventories")
            설정한 값으로 보내주는 역할
        */
        println("요청주소: ${salesClient}")
        println("저쪽에 요청보내서 반환받은 값: ${salesClient.fetchProductStocks(productId)}")

        return salesClient.fetchProductStocks(productId)
        // return 요청보낼주소/inventories/{productId}
    }
}