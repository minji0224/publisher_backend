package com.cmj.publisher.auth

import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/profile")
class ProfileController {

    @Auth
    @GetMapping
    fun fetch(@RequestAttribute authProfile: AuthProfile) :ResponseEntity<ProfileResponse> {


        val profile = transaction {
            Profiles.select{Profiles.id eq authProfile.id}.map { r ->
                ProfileResponse(
                    r[Profiles.publisherName],
                    r[Profiles.email],
                    r[Profiles.businessRegistrationNumber]
                )
            }.singleOrNull()
        }

        return profile?.let {ResponseEntity.ok(it)} ?: ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()


//        if(!profile.isNullOrEmpty()) {
//            return ResponseEntity.status(HttpStatus.OK).body(mapOf("data" to profile))
//        } else {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("data" to profile, "error" to "unauthorized"))
//        }
    }










}