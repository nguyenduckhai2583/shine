package com.example.shine.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateChannelRequestDto(
    val name: String,
    val isPrivate: Boolean = false,
    val isEncrypted: Boolean = false,
)

