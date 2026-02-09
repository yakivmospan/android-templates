package com.yakivmospan.templates.presentation.screens.userdetails

import androidx.lifecycle.ViewModel
import com.yakivmospan.templates.presentation.services.Navigator

class UserDetailsViewModel(
    private val navigator: Navigator
) : ViewModel() {

    fun onLoaded() {
        // navigator.navigateTo("user")
    }
}