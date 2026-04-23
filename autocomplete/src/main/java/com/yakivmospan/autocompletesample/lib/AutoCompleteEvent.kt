package com.yakivmospan.autocompletesample.lib

/**
 * All UI interactions with [AutoCompleteViewModel] expressed as a sealed type.
 */
sealed interface AutoCompleteEvent {

    /** Fired on every keystroke in the search field. */
    data class QueryChanged(val query: String) : AutoCompleteEvent

    /** Fired when the last visible list item is reached — triggers next-page load. */
    data object LoadMore : AutoCompleteEvent

    /** Fired when the search field is cleared — resets state to [AutoCompleteState.Idle]. */
    data object Clear : AutoCompleteEvent

    /** Fired when the user explicitly submits the query (e.g. IME Search action) — re-runs the search immediately. */
    data object Search : AutoCompleteEvent
}