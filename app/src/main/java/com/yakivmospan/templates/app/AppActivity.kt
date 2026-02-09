package com.yakivmospan.templates.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yakivmospan.templates.presentation.screens.main.MainScreen
import com.yakivmospan.templates.presentation.screens.userdetails.UserDetailsScreen
import com.yakivmospan.templates.presentation.screens.userlist.UserListScreen
import com.yakivmospan.templates.presentation.services.ComposeNavigatorCommandsHandler
import com.yakivmospan.templates.presentation.services.NavigationRoute
import com.yakivmospan.templates.presentation.services.NavigationTarget
import com.yakivmospan.templates.presentation.services.NavigatorCommandsFlow
import com.yakivmospan.templates.presentation.services.NavigatorResultsFlow
import com.yakivmospan.templates.presentation.theme.AppTheme
import kotlinx.serialization.Serializable
import org.koin.compose.getKoin

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        innerPadding,
                        navController = rememberNavController(),
                        navigatorCommands = getKoin().get(),
                        navigatorResults = getKoin().get()
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    innerPadding: PaddingValues,
    navController: NavHostController,
    navigatorCommands: NavigatorCommandsFlow = getKoin().get(),
    navigatorResults: NavigatorResultsFlow = getKoin().get()
) {
    // Connects navigation commands & results from Navigator with NavController
    ComposeNavigatorCommandsHandler(navController, navigatorCommands, navigatorResults, onNothingToPop = {/*finish?*/ })

    // Destination graph
    NavHost(navController, startDestination = AppNavigationRoutes.Main) {
        composable<AppNavigationRoutes.Main> { MainScreen(innerPadding) }
        composable<AppNavigationRoutes.UserList> { (UserListScreen(innerPadding)) }
        composable<AppNavigationRoutes.UserDetails> { UserDetailsScreen(innerPadding) }
//        composable<AppNavigationRoutes.Settings> { SettingsScreen() }
    }
}


object AppNavigationResults {
    fun updateKioskDetails(id: Int) = "UpdateKioskDetails:$id"
    const val updateKiosksLists = "UpdateKioskLists"
}

@Serializable
sealed class AppNavigationRoutes(val route: String) {
    @Serializable
    object Main : NavigationRoute

    @Serializable
    object UserList : NavigationRoute

    @Serializable
    data class UserDetails(val id: String) : NavigationRoute

    @Serializable
    object Settings : NavigationRoute
}

sealed class AppNavigationTargets {
    object ToUserList : NavigationTarget(AppNavigationRoutes.UserList, clearBackStackUntil = AppNavigationRoutes.Main)

    data class ToUserDetails(val id: String) : NavigationTarget(
        AppNavigationRoutes.UserDetails(id),
        clearBackStackUntil = AppNavigationRoutes.UserList,
        clearBackInclusively = false
    )

    object ToSettings : NavigationTarget(
        AppNavigationRoutes.Settings,
        clearBackStackUntil = AppNavigationRoutes.UserList,
        clearBackInclusively = false
    )
}