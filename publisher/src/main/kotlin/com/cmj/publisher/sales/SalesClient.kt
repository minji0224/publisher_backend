package com.cmj.publisher.sales

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
/*
    페인 클라이언트에서는 기본적으로 설정되는 Bean들이 있기 때문에 별도의 Configuration을 설정하지 않아도 사용하는 데는 문제가 없는데요.
    (해당 내용은 org.springframework.cloud.openfeign.FeignClientsConfiguration 클래스에서 볼 수 있습니다.)
    기본적으로 제공되는 Bean은 Encoder, Decoder, Logger, Contract, Retryer 등이 있으며,
    재요청을 하는 Retryer 같은 경우에는 default 옵션이 Retryer.NEVER_RETRY로 재요청이 비활성화되어 있습니다.
    Retryer 외에도 다른 빈들도 default 설정 값을 변경하고 싶다면 custom configuration을 통해 가능합니다.
*/
@FeignClient(name = "salesClient") // url="http://192.168.100.155:8082/product" 이걸 application.properties / dev에 작성
interface SalesClient {


    // 관리자가 판매내역 레디스로 보낸 값 가져오기
    @GetMapping("/sales")
    fun getSalesBooks() : List<BookSalesMessageResponse>

}