package com.example.shine.core.model

data class Workspace(
    val id: String,
    val name: String,
    val isActive: Boolean = true,
    val subdomain: String? = null,
    val logoUrl: String? = null,
)
