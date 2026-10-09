package com.example.shine.domain.model

data class Project(
    val id: String,
    val name: String,
    val prefix: String?,
    val status: String?,
    val logoUrl: String?,
    val leaderName: String?,
)
