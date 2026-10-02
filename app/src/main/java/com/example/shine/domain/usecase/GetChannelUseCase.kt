package com.example.shine.domain.usecase

import com.example.shine.domain.model.Channel
import com.example.shine.domain.repository.ChannelRepository
import javax.inject.Inject

class GetChannelUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
) {
    suspend operator fun invoke(id: String): Result<Channel> = channelRepository.getChannel(id)
}
