package com.yakivmospan.templates.core.storage.dao

import com.yakivmospan.templates.core.storage.dto.UserDTO

interface UserDao {
    fun getUserById(id: String): UserDTO?
    fun getAllUsers(): List<UserDTO>
}

class UserDaoImpl : UserDao {
    override fun getUserById(id: String): UserDTO? {
        return when (id) {
            "1" -> UserDTO(id = "1", name = "Local John Doe", age = 30, email = "")
            "2" -> UserDTO(id = "2", name = "Local Jane Doe 2", age = 25, email = "")
            "3" -> UserDTO(id = "3", name = "Local Jack Doe 3", age = 35, email = "")
            else -> null
        }
    }

    override fun getAllUsers(): List<UserDTO> {
        return listOf(
            UserDTO(id = "1", name = "Local John Doe", age = 30, email = ""),
            UserDTO(id = "2", name = "Local Jane Doe 2", age = 25, email = ""),
            UserDTO(id = "3", name = "Local Jack Doe 3", age = 35, email = "")
        )
    }
}