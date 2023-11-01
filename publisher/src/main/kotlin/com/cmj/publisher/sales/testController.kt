package com.cmj.publisher.sales

import com.cmj.publisher.product.BookSalesResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/test")
class testController(private val testService: testService) {

    @GetMapping("/sales")
    fun fetchSales(): List<BookSalesResponse> {
        println(testService.getCachedSales())


        return testService.getCachedSales()
        // 가져온 값으로 테이블 업뎃하기?insert?
    }
}