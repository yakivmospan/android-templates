package com.yakivmospan.templates.app

import com.yakivmospan.templates.core.api.Api
import com.yakivmospan.templates.core.api.ApiImpl
import com.yakivmospan.templates.core.data.UserRepository
import com.yakivmospan.templates.core.data.UserRepositoryImpl
import com.yakivmospan.templates.core.domain.ValidateUserInteractor
import com.yakivmospan.templates.core.service.Connectivity
import com.yakivmospan.templates.core.service.ConnectivityImpl
import com.yakivmospan.templates.core.storage.Storage
import com.yakivmospan.templates.core.storage.StorageImpl
import com.yakivmospan.templates.presentation.screens.main.MainViewModel
import com.yakivmospan.templates.presentation.screens.userdetails.UserDetailsViewModel
import com.yakivmospan.templates.presentation.screens.userlist.UserListViewModel
import com.yakivmospan.templates.presentation.services.ComposeNavigator
import com.yakivmospan.templates.presentation.services.Navigator
import com.yakivmospan.templates.presentation.services.NavigatorCommandsFlow
import com.yakivmospan.templates.presentation.services.NavigatorResultsFlow
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModules() = listOf(services, navigation, viewModels, repositories, interactors)

val services = module {
    single<Api> { ApiImpl() }
    single<Storage> { StorageImpl() }
    single<Connectivity> { ConnectivityImpl(context = get()) }
}

val navigation = module {
    val composeNavigator = ComposeNavigator()
    single<Navigator> { composeNavigator }
    single<NavigatorCommandsFlow> { composeNavigator }
    single<NavigatorResultsFlow> { composeNavigator }
}

val viewModels = module {
    viewModel { MainViewModel(navigator = get()) }
    viewModel { UserListViewModel(navigator = get()) }
    viewModel { UserDetailsViewModel(navigator = get()) }
}

val interactors = module {
    factory { ValidateUserInteractor() }
}

val repositories = module {
    single<UserRepository> { UserRepositoryImpl(api = get(), storage = get()) }
}
