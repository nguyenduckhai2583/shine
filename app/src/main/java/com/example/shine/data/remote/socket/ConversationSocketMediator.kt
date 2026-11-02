package com.example.shine.data.remote.socket

import android.util.Log
import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.local.UserLocalDataSource
import com.example.shine.data.remote.dto.ChannelDto
import com.example.shine.data.remote.dto.DirectMessageDto
import com.example.shine.data.remote.dto.toDomain
import com.example.shine.data.remote.socket.event.ConversationCreatedEvent
import com.example.shine.data.remote.socket.event.SocketEvent
import com.example.shine.data.remote.socket.model.ConvSocketMessage
import com.example.shine.data.remote.socket.model.ConversationType
import com.example.shine.domain.repository.ChannelRepository
import com.example.shine.domain.repository.DirectMessageRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationSocketMediator @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val directMessageRepository: DirectMessageRepository,
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val userLocalDataSource: UserLocalDataSource,
    private val json: Json,
) : SocketMediator {

    override suspend fun handleEvent(event: SocketEvent) {
        when (event) {
            is ConversationCreatedEvent -> handleConversationCreated(event.conversation)
        }
    }

    suspend fun handleConversationCreated(message: ConvSocketMessage) {
        when (message.type) {
            ConversationType.CHANNEL -> {
                try {
                    val channelDto = json.decodeFromJsonElement<ChannelDto>(message.rawJson)
                    channelRepository.onChannelCreated(channelDto.toDomain())
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process CHANNEL creation: ${e.message}", e)
                }
            }
            ConversationType.DIRECT,
            ConversationType.GROUP,
            -> {
                try {
                    val dmDto = json.decodeFromJsonElement<DirectMessageDto>(message.rawJson)
                    val currentUserId = sessionLocalDataSource.session.firstOrNull()?.user?.id
                    val chatUsersMap = userLocalDataSource.getUsers().associate { (id, email, firstName, lastName) ->
                        val name = listOfNotNull(firstName, lastName)
                            .joinToString(" ")
                            .trim()
                            .ifEmpty { email.orEmpty() }
                        id to name
                    }
                    val dm = dmDto.toDomain(currentUserId, chatUsersMap)
                    directMessageRepository.onDirectMessageCreated(dm)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process DIRECT/GROUP creation: ${e.message}", e)
                }
            }
            ConversationType.THREAD -> {
                // Thread conversation created; extensible for future thread handling
            }
        }
    }

    companion object {
        private const val TAG = "ConversationSocketMediator"
    }
}
