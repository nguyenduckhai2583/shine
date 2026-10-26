package com.example.shine.domain.model

data class DirectMessage(
    val id: String,
    val name: String,
    val time: Long?,
    val type: String? = null,
)
