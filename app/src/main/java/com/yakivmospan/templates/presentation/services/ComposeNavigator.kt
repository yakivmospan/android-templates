package com.yakivmospan.templates.presentation.services

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicLong

class ComposeNavigator(
    private val backDebounceMillis: Long = 300L
) : Navigator, NavigatorCommandsFlow, NavigatorResultsFlow {

    private val navigatorCommandsFlow = MutableSharedFlow<NavigatorCommand>(extraBufferCapacity = 1)
    private val results = mutableMapOf<String, MutableSharedFlow<NavigationResult>>()

    // Debouncing timestamps using atomic operations for thread safety
    private val lastNavigateTime = AtomicLong(0L)
    private val lastBackTime = AtomicLong(0L)

    override fun navigate(target: NavigationTarget) {
        if (!shouldAllowNavigation(lastNavigateTime)) {
            return
        }

        navigatorCommandsFlow.tryEmit(
            NavigatorCommand.Target(
                target.route,
                target.clearBackStackUntil,
                target.clearBackInclusively
            )
        )
    }

    override fun openUri(uri: String) {
        navigatorCommandsFlow.tryEmit(NavigatorCommand.URI(uri))
    }

    override fun back(to: NavigationRoute?, inclusive: Boolean, with: NavigationResult?) {
        if (!shouldAllowNavigation(lastBackTime)) {
            return
        }

        navigatorCommandsFlow.tryEmit(NavigatorCommand.Pop(to, inclusive, with))
    }

    override fun exit() {
        navigatorCommandsFlow.tryEmit(NavigatorCommand.Exit)
    }

    override fun waitForResults(key: String): SharedFlow<NavigationResult> {
        return resultsFor(key = key)
    }

    override fun values(): SharedFlow<NavigatorCommand> {
        return navigatorCommandsFlow
    }

    override fun setResult(result: NavigationResult) {
        val flow = results.getOrPut(result.key) { MutableSharedFlow(replay = 0, extraBufferCapacity = 1) }
        flow.tryEmit(result)
    }

    override fun resultsFor(key: String): SharedFlow<NavigationResult> {
        return results.getOrPut(key) { MutableSharedFlow(replay = 0, extraBufferCapacity = 1) }.asSharedFlow()
    }

    /**
     * Checks if enough time has passed since the last navigation to allow a new one.
     * Uses atomic compareAndSet to ensure thread-safe updates.
     *
     * @param lastTime AtomicLong tracking the last navigation timestamp
     * @return true if navigation should be allowed, false if it should be debounced
     */
    private fun shouldAllowNavigation(lastTime: AtomicLong): Boolean {
        val currentTime = System.currentTimeMillis()
        val previousTime = lastTime.get()

        // Check if enough time has passed
        if (currentTime - previousTime < backDebounceMillis) {
            return false
        }

        // Atomically update the timestamp only if it hasn't been changed by another thread
        return lastTime.compareAndSet(previousTime, currentTime)
    }
}


@Composable
fun ComposeNavigatorCommandsHandler(
    navController: NavController,
    navigatorCommandsFlow: NavigatorCommandsFlow,
    navigatorResultsFlow: NavigatorResultsFlow,
    uriHandler: UriHandler = LocalUriHandler.current,
    onNothingToPop: () -> (Unit)
) = LaunchedEffect("ComposeNavigatorCommandsHandler.NavigatorCommandsFlow") {
    navigatorCommandsFlow.values().onEach { command ->
        when (command) {
            is NavigatorCommand.Target -> navigate(navController, command)
            is NavigatorCommand.Pop -> pop(
                navController,
                navigatorResultsFlow,
                command.to,
                command.result,
                command.inclusive,
                onNothingToPop
            )

            is NavigatorCommand.URI -> uriHandler.openUri(command.uri)
            is NavigatorCommand.Exit -> exit()
        }
    }.launchIn(this)
}

private fun navigate(
    navController: NavController,
    navTarget: NavigatorCommand.Target
) {
    navController.navigate(navTarget.route) {
        navTarget.clearBackStackUntil?.let { popUpToRoute ->
            popUpTo(popUpToRoute) { inclusive = navTarget.clearBackInclusively }
        }
    }
}

@SuppressLint("RestrictedApi")
private fun pop(
    navController: NavController,
    navigatorResultsFlow: NavigatorResultsFlow,
    to: NavigationRoute?,
    result: NavigationResult?,
    inclusive: Boolean = false,
    onNothingToPop: () -> Unit
) {
    // Check if there's anything to pop (more than one entry in the back stack)
    if (navController.currentBackStack.value.size <= 2) {
        onNothingToPop()
        return
    }

    if (to != null) {
        navController.popBackStack(to, inclusive)
    } else {
        navController.popBackStack()
    }

    if (result != null) {
        navigatorResultsFlow.setResult(result = result)
    }
}

private fun exit() {
    TODO()
}
