package com.yakivmospan.autocompletesample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yakivmospan.autocompletesample.lib.AutoCompleteState
import com.yakivmospan.autocompletesample.lib.GitHubAutoCompleteComponent
import com.yakivmospan.autocompletesample.lib.GitHubItem
import com.yakivmospan.autocompletesample.lib.rememberGitHubAutoCompleteViewModel
import com.yakivmospan.autocompletesample.ui.theme.AutocompleteSampleTheme

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutocompleteSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AutocompleteSampleApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AutocompleteSampleApp(modifier: Modifier = Modifier) {
    val gitHubAutoCompleteViewModel = rememberGitHubAutoCompleteViewModel()

    GitHubAutoCompleteComponent(
        viewModel = gitHubAutoCompleteViewModel,
        onItemSelected = { item -> Log.d(TAG, "Selected: [${item.javaClass.simpleName}] ${item.name}") },
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

// ── Previews ──────────────────────────────────────────────────────────────────

private val previewModifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)

@Preview(showBackground = true, name = "App — Idle")
@Composable
private fun AppIdlePreview() {
    AutocompleteSampleTheme {
        GitHubAutoCompleteComponent(
            query = "",
            state = AutoCompleteState.Idle,
            onEvent = {},
            onItemSelected = {},
            modifier = previewModifier,
        )
    }
}

@Preview(showBackground = true, name = "App — Loading")
@Composable
private fun AppLoadingPreview() {
    AutocompleteSampleTheme {
        GitHubAutoCompleteComponent(
            query = "kotlin",
            state = AutoCompleteState.Loading,
            onEvent = {},
            onItemSelected = {},
            modifier = previewModifier,
        )
    }
}

@Preview(showBackground = true, name = "App — Results")
@Composable
private fun AppSuccessPreview() {
    AutocompleteSampleTheme {
        GitHubAutoCompleteComponent(
            query = "kotlin",
            state = AutoCompleteState.Success(
                items = listOf(
                    GitHubItem.User(id = 1L, login = "torvalds", avatarUrl = null),
                    GitHubItem.Repository(id = 2L, fullName = "kotlin/kotlin", avatarUrl = null),
                    GitHubItem.User(id = 3L, login = "gvanrossum", avatarUrl = null),
                ),
            ),
            onEvent = {},
            onItemSelected = {},
            modifier = previewModifier,
        )
    }
}

@Preview(showBackground = true, name = "App — Empty")
@Composable
private fun AppEmptyPreview() {
    AutocompleteSampleTheme {
        GitHubAutoCompleteComponent(
            query = "xyzxyz",
            state = AutoCompleteState.Empty,
            onEvent = {},
            onItemSelected = {},
            modifier = previewModifier,
        )
    }
}

@Preview(showBackground = true, name = "App — Error")
@Composable
private fun AppErrorPreview() {
    AutocompleteSampleTheme {
        GitHubAutoCompleteComponent(
            query = "kotlin",
            state = AutoCompleteState.Error("Unable to reach GitHub"),
            onEvent = {},
            onItemSelected = {},
            modifier = previewModifier,
        )
    }
}
