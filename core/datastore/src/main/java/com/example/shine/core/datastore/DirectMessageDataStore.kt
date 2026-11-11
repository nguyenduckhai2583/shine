package com.example.shine.core.datastore

import com.example.shine.core.model.DirectMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    fun updateDirectMessage(message: DirectMessage) {
        _directMessagesState.update { current ->
            val index = current.indexOfFirst { it.id == message.id }
            val list = if (index >= 0) {
                current.toMutableList().apply { set(index, message) }
            } else {
                listOf(message) + current
            }
            list.sortedWith { a, b ->
                val t1 = a.time ?: 0L
                val t2 = b.time ?: 0L
                t2.compareTo(t1)
            }
        }
    }

    fun clear() {
        _directMessagesState.value = emptyList()
    }
}
