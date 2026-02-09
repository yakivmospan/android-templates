package com.yakivmospan.templates.core.storage.dto

import com.yakivmospan.templates.core.entity.User

data class UserDTO(
    val id: String,
    val name: String,
    val age: Int,
    val email: String
)

fun UserDTO.toUser(): User {
    return User(
        id = id,
        name = name,
        age = age,
        email = email
    )
}