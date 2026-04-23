package com.yakivmospan.autocompletesample.lib

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubAutoCompleteDataSourceTest {

    // -------------------------------------------------------------------------
    // Infrastructure
    // -------------------------------------------------------------------------

    /** Minimal valid GitHub user search JSON response. */
    private val usersJson = """{"items":[{"id":1,"login":"apple","avatar_url":"https://avatar/apple"}]}"""

    /** Minimal valid GitHub repo search JSON response. */
    private val reposJson = """{"items":[{"id":2,"full_name":"beta/repo","owner":{"avatar_url":"https://avatar/beta"}}]}"""

    /** Empty response for both endpoints. */
    private val emptyJson = """{"items":[]}"""

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a [GitHubAutoCompleteDataSource] backed by a [MockEngine].
     * [handler] receives the request URL string and returns a pair of (body, statusCode).
     */
    private fun buildDataSource(
        pageSize: Int = 10,
        handler: (url: String) -> Pair<String, HttpStatusCode>,
    ): GitHubAutoCompleteDataSource {
        val engine = MockEngine { request ->
            val (body, status) = handler(request.url.toString())
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return GitHubAutoCompleteDataSource(httpClient = client, pageSize = pageSize)
    }

    /** Handler that returns [usersJson] for /users and [reposJson] for /repositories. */
    private fun happyPathHandler(url: String): Pair<String, HttpStatusCode> = when {
        url.contains("search/users") -> usersJson to HttpStatusCode.OK
        url.contains("search/repositories") -> reposJson to HttpStatusCode.OK
        else -> "" to HttpStatusCode.NotFound
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    fun `when search is called then users and repositories are fetched and merged`() = runTest {
        // Given
        val ds = buildDataSource(handler = ::happyPathHandler)

        // When
        val results = ds.search("apple", page = 1)

        // Then — one user + one repo merged into a single list
        assertEquals(2, results.size)
        assertEquals(
            GitHubItem.User(id = 1, login = "apple", avatarUrl = "https://avatar/apple"),
            results.first { it is GitHubItem.User },
        )
        assertEquals(
            GitHubItem.Repository(id = 2, fullName = "beta/repo", avatarUrl = "https://avatar/beta"),
            results.first { it is GitHubItem.Repository },
        )
    }

    @Test
    fun `when search is called then results are sorted alphabetically by name`() = runTest {
        // Given — "apple" (user login) sorts before "beta/repo" (repo fullName)
        val ds = buildDataSource(handler = ::happyPathHandler)

        // When
        val results = ds.search("apple", page = 1)

        // Then — alphabetical order: "apple" < "beta/repo"
        assertEquals(2, results.size)
        assertEquals(GitHubItem.User(id = 1, login = "apple", avatarUrl = "https://avatar/apple"), results[0])
        assertEquals(GitHubItem.Repository(id = 2, fullName = "beta/repo", avatarUrl = "https://avatar/beta"), results[1])
    }

    @Test
    fun `when search is called then correct query page and per_page params are sent`() = runTest {
        // Given — capture every request URL
        val capturedUrls = mutableListOf<String>()
        val ds = buildDataSource(pageSize = 25) { url ->
            capturedUrls += url
            happyPathHandler(url)
        }

        // When
        ds.search("kotlin", page = 3)

        // Then — both endpoints received q=kotlin, page=3, per_page=25
        assertEquals(2, capturedUrls.size)
        capturedUrls.forEach { url ->
            assertTrue(url.contains("q=kotlin"))
            assertTrue(url.contains("page=3"))
            assertTrue(url.contains("per_page=25"))
        }
    }

    // -------------------------------------------------------------------------
    // Error conditions
    // -------------------------------------------------------------------------

    @Test
    fun `when both endpoints fail then throws exception`() = runTest {
        // Given — both endpoints return a non-parseable body causing serialization failure
        val ds = buildDataSource { _ -> "" to HttpStatusCode.InternalServerError }

        // When
        var thrownException: Exception? = null
        try {
            ds.search("kotlin", page = 1)
        } catch (e: Exception) {
            thrownException = e
        }

        // Then — data source rethrows a combined exception
        assertTrue(thrownException != null)
        assertEquals("Unable to reach data.", thrownException!!.message)
    }

    @Test
    fun `when users endpoint fails then returns repositories only`() = runTest {
        // Given
        val ds = buildDataSource { url ->
            when {
                url.contains("search/users") -> "" to HttpStatusCode.InternalServerError
                else -> reposJson to HttpStatusCode.OK
            }
        }

        // When
        val results = ds.search("kotlin", page = 1)

        // Then — only the repository item is present
        assertEquals(1, results.size)
        assertEquals(
            GitHubItem.Repository(id = 2, fullName = "beta/repo", avatarUrl = "https://avatar/beta"),
            results[0],
        )
    }

    @Test
    fun `when repositories endpoint fails then returns users only`() = runTest {
        // Given
        val ds = buildDataSource { url ->
            when {
                url.contains("search/repositories") -> "" to HttpStatusCode.InternalServerError
                else -> usersJson to HttpStatusCode.OK
            }
        }

        // When
        val results = ds.search("kotlin", page = 1)

        // Then — only the user item is present
        assertEquals(1, results.size)
        assertEquals(
            GitHubItem.User(id = 1, login = "apple", avatarUrl = "https://avatar/apple"),
            results[0],
        )
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    fun `when both endpoints return empty lists then returns empty list`() = runTest {
        // Given
        val ds = buildDataSource { _ -> emptyJson to HttpStatusCode.OK }

        // When
        val results = ds.search("kotlin", page = 1)

        // Then
        assertEquals(emptyList<GitHubItem>(), results)
    }

    @Test
    fun `when pageSize is customized then per_page param reflects it`() = runTest {
        // Given
        val customPageSize = 5
        val capturedUrls = mutableListOf<String>()
        val ds = buildDataSource(pageSize = customPageSize) { url ->
            capturedUrls += url
            happyPathHandler(url)
        }

        // When
        ds.search("kotlin", page = 1)

        // Then — per_page on both endpoints equals the custom page size
        assertEquals(2, capturedUrls.size)
        capturedUrls.forEach { url ->
            assertTrue(url.contains("per_page=$customPageSize"))
        }
    }
}
