package com.yakivmospan.templates.presentation.screens.main

import androidx.lifecycle.ViewModel
import com.yakivmospan.templates.app.AppNavigationTargets
import com.yakivmospan.templates.presentation.services.Navigator

class MainViewModel(
    private val navigator: Navigator
) : ViewModel() {

    fun onLoaded() {
         navigator.navigate(AppNavigationTargets.ToUserList)
    }
}