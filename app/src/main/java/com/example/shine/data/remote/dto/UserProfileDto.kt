package com.example.shine.data.remote.dto

import com.example.shine.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: String,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val gender: String? = null,
    val address: String? = null,
    val bankId: String? = null,
    val bankAccount: String? = null,
    val nationalCard: String? = null,
    val nationalCardAuthority: String? = null,
    val nationalCardIssueDate: String? = null,
    val taxCode: String? = null,
    val socialInsurance: String? = null,
    val birthDate: String? = null,
    val avatarId: String? = null,
    val avatar: FileDto? = null,
)

@Serializable
data class UpdateUserRequest(
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val gender: String? = null,
    val address: String? = null,
    val bankId: String? = null,
    val bankAccount: String? = null,
    val nationalCard: String? = null,
    val nationalCardAuthority: String? = null,
    val nationalCardIssueDate: String? = null,
    val taxCode: String? = null,
    val socialInsurance: String? = null,
    val birthDate: String? = null,
    val avatarId: String? = null,
)

fun UserProfileDto.toRequest(avatarId: String? = null) = UpdateUserRequest(
    email = email,
    firstName = firstName,
    lastName = lastName,
    phoneNumber = phoneNumber,
    gender = gender,
    address = address,
    bankId = bankId,
    bankAccount = bankAccount,
    nationalCard = nationalCard,
    nationalCardAuthority = nationalCardAuthority,
    nationalCardIssueDate = nationalCardIssueDate,
    taxCode = taxCode,
    socialInsurance = socialInsurance,
    birthDate = birthDate,
    avatarId = avatarId,
)


fun UserProfileDto.toDomain(fallbackAvatarUrl: String? = null) = User(
    id = id,
    email = email,
    firstName = firstName,
    lastName = lastName,
    avatarUrl = avatar?.signedUrl?.takeIf { it.isNotBlank() } ?: fallbackAvatarUrl,
)




