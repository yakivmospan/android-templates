package com.yakivmospan.templates.core.service

import android.content.Context

interface Connectivity {
    fun isConnected(): Boolean
}

class ConnectivityImpl(private val context: Context) : Connectivity {
    override fun isConnected(): Boolean {
        // Implement platform-specific connectivity check here
        return true // Placeholder implementation
    }
}