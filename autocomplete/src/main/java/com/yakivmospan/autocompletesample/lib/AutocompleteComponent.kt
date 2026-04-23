package com.yakivmospan.autocompletesample.lib

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Factory / entry point — convenience for the common case
 */
@Composable
fun <T> rememberAutoCompleteViewModel(dataSource: AutoCompleteDataSource<T>): AutoCompleteViewModel<T> = viewModel(
    factory = AutoCompleteViewModel.Factory(dataSource)
)

// ── Public overload — ViewModel-connected ─────────────────────────────────────

/**
 * Generic autocomplete component. Connects to [AutoCompleteViewModel] and delegates
 * item rendering to [itemContent]. Click handling belongs to the caller via
 * [Modifier.clickable] inside [itemContent].
 *
 * @param viewModel the ViewModel driving this component.
 * @param itemContent slot that renders a single result item.
 * @param modifier optional modifier applied to the root column.
 */
@Composable
fun <T> AutoCompleteComponent(
    viewModel: AutoCompleteViewModel<T>,
    itemContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    AutoCompleteComponent(
        query = query,
        state = state,
        onEvent = viewModel::onEvent,
        itemContent = itemContent,
        modifier = modifier,
    )
}

// ── Stateless, Preview-friendly overload────────────────────────────

@Composable
fun <T> AutoCompleteComponent(
    query: String,
    state: AutoCompleteState<T>,
    onEvent: (AutoCompleteEvent) -> Unit,
    itemContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SearchField(query = query, onEvent = onEvent)
        Spacer(modifier = Modifier.height(4.dp))
        when (state) {
            is AutoCompleteState.Idle -> IdleContent()
            is AutoCompleteState.Loading -> LoadingContent()
            is AutoCompleteState.Empty -> EmptyContent()
            is AutoCompleteState.Error -> ErrorContent(state.message)
            is AutoCompleteState.Success -> SuccessContent(
                state = state,
                onEvent = onEvent,
                itemContent = itemContent,
            )
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun SearchField(
    query: String,
    onEvent: (AutoCompleteEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clearDescription = stringResource(R.string.autocomplete_clear)
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = { onEvent(AutoCompleteEvent.QueryChanged(it)) },
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.autocomplete_hint)) },
        label = { Text(stringResource(R.string.autocomplete_title)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
                onEvent(AutoCompleteEvent.Search)
            }
        ),
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onEvent(AutoCompleteEvent.Clear) }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = clearDescription,
                    )
                }
            }
        },
    )
}

@Composable
private fun IdleContent(modifier: Modifier = Modifier) {
    // Intentionally empty — no prompt shown while the field is blank.
    Box(modifier = modifier)
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    val message = stringResource(R.string.autocomplete_empty)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier,
) {
    val errorText = stringResource(R.string.autocomplete_error, message)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = errorText
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = errorText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun <T> SuccessContent(
    state: AutoCompleteState.Success<T>,
    onEvent: (AutoCompleteEvent) -> Unit,
    itemContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Trigger loadMore when last item is visible.
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = listState.layoutInfo.totalItemsCount
            state.hasMore && !state.isLoadingMore && totalItems > 0 && lastVisible >= totalItems - 1
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onEvent(AutoCompleteEvent.LoadMore)
    }

    val loadingMoreLabel = stringResource(R.string.autocomplete_loading_more)

    LazyColumn(state = listState, modifier = modifier.fillMaxWidth()) {
        itemsIndexed(
            items = state.items,
            key = { index, _ -> index },
        ) { _, item ->
            itemContent(item)
        }

        if (state.isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .wrapContentHeight()
                        .semantics { contentDescription = loadingMoreLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Idle")
@Composable
private fun AutoCompleteComponentIdlePreview() {
    AutoCompleteComponent(
        query = "",
        state = AutoCompleteState.Idle,
        onEvent = {},
        itemContent = { _: String -> },
    )
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun AutoCompleteComponentLoadingPreview() {
    AutoCompleteComponent(
        query = "kotlin",
        state = AutoCompleteState.Loading,
        onEvent = {},
        itemContent = { _: String -> },
    )
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun AutoCompleteComponentEmptyPreview() {
    AutoCompleteComponent(
        query = "xyzxyz",
        state = AutoCompleteState.Empty,
        onEvent = {},
        itemContent = { _: String -> },
    )
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun AutoCompleteComponentErrorPreview() {
    AutoCompleteComponent(
        query = "kotlin",
        state = AutoCompleteState.Error("Network unavailable"),
        onEvent = {},
        itemContent = { _: String -> },
    )
}

@Preview(showBackground = true, name = "Success")
@Composable
private fun AutoCompleteComponentSuccessPreview() {
    AutoCompleteComponent(
        query = "kotlin",
        state = AutoCompleteState.Success(
            items = listOf("kotlin/kotlin", "kotlinx/coroutines", "JetBrains/kotlin"),
            hasMore = true,
        ),
        onEvent = {},
        itemContent = { item: String ->
            Text(
                text = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        },
    )
}

@Preview(showBackground = true, name = "Success — loading more")
@Composable
private fun AutoCompleteComponentLoadingMorePreview() {
    AutoCompleteComponent(
        query = "kotlin",
        state = AutoCompleteState.Success(
            items = listOf("kotlin/kotlin", "kotlinx/coroutines"),
            hasMore = true,
            isLoadingMore = true,
        ),
        onEvent = {},
        itemContent = { item: String ->
            Text(
                text = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        },
    )
}