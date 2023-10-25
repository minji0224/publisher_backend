package com.cmj.publisher.auth

import com.cmj.publisher.auth.util.HashUtil
import com.cmj.publisher.auth.util.JwtUtil
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AuthService(private  val database: Database) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)
    /*
      현재 클래스의 이름을 기반으로 로거 객체를 가져옴.
      -> 로거객체를 사용하여 로깅 메세지를 생성 및 출력
      -> 로거를 사용하면 애플리케이션의 로그를 기록 및 추적할 수 있고, 디버깅 및 오류 추적에 사용
     */

    fun createIdentity(signupRequest: SignupRequest): Long {
        val record = transaction {
            Identities.select { Identities.publisherName eq signupRequest.publisherName }.singleOrNull()
        }
        // 해당테이블에 publisherName이 존재하면
        if(record != null) {
            return 0;
        }

        // 트랜잭션에 묶으면 오래걸려서 일부러 뺌
        val secret = HashUtil.createHash(signupRequest.password)

        val profileId = transaction {
            try {
                val identityId = Identities.insertAndGetId {
                    it[this.publisherName] = signupRequest.publisherName
                    it[this.secret] = secret
                }
                val profileId = Profiles.insertAndGetId {
                    it[this.publisherName] = signupRequest.publisherName
                    it[this.businessRegistrationNumber] = signupRequest.businessRegistrationNumber
                    it[this.email] = signupRequest.email
                    it[this.identityId] = identityId.value
                }
                return@transaction profileId.value
            } catch (e: Exception) {
                rollback()
                logger.error(e.message)
                return@transaction 0
                /*
                  1. transaction 내부에서 예외처리 발생하면 자동 rollback
                  2. transaction 내부에 try-catch구문이 있으면
                      -> 예외처리 발생시에 catch로 가버림
                      -> transaction 함수에서는 예외처리 발생하지 않은 것으로 봄
                     그러므로 수동으로 catch 구문에서 rollback()을 해줘야함.
                */
            }
        }
        return profileId
    }

    fun authenticate(publisherName: String, password: String) : Pair<Boolean, String> {
        /*
            payload : 일반적으로 데이터나 정보를 전송/처리할 때 사용되는 데이터 조각
                        -> 메세지, 요청, 패킷 내의 실제 데이터 등
            pair : 두 개의 값을 묶어서 저장(첫번째 요소를 통해 두번째 요소에 접근할 수 있음)
        */

        val(result, payload) = transaction(database.transactionManager.defaultIsolationLevel, readOnly = true) {

            val identityRecord = Identities.select { Identities.publisherName eq publisherName }.singleOrNull()
                ?: return@transaction Pair(false, mapOf("message" to "Unauthorized"))

            val profileRecord = Profiles.select { Profiles.identityId eq identityRecord[Identities.id].value }.singleOrNull()
                ?: return@transaction Pair(false, mapOf("massage" to "Conflict"))

            return@transaction Pair(true, mapOf(
                    "id" to profileRecord[Profiles.id],
                    "businessRegistrationNumber" to profileRecord[Profiles.businessRegistrationNumber],
                    "publisherName" to identityRecord[Identities.publisherName],
                    "secret" to identityRecord[Identities.secret]
            ))
        }

        if (!result) {
            return Pair(false, payload["massage"].toString())
        }

        val isVerified = HashUtil.verifyHash(password, payload["secret"].toString())
        if (!isVerified) {
            return Pair(false, "Unauthorized")
        }

        val token = JwtUtil.createToken(
                payload["id"].toString().toLong(),
                payload["publisherName"].toString(),
                payload["businessRegistrationNumber"].toString()
        )
        return Pair(true, token)
    }
}