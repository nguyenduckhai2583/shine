package com.example.shine.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionMapperTest {

    @Test
    fun toDomain_prefersAvatarUrlWhenPresent() {
        val dto = UserDto(
            id = "1",
            firstName = "John",
            lastName = "Doe",
            avatarUrl = "https://cdn.example.com/direct_avatar.png",
            avatar = FileDto(signedUrl = "https://s3.example.com/signed_avatar.png"),
        )

        val user = dto.toDomain()
        assertEquals("https://cdn.example.com/direct_avatar.png", user.avatarUrl)
        assertEquals("https://cdn.example.com/direct_avatar.png", user.avatarPath)
    }

    @Test
    fun toDomain_fallsBackToAvatarSignedUrlWhenAvatarUrlIsNull() {
        val dto = UserDto(
            id = "2",
            firstName = "Jane",
            lastName = "Doe",
            avatarUrl = null,
            avatar = FileDto(signedUrl = "https://s3.example.com/signed_avatar.png"),
        )

        val user = dto.toDomain()
        assertEquals("https://s3.example.com/signed_avatar.png", user.avatarUrl)
        assertEquals("https://s3.example.com/signed_avatar.png", user.avatarPath)
    }

    @Test
    fun toDomain_nullWhenBothAreNull() {
        val dto = UserDto(
            id = "3",
            firstName = "Bob",
            lastName = "Smith",
            avatarUrl = null,
            avatar = null,
        )

        val user = dto.toDomain()
        assertNull(user.avatarUrl)
        assertNull(user.avatarPath)
    }
}
