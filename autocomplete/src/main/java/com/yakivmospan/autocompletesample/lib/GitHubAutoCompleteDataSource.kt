package com.yakivmospan.autocompletesample.lib

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ── GitHub API DTOs ────────────────────────────────────────────────────────────

@Serializable
private data class GitHubUserSearchResponse(
    @SerialName("items") val items: List<GitHubUserDto>,
)

@Serializable
private data class GitHubUserDto(
    @SerialName("id") val id: Long,
    @SerialName("login") val login: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
private data class GitHubRepoSearchResponse(
    @SerialName("items") val items: List<GitHubRepoDto>,
)

@Serializable
private data class GitHubRepoDto(
    @SerialName("id") val id: Long,
    @SerialName("full_name") val fullName: String,
    @SerialName("owner") val owner: GitHubOwnerDto? = null,
)

@Serializable
private data class GitHubOwnerDto(
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

// ── Data source ────────────────────────────────────────────────────────────────

/**
 * [AutoCompleteDataSource] implementation that queries the GitHub Search API
 * for both users and repositories, merges the results, sorts alphabetically,
 * and returns up to [pageSize] items per page.
 *
 * @param httpClient Ktor [HttpClient] to use for requests. Defaults to a pre-configured
 *   Android client with JSON content negotiation and logging.
 * @param pageSize Number of items requested from each GitHub endpoint per page via `per_page` (default 50).
 */
class GitHubAutoCompleteDataSource(
    private val httpClient: HttpClient = defaultHttpClient(),
    private val pageSize: Int = 50,
) : AutoCompleteDataSource<GitHubItem> {

    override suspend fun search(query: String, page: Int): List<GitHubItem> = coroutineScope {
        val usersDeferred = async { fetchUsers(query, page) }
        val reposDeferred = async { fetchRepositories(query, page) }

        val users = usersDeferred.await()
        val repos = reposDeferred.await()

        (users + repos)
            .sortedBy { it.name.lowercase() }
    }

    private suspend fun fetchUsers(query: String, page: Int): List<GitHubItem.User> =
        httpClient.get("https://api.github.com/search/users") {
            parameter("q", query)
            parameter("page", page)
            parameter("per_page", pageSize)
        }.body<GitHubUserSearchResponse>().items.map { dto ->
            GitHubItem.User(
                id = dto.id,
                login = dto.login,
                avatarUrl = dto.avatarUrl,
            )
        }

    private suspend fun fetchRepositories(query: String, page: Int): List<GitHubItem.Repository> =
        httpClient.get("https://api.github.com/search/repositories") {
            parameter("q", query)
            parameter("page", page)
            parameter("per_page", pageSize)
        }.body<GitHubRepoSearchResponse>().items.map { dto ->
            GitHubItem.Repository(
                id = dto.id,
                fullName = dto.fullName,
                avatarUrl = dto.owner?.avatarUrl,
            )
        }
}

// ── Default client factory ─────────────────────────────────────────────────────

private fun defaultHttpClient() = HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
    install(Logging) {
        level = LogLevel.BODY
    }
}

