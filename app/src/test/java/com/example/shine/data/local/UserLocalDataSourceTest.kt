package com.example.shine.data.local

import com.example.shine.data.local.dao.UserDao
import com.example.shine.data.local.entity.UserEntity
import com.example.shine.data.local.entity.toDomain
import com.example.shine.data.local.entity.toEntity
import com.example.shine.domain.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class UserLocalDataSourceTest {

    private val userDao = mock(UserDao::class.java)
    private val dataSource = UserLocalDataSource(userDao)

    @Test
    fun observeUsers_mapsEntitiesToDomain() = runTest {
        val entities = listOf(
            UserEntity(id = "1", email = "test1@test.com", firstName = "A", lastName = "B"),
            UserEntity(id = "2", email = "test2@test.com", firstName = "C", lastName = "D"),
        )
        `when`(userDao.observeUsers()).thenReturn(flowOf(entities))

        val result = dataSource.observeUsers().first()
        assertEquals(2, result.size)
        assertEquals("1", result[0].id)
        assertEquals("2", result[1].id)
    }

    @Test
    fun getUsers_mapsEntitiesToDomain() = runTest {
        val entities = listOf(
            UserEntity(id = "1", email = "test1@test.com"),
        )
        `when`(userDao.getUsers()).thenReturn(entities)

        val result = dataSource.getUsers()
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun getUser_found_returnsDomain() = runTest {
        val entity = UserEntity(id = "1", email = "test1@test.com")
        `when`(userDao.getUser("1")).thenReturn(entity)

        val result = dataSource.getUser("1")
        assertEquals("1", result?.id)
        assertEquals("test1@test.com", result?.email)
    }

    @Test
    fun getUser_notFound_returnsNull() = runTest {
        `when`(userDao.getUser("unknown")).thenReturn(null)

        val result = dataSource.getUser("unknown")
        assertNull(result)
    }

    @Test
    fun replaceUsers_callsDaoReplace() = runTest {
        val users = listOf(User(id = "1", email = "u1@test.com"))
        dataSource.replaceUsers(users)

        verify(userDao).replaceUsers(users.map { it.toEntity() })
    }

    @Test
    fun upsertUsers_callsDaoUpsert() = runTest {
        val users = listOf(User(id = "1", email = "u1@test.com"))
        dataSource.upsertUsers(users)

        verify(userDao).upsertUsers(users.map { it.toEntity() })
    }

    @Test
    fun clear_callsDaoClear() = runTest {
        dataSource.clear()

        verify(userDao).clear()
    }
}

