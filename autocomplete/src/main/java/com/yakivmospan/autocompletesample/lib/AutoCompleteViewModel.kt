package com.yakivmospan.autocompletesample.lib

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_MIN_QUERY_LENGTH = 3
private const val DEFAULT_DEBOUNCE_MILLIS = 300L

/**
 * Presentation layer for the autocomplete component.
 *
 * Manages query debouncing, pagination, and exposes a single [state] stream.
 * All business logic lives here — the composable is kept stateless.
 *
 * @param T the item type produced by [dataSource].
 * @param dataSource the data provider to query.
 */
@OptIn(FlowPreview::class)
class AutoCompleteViewModel<T>(
    private val dataSource: AutoCompleteDataSource<T>,
    private val minQueryLength: Int = DEFAULT_MIN_QUERY_LENGTH,
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow<AutoCompleteState<T>>(AutoCompleteState.Idle)
    internal val state = _state.asStateFlow()

    // Query state — StateFlow so current value is always accessible (e.g. for loadMore).
    private val queryFlow = MutableStateFlow("")
    internal val query = queryFlow.asStateFlow()

    // Current pagination page; reset to 1 on every new query.
    private var currentPage = 1

    // Tracks the in-flight loadMore coroutine so it can be cancelled on query change.
    private var loadMoreJob: Job? = null

    init {
        viewModelScope.launch { subscribeToQueryChanges() }
    }

    private suspend fun subscribeToQueryChanges() = queryFlow
        .drop(1) // skip initial empty-string emission from StateFlow
        .debounce(debounceMillis)
        .distinctUntilChanged()
        .collectLatest { query -> onQueryChange(query) }

    private suspend fun onQueryChange(query: String) {
        cancelLoadMore()
        resetCurrentPage()

        if (query.length < minQueryLength) {
            _state.value = AutoCompleteState.Idle
        } else {
            _state.value = AutoCompleteState.Loading
            _state.value = fetchPage(query, page = 1, existing = emptyList())
        }
    }

    /**
     * Single entry point for all UI interactions.
     *
     * ```kotlin
     * viewModel.onEvent(AutoCompleteEvent.QueryChanged("kot"))
     * viewModel.onEvent(AutoCompleteEvent.LoadMore)
     * ```
     */
    internal fun onEvent(event: AutoCompleteEvent) {
        when (event) {
            is AutoCompleteEvent.QueryChanged -> handleQueryChangedEvent(event.query)
            is AutoCompleteEvent.LoadMore -> handleLoadMoreEvent()
            is AutoCompleteEvent.Clear -> handleClearEvent()
        }
    }

    private fun handleQueryChangedEvent(query: String) {
        queryFlow.value = query
    }

    private fun handleLoadMoreEvent() {
        val current = _state.value as? AutoCompleteState.Success ?: return
        if (!current.hasMore || current.isLoadingMore) return

        _state.update { current.copy(isLoadingMore = true) }

        loadMoreJob = viewModelScope.launch {
            val nextPage = currentPage + 1
            val nextState = fetchPage(queryFlow.value, nextPage, existing = current.items)

            if (nextState is AutoCompleteState.Success) {
                currentPage = nextPage
                _state.value = nextState
            } else {
                // silent failure — restore previous state so user can retry by scrolling again
                _state.value = current.copy(isLoadingMore = false)
            }
        }
    }

    private fun handleClearEvent() {
        cancelLoadMore()
        queryFlow.value = ""
        _state.value = AutoCompleteState.Idle
        resetCurrentPage()
    }

    private fun resetCurrentPage() {
        currentPage = 1
    }

    private fun cancelLoadMore() {
        loadMoreJob?.cancel()
        loadMoreJob = null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun fetchPage(
        query: String,
        page: Int,
        existing: List<T>,
    ): AutoCompleteState<T> = withContext(ioDispatcher) {
        runCatching {
            val newItems = dataSource.search(query, page)
            val combined = existing + newItems
            if (combined.isEmpty()) {
                AutoCompleteState.Empty
            } else {
                AutoCompleteState.Success(
                    items = combined,
                    isLoadingMore = false,
                    hasMore = newItems.isNotEmpty(),
                )
            }
        }.getOrElse { error ->
            AutoCompleteState.Error(error.message ?: "Unknown error")
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        /**
         * Creates a [ViewModelProvider.Factory] for [AutoCompleteViewModel].
         * Use when constructing the ViewModel outside of a DI framework.
         *
         * ```kotlin
         * val viewModel: AutoCompleteViewModel<GitHubItem> = viewModel(
         *     factory = AutoCompleteViewModel.Factory(GitHubAutoCompleteDataSource())
         * )
         * ```
         */
        fun <T> Factory(
            dataSource: AutoCompleteDataSource<T>,
            minQueryLength: Int = DEFAULT_MIN_QUERY_LENGTH,
            debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <VM : ViewModel> create(modelClass: Class<VM>): VM =
                    AutoCompleteViewModel(dataSource, minQueryLength, debounceMillis, ioDispatcher) as VM
            }
    }
}