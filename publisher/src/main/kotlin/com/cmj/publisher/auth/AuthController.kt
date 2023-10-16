package com.cmj.publisher.auth

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

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




}