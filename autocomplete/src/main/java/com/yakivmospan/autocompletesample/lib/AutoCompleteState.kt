package com.yakivmospan.autocompletesample.lib

/**
 * Represents the UI state of the autocomplete component.
 *
 * @param T the type of item displayed in the results list.
 */
sealed class AutoCompleteState<out T> {

    /** No query entered yet — component is idle. */
    data object Idle : AutoCompleteState<Nothing>()

    /** Initial search in progress (first page, no results shown yet). */
    data object Loading : AutoCompleteState<Nothing>()

    /** Search completed but returned no results. */
    data object Empty : AutoCompleteState<Nothing>()

    /**
     * Results are available.
     *
     * @property items the accumulated list of results across all loaded pages.
     * @property isLoadingMore true while the next page is being fetched (shows bottom spinner).
     * @property hasMore false when the data source returned an empty page (no further loads).
     */
    data class Success<T>(
        val items: List<T>,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true,
    ) : AutoCompleteState<T>()

    /**
     * Initial search failed.
     *
     * @property message human-readable error description.
     */
    data class Error(val message: String) : AutoCompleteState<Nothing>()
}


