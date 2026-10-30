package com.example.shine.data.local

import com.example.shine.data.local.dao.UserDao
import com.example.shine.data.local.entity.toDomain
import com.example.shine.data.local.entity.toEntity
import com.example.shine.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserLocalDataSource @Inject constructor(
    private val userDao: UserDao,
) {
    fun observeUsers(): Flow<List<User>> = userDao.observeUsers().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getUsers(): List<User> = userDao.getUsers().map { it.toDomain() }

    suspend fun getUser(id: String): User? = userDao.getUser(id)?.toDomain()

    suspend fun replaceUsers(users: List<User>) {
        userDao.replaceUsers(users.map { it.toEntity() })
    }

    suspend fun upsertUsers(users: List<User>) {
        userDao.upsertUsers(users.map { it.toEntity() })
    }

    suspend fun clear() {
        userDao.clear()
    }
}

