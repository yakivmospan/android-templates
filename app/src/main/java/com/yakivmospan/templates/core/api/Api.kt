package com.yakivmospan.templates.core.api

interface Api {
    val userApi: UserApi
}

data class ApiImpl(
    override val userApi: UserApi = UserApiImpl()
) : Api