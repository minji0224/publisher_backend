package com.cmj.publisher.rabbit

data class BookActiveMessageRes(
        val id: Long,
        val isActive: Boolean
)

data class BookStocksMessageRes(
        val id: Long,
        val stocks: Long,
)

