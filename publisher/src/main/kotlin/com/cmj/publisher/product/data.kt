package com.cmj.publisher.product

import com.cmj.publisher.book.BookSales
import com.cmj.publisher.book.Books

data class InventoryResponse (
    val id : Long,
    val publisher: String,
    val title : String,
    val link : String,
    val author : String,
    val pubDate: String,
    val isbn: String,
    val isbn13: String,
    val itemId: Int,
    val categoryId: Int,
    val categoryName: String,
    val priceSales: Int,
    val priceStandard: Int,
    val stockStatus: String,
    val cover : String,
)

// 하루에 한번씩 관리자로부터 레디스로 판매통계 받을 곳
data class BookSalesResponse(
    val id : Long,
    val isbn : String,
    val price : Long,
    val salesQuantity : Int,
    val saleDate : String,
)