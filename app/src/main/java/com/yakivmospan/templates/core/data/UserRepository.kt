package com.yakivmospan.templates.core.data

import com.yakivmospan.templates.core.api.Api
import com.yakivmospan.templates.core.api.request.UserRequest
import com.yakivmospan.templates.core.api.response.toUser
import com.yakivmospan.templates.core.entity.User
import com.yakivmospan.templates.core.storage.Storage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

interface UserRepository {
    suspend fun getUser(id: String): User
    suspend fun getUsers(strategy: UpdateStrategy): List<User>
}

class UserRepositoryImpl(
    private val api: Api,
    private val storage: Storage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserRepository {

    override suspend fun getUser(id: String): User {
        return withContext(ioDispatcher) {
            delay(1000L)
            return@withContext api.userApi.fetchUser(UserRequest(id)).toUser()
        }
    }

    override suspend fun getUsers(strategy: UpdateStrategy): List<User> {
        return when (strategy) {
            UpdateStrategy.ALWAYS_FETCH -> fetchUsersFromRemote()
            UpdateStrategy.ALWAYS_CACHED -> getUsersFromStorage()
            UpdateStrategy.TRY_FETCH_ELSE_CACHED -> fetchUsersFromRemoteOrGetFromStorage()
            UpdateStrategy.TRY_CACHED_ELSE_FETCH -> getUsersFromStorageOrFetch()
        }
    }

    private suspend fun fetchUsersFromRemote(): List<User> {
        return withContext(ioDispatcher) {
            delay(1000L)
            return@withContext api.userApi.fetchUsers().map { it.toUser() }
        }
    }

    private suspend fun getUsersFromStorage(): List<User> {
        return withContext(ioDispatcher) {
            delay(1000L)
            return@withContext api.userApi.fetchUsers().map { it.toUser() }
        }
    }

    private suspend fun fetchUsersFromRemoteOrGetFromStorage(): List<User> {
        val users = try {
            fetchUsersFromRemote()
        } catch (e: Throwable) {
            emptyList()
        }
        return users.ifEmpty { getUsersFromStorage() }
    }

    private suspend fun getUsersFromStorageOrFetch(): List<User> {
        val users = try {
            getUsersFromStorage()
        } catch (e: Throwable) {
            emptyList()
        }
        return users.ifEmpty { fetchUsersFromRemote() }
    }
}