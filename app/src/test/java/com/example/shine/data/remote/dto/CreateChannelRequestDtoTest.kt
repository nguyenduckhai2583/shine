package com.example.shine.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateChannelRequestDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Test
    fun serialization_alwaysIncludesIsPrivateAndIsEncrypted() {
        val dto = CreateChannelRequestDto(
            name = "hihi",
            isPrivate = false,
            isEncrypted = false,
        )
        val serialized = json.encodeToString(dto)
        assertEquals("""{"name":"hihi","isPrivate":false,"isEncrypted":false}""", serialized)
    }

    @Test
    fun serialization_withDefaultValues_alwaysIncludesIsPrivateAndIsEncrypted() {
        val dto = CreateChannelRequestDto(name = "hihi")
        val serialized = json.encodeToString(dto)
        assertEquals("""{"name":"hihi","isPrivate":false,"isEncrypted":false}""", serialized)
    }
}
