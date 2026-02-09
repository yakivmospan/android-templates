package com.yakivmospan.templates.core.domain

import com.yakivmospan.templates.core.entity.User

// App not valid for users younger than 12 years old.
class ValidateUserInteractor {
    fun execute(user: User): Boolean {
        return user.name.isNotEmpty() && user.age >= 12 && user.email.contains("@")
    }
}