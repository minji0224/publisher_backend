package com.cmj.publisher.auth

data class SignupRequest (
        val publisherName: String,
        val password: String,
        val email: String,
        val businessRegistrationNumber: String,
)

data class AuthProfile (
        val id: Long = 0, // 프로필 id
        val publisherName: String, // 로그인(아이덴티티스)
        val businessRegistrationNumber: String // 프로필
)