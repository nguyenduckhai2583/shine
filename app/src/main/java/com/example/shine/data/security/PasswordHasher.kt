package com.example.shine.data.security

import com.example.shine.BuildConfig
import java.security.MessageDigest
import javax.inject.Inject

/** Mirrors employer-mobile `EncryptUtil.generateSha1Password`: sha1(password + salt). */
class PasswordHasher @Inject constructor() {

    fun sha1(password: String, salt: String = BuildConfig.PASSWORD_SALT): String =
        MessageDigest.getInstance("SHA-1")
            .digest((password + salt).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
