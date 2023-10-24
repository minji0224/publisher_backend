package com.cmj.publisher.product

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/test")
class ProductController(private val productService: ProductService) {
    @GetMapping("/inventories")
    fun fetchInventories() : List<InventoryResponse> {
        println(productService.getCachedInventories())
        return productService.getCachedInventories()
    }
}