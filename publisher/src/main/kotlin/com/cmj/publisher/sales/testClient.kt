package com.cmj.publisher.sales

import com.cmj.publisher.product.BookSalesResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping

@FeignClient(name = "testClient", url="http://192.168.100.155:8082/test")
interface testClient {

    @GetMapping("/sales")
    fun getSalesBooks() : List<BookSalesResponse>
}