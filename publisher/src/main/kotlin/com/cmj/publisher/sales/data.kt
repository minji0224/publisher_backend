package com.cmj.publisher.sales

data class BookActiveMessageRes(
        val id: Long,
        val isActive: Boolean
)

data class BookStocksMessageRes(
        val id: Long,
        val stocks: Long,
)

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
