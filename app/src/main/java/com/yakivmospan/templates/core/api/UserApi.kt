package com.yakivmospan.templates.core.api

import com.yakivmospan.templates.core.api.request.UserRequest
import com.yakivmospan.templates.core.api.response.UserResponse


interface UserApi {
    fun fetchUser(data: UserRequest): UserResponse
    fun fetchUsers(): List<UserResponse>
}

class UserApiImpl : UserApi {
    override fun fetchUser(data: UserRequest): UserResponse {
        return UserResponse(
            userId = data.id,
            userName = "John Doe",
            userAge = 30,
            userEmail = "john@doe.com"
        )
    }

    override fun fetchUsers(): List<UserResponse> {
        return listOf(
            UserResponse(
                userId = "1",
                userName = "John Doe",
                userAge = 30,
                userEmail = "john@doe.com"
            ),
            UserResponse(
                userId = "2",
                userName = "Jane Doe 2",
                userAge = 25,
                userEmail = "john@doe2.com"
            ),
            UserResponse(
                userId = "3",
                userName = "Jack Doe 3",
                userAge = 35,
                userEmail = "john@doe3.com"
            ),
        )
    }

}