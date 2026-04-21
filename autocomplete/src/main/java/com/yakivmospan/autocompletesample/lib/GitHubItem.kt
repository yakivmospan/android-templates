package com.yakivmospan.autocompletesample.lib

/**
 * Domain model representing a single GitHub autocomplete result.
 * Used as the item type for [GitHubAutoCompleteDataSource] and [GitHubAutoCompleteComponent].
 */
sealed class GitHubItem {

    /** Display name used for alphabetical sorting and UI rendering. */
    abstract val name: String

    /**
     * A GitHub user / organisation profile.
     *
     * @property id GitHub user id.
     * @property login GitHub login / username.
     * @property avatarUrl URL of the user's avatar image, or null if unavailable.
     */
    data class User(
        val id: Long,
        val login: String,
        val avatarUrl: String?,
    ) : GitHubItem() {
        override val name: String get() = login
    }

    /**
     * A GitHub repository.
     *
     * @property id GitHub repository id.
     * @property fullName Full repository name in "owner/repo" format.
     * @property avatarUrl URL of the owner's avatar image, or null if unavailable.
     */
    data class Repository(
        val id: Long,
        val fullName: String,
        val avatarUrl: String?,
    ) : GitHubItem() {
        override val name: String get() = fullName
    }
}