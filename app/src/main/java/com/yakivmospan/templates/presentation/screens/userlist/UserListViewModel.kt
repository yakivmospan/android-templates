package com.yakivmospan.templates.presentation.screens.userlist

import androidx.lifecycle.ViewModel
import com.yakivmospan.templates.app.AppNavigationTargets
import com.yakivmospan.templates.presentation.services.Navigator

class UserListViewModel(
    private val navigator: Navigator
) : ViewModel() {

    fun onOpenUserDetails(id: String) {
        navigator.navigate(AppNavigationTargets.ToUserDetails(id))
    }
}