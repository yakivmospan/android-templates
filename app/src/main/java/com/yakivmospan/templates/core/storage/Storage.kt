package com.yakivmospan.templates.core.storage

import com.yakivmospan.templates.core.storage.dao.UserDao
import com.yakivmospan.templates.core.storage.dao.UserDaoImpl

interface Storage {
    val userDao: UserDao
}

class StorageImpl() : Storage {
    override val userDao: UserDao = UserDaoImpl()
}