package com.example.shine.core.datastore.security

interface CryptoManager {
    fun encrypt(plainText: String): String
    fun decrypt(cipherText: String): String
}
