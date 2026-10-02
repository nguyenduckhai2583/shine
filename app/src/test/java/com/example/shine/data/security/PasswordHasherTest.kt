package com.example.shine.data.security

import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun sha1_appendsSaltAndReturnsLowercaseHex() {
        val hash = PasswordHasher().sha1("Password@123", salt = "709d0a32e4d0b1a2ce8800998d95d6f9")

        assertEquals("94feabd9ee91a830a77e12d0a9ea2cb5b1ab5efd", hash)
    }
}
