package com.yakivmospan.autocompletesample.lib

/**
 * Generic data source contract for autocomplete queries.
 *
 * @param T the type of item returned by this data source.
 */
interface AutoCompleteDataSource<T> {

    /**
     * Search for items matching [query] on the given [page] (1-based).
     * Returns an empty list when no more results are available, signalling end of pagination.
     */
    suspend fun search(query: String, page: Int): List<T>
}