package com.example.shine.core.testing.repository

import com.example.shine.core.data.repository.DirectMessageRepository
import com.example.shine.core.model.DirectMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class FakeDirectMessageRepository(
    initialDms: List<DirectMessage> = emptyList(),
) : DirectMessageRepository {

    val dmsFlow = MutableStateFlow<List<DirectMessage>>(initialDms)
    val dmFlow = MutableStateFlow<DirectMessage?>(null)

    var shouldFail: Boolean = false
    var failureException: Throwable = Exception("Failed to connect")
    var clearCalled: Boolean = false
    var getDmsCallCount: Int = 0
    var lastId: String? = null
    var autoUpdateFlowOnGetDirectMessage: Boolean = true
    val createdDms = mutableListOf<DirectMessage>()

    override fun getDirectMessagesFlow(): Flow<List<DirectMessage>> = dmsFlow

    override fun getDirectMessageFlow(id: String): Flow<DirectMessage?> {
        return combine(dmsFlow, dmFlow) { dms, single ->
            dms.find { it.id == id } ?: single
        }
    }

    override suspend fun getDirectMessages(): Result<Unit> {
        getDmsCallCount++
        if (shouldFail) return Result.failure(failureException)
        return Result.success(Unit)
    }

    override suspend fun getDirectMessage(id: String): Result<Unit> {
        lastId = id
        if (shouldFail) return Result.failure(failureException)
        if (autoUpdateFlowOnGetDirectMessage) {
            val existing = dmsFlow.value.find { it.id == id }
            if (existing != null) {
                dmFlow.value = existing
            } else if (dmFlow.value == null) {
                dmFlow.value = DirectMessage(
                    id = id,
                    name = "Alice",
                    time = null,
                    type = "direct",
                )
            }
        }
        return Result.success(Unit)
    }

    override suspend fun onDirectMessageCreated(message: DirectMessage) {
        createdDms.add(message)
        dmsFlow.value += message
    }

    override suspend fun clear() {
        clearCalled = true
        dmsFlow.value = emptyList()
        dmFlow.value = null
    }
}
