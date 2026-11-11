package com.example.shine.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.shine.core.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val avatarUrl: String? = null,
)

fun UserEntity.toDomain(): User = User(
    id = id,
    email = email,
    firstName = firstName,
    lastName = lastName,
    avatarUrl = avatarUrl,
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    email = email,
    firstName = firstName,
    lastName = lastName,
    avatarUrl = avatarUrl,
)
