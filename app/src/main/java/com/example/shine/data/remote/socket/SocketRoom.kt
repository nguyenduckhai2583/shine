package com.example.shine.data.remote.socket

object SocketRoom {
    private const val KEY_ROOM = "room"

    fun user(userId: String?): String = "user.$userId"

    fun workspace(workspaceId: String): String = "workspace.$workspaceId"

    fun payload(room: String): Map<String, String> = mapOf(KEY_ROOM to room)
}
