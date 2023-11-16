package com.cmj.publisher.auth

import com.cmj.publisher.auth.util.JwtUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Tag(name = "인증 관련 API")
@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @Operation(summary = "회원가입")
    @PostMapping(value = ["/signup"])
    fun signUp(@RequestBody signupRequest: SignupRequest): ResponseEntity<Long> {
        println(signupRequest)
        // 1. 입력값 검증 넣기(널값체트)
        if(signupRequest.publisherName.isNullOrEmpty() ||
                signupRequest.password.isNullOrEmpty() ||
                signupRequest.email.isNullOrEmpty() ||
                signupRequest.businessRegistrationNumber.isNullOrEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        // 2. 해당 출판사명/사업자등록번호 존재하면
        if(Identities.publisherName.equals(signupRequest.publisherName)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        val profileId = authService.createIdentity(signupRequest)
        if(profileId > 0) {
            return ResponseEntity.status(HttpStatus.CREATED).body(profileId)
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(profileId)
        }
    }

    @Operation(summary = "로그인")
    @PostMapping(value = ["/signin"])
    fun signIn(@RequestParam publisherName: String, @RequestParam password: String,
               httpServletResponse: HttpServletResponse,) : ResponseEntity<*> {
        println(publisherName)
        println(password)

        // 쿠키 생성 작업
        val (result, message) = authService.authenticate(publisherName, password)
        if(result) {
            // 1. 쿠키와 헤더를 생성한 후
            val cookie = Cookie("token", message)
            cookie.path = "/"
            cookie.maxAge = (JwtUtil.TOKEN_TIMEOUT / 1000L).toInt() // 만료시간
            cookie.domain = "localhost" // 쿠키를 사용할 수 있는 도메인

            // 2. 응답헤더에 쿠키 추가
            httpServletResponse.addCookie(cookie)
            println(cookie)

            // 3. 이후 웹 첫페이지로 리다이렉트
            return  ResponseEntity.status(HttpStatus.FOUND).location(
                ServletUriComponentsBuilder.fromHttpUrl("http://localhost:5000").build().toUri()
            ).build<Any>()
        }

        // 오류 메세지 반환
        return ResponseEntity.status(HttpStatus.FOUND).location(
                ServletUriComponentsBuilder.fromHttpUrl("http://localhost:5000/login.html?err=$message")
                    .build().toUri()
            ).build<Any>()
    }



}




