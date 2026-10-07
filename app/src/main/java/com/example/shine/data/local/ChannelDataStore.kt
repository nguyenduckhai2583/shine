package com.example.shine.data.local

import com.example.shine.domain.model.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
    }

    fun replaceChannels(channels: List<Channel>) {
        _channelsState.value = channels
    }

    fun updateChannel(channel: Channel) {
        val current = _channelsState.value.toMutableList()
        val index = current.indexOfFirst { it.id == channel.id }
        if (index >= 0) {
            current[index] = channel
        } else {
            current.add(channel)
        }
        _channelsState.value = current
    }

    fun clear() {
        _channelsState.value = emptyList()
    }
}
