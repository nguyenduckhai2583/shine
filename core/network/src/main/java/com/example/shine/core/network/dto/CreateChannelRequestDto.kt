package com.example.shine.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateChannelRequestDto(
    val name: String,
    val isPrivate: Boolean = false,
    val isEncrypted: Boolean = false,
)

