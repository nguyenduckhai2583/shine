package com.example.shine.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserProfileDtoTest {

    @Test
    fun toRequest_preservesAllProfileFieldsAndSetsNewAvatarId() {
        val dto = UserProfileDto(
            id = "user-123",
            email = "user@example.com",
            firstName = "John",
            lastName = "Doe",
            phoneNumber = "+123456789",
            gender = "male",
            address = "123 Street",
            bankId = "bank-1",
            bankAccount = "acc-123",
            nationalCard = "nc-123",
            nationalCardAuthority = "Auth",
            nationalCardIssueDate = "2020-01-01",
            taxCode = "tax-123",
            socialInsurance = "si-123",
            birthDate = "1990-01-01",
            avatarId = "old-avatar",
        )

        val request = dto.toRequest(avatarId = "new-avatar")

        assertEquals("user@example.com", request.email)
        assertEquals("John", request.firstName)
        assertEquals("Doe", request.lastName)
        assertEquals("+123456789", request.phoneNumber)
        assertEquals("male", request.gender)
        assertEquals("123 Street", request.address)
        assertEquals("bank-1", request.bankId)
        assertEquals("acc-123", request.bankAccount)
        assertEquals("nc-123", request.nationalCard)
        assertEquals("Auth", request.nationalCardAuthority)
        assertEquals("2020-01-01", request.nationalCardIssueDate)
        assertEquals("tax-123", request.taxCode)
        assertEquals("si-123", request.socialInsurance)
        assertEquals("1990-01-01", request.birthDate)
        assertEquals("new-avatar", request.avatarId)
    }

    @Test
    fun toRequest_defaultAvatarIdIsNull() {
        val dto = UserProfileDto(
            id = "user-123",
            email = "user@example.com",
            firstName = "John",
            lastName = "Doe",
        )

        val request = dto.toRequest()

        assertEquals("user@example.com", request.email)
        assertEquals("John", request.firstName)
        assertEquals("Doe", request.lastName)
        assertNull(request.avatarId)
    }

    @Test
    fun toDomain_mapsUserProfileDtoToDomainUser() {
        val dto = UserProfileDto(
            id = "user-123",
            email = "user@example.com",
            firstName = "John",
            lastName = "Doe",
            avatar = FileDto(signedUrl = "https://cdn.example.com/avatar.png"),
        )

        val user = dto.toDomain()

        assertEquals("user-123", user.id)
        assertEquals("user@example.com", user.email)
        assertEquals("John", user.firstName)
        assertEquals("Doe", user.lastName)
        assertEquals("https://cdn.example.com/avatar.png", user.avatarUrl)
    }
}


