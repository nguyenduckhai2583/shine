package com.example.shine.core.model

data class DirectMessage(
    val id: String,
    val name: String,
    val time: Long?,
    val type: String? = null,
)
