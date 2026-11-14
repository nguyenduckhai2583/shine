package com.example.shine.core.data

import com.example.shine.core.datastore.security.CryptoManager

class FakeCryptoManager : CryptoManager {
    override fun encrypt(plainText: String): String = plainText
    override fun decrypt(cipherText: String): String = cipherText
}
