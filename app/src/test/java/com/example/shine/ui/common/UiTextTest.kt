package com.example.shine.ui.common

import com.example.shine.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiTextTest {

    @Test
    fun raw_constructor_and_invoke() {
        val text = UiText("Direct message")
        assertTrue(text is UiText.Raw)
        assertEquals("Direct message", (text as UiText.Raw).value)
    }

    @Test
    fun resource_constructor_and_invoke_with_args() {
        val text = UiText(R.string.channel_created_success, "#android")
        assertTrue(text is UiText.Resource)
        val res = text as UiText.Resource
        assertEquals(R.string.channel_created_success, res.id)
        assertEquals(listOf("#android"), res.args)
    }
}

