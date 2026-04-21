package com.yakivmospan.autocompletesample.lib

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

private const val TAG = "AutoCompleteComponent"
private const val TEST_QUERY = "kotlin"

@Composable
fun AutoCompleteComponent(modifier: Modifier = Modifier) {
    val dataSource = remember { GitHubAutoCompleteDataSource() }

    LaunchedEffect(Unit) {
        Log.d(TAG, "Running test search for \"$TEST_QUERY\"…")
        runCatching { dataSource.search(query = TEST_QUERY, page = 1) }
            .onSuccess { items ->
                Log.d(TAG, "Results (${items.size}):")
                items.forEach { item ->
                    when (item) {
                        is GitHubItem.User -> Log.d(TAG, "  [User]       ${item.login}")
                        is GitHubItem.Repository -> Log.d(TAG, "  [Repository] ${item.fullName}")
                    }
                }
            }
            .onFailure { error ->
                Log.e(TAG, "Search failed: ${error.message}", error)
            }
    }

    Box(modifier = modifier) {
        Text(stringResource(R.string.autocomplete_title))
    }
}

@Preview(showBackground = true)
@Composable
fun AutoCompleteComponentPreview() {
    AutoCompleteComponent()
}