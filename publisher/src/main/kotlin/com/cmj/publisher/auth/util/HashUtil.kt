package com.cmj.publisher.auth.util

import at.favre.lib.crypto.bcrypt.BCrypt

object HashUtil {
    fun createHash(cipherText: String): String {
        return BCrypt.withDefaults().hashToString(12, cipherText.toCharArray())
    }

    fun verifyHash(cipherText: String, hash: String) : Boolean {
        return BCrypt.verifyer().verify(cipherText.toCharArray(), hash).verified
    }
}