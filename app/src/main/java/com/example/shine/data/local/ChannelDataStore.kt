package com.example.shine.data.local

import com.example.shine.domain.model.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelDataStore @Inject constructor() {

    private val _channelsState = MutableStateFlow<List<Channel>>(emptyList())
    val channelsFlow: Flow<List<Channel>> = _channelsState.asStateFlow()

    fun getChannels(): List<Channel> = _channelsState.value

    fun getChannel(id: String): Channel? = _channelsState.value.firstOrNull { it.id == id }

    fun getChannelFlow(id: String): Flow<Channel?> = channelsFlow.map { channels ->
        channels.firstOrNull { it.id == id }
    }.distinctUntilChanged()

    fun replaceChannels(channels: List<Channel>) {
        _channelsState.value = channels
    }

    fun updateChannel(channel: Channel) {
        _channelsState.update { current ->
            val index = current.indexOfFirst { it.id == channel.id }
            if (index >= 0) {
                current.toMutableList().apply { set(index, channel) }
            } else {
                current + channel
            }
        }
    }

    fun clear() {
        _channelsState.value = emptyList()
    }
}
