package com.cmj.publisher.sales


data class PieChartResponse(
        val title: String,
        val author: String,
        val bookId: Long,
        val isbn : String,
        val priceSales : Int,
        val totalCount : Int,
        val uuidFilename: String,
)

data class LineChartResponse(
        val saleDate : String,
        val totalCount : Int,
        val totalPrice: Int,
)

data class BookSalesMessageResponse(
        val isbn : String,
        val priceSales : Int,
        val count : Int,
        val saleDate : String,
        // 관리자쪽에서 날짜를 LocalDate값으로 보내줘야 디비안꼬임
)


