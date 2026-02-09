package com.yakivmospan.templates.core.api.response

import com.yakivmospan.templates.core.entity.User

data class UserResponse(
    val userId: String?,
    val userName: String?,
    val userAge: Int?,
    val userEmail: String?
)

fun UserResponse.toUser(): User {
    return User(
        id = userId ?: throw IllegalStateException("User ID is required"),
        name = userName ?: "",
        age = userAge ?: 0,
        email = userEmail ?: ""
    )
}