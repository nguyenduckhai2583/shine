package com.example.shine.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SignIn : NavKey

@Serializable
data object Home : NavKey

@Serializable
data class ChannelDetail(val channelId: String) : NavKey

@Serializable
data object PlanixList : NavKey
