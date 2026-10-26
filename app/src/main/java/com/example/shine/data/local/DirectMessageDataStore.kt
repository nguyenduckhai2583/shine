package com.example.shine.data.local

import com.example.shine.domain.model.DirectMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectMessageDataStore @Inject constructor() {

    private val _directMessagesState = MutableStateFlow<List<DirectMessage>>(emptyList())
    val directMessagesFlow: Flow<List<DirectMessage>> = _directMessagesState.asStateFlow()

    fun getDirectMessages(): List<DirectMessage> = _directMessagesState.value

    fun replaceDirectMessages(messages: List<DirectMessage>) {
        _directMessagesState.value = messages
    }

    fun clear() {
        _directMessagesState.value = emptyList()
    }
}
