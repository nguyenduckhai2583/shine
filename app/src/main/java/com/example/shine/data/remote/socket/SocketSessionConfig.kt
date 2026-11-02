package com.example.shine.data.remote.socket

/**
 * Encapsulates active socket session parameters.
 * Serves as the single source of truth for the socket's room subscriptions and credentials.
 */
data class SocketSessionConfig(
    val token: String,
    val userId: String?,
    val workspaceId: String? = null,
)
