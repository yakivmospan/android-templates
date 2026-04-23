package com.yakivmospan.autocompletesample.lib

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GitHubAutoCompleteComponentTest {

    @get:Rule
    val composeRule = createComposeRule()

    // -------------------------------------------------------------------------
    // Infrastructure
    // -------------------------------------------------------------------------

    private val mockViewModel: AutoCompleteViewModel<GitHubItem> = mockk(relaxed = true)

    // -------------------------------------------------------------------------
    // Reusable test data
    // -------------------------------------------------------------------------

    private val userItem = GitHubItem.User(id = 1L, login = "torvalds", avatarUrl = null)
    private val repoItem = GitHubItem.Repository(id = 2L, fullName = "kotlin/kotlin", avatarUrl = null)

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Before
    fun setup() {
        every { mockViewModel.state } returns MutableStateFlow(AutoCompleteState.Idle)
        every { mockViewModel.query } returns MutableStateFlow("")
    }

    @After
    fun tearDown() {
        io.mockk.clearAllMocks()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun launchComponent(
        state: AutoCompleteState<GitHubItem> = AutoCompleteState.Idle,
        query: String = "",
        onItemSelected: (GitHubItem) -> Unit = {},
    ) {
        every { mockViewModel.state } returns MutableStateFlow(state)
        every { mockViewModel.query } returns MutableStateFlow(query)
        composeRule.setContent {
            GitHubAutoCompleteComponent(
                viewModel = mockViewModel,
                onItemSelected = onItemSelected,
            )
        }
    }

    // -------------------------------------------------------------------------
    // State rendering
    // -------------------------------------------------------------------------

    @Test
    fun when_state_is_Success_with_User_item_then_stateless_content_is_shown_and_user_name_is_visible() {
        // Given — ViewModel provides Success state with a User item
        launchComponent(state = AutoCompleteState.Success(items = listOf(userItem)))

        // When
        composeRule.waitForIdle()

        // Then — stateless overload is delegated to
        composeRule.onNodeWithTag(TAG_AUTOCOMPLETE_STATELESS).assertIsDisplayed()
        // And — GitHub-specific row elements: avatar, login name, type label
        composeRule.onNodeWithContentDescription("torvalds avatar").assertIsDisplayed()
        composeRule.onNodeWithText("torvalds").assertIsDisplayed()
        composeRule.onNodeWithText("User").assertIsDisplayed()
    }

    @Test
    fun when_state_is_Success_with_Repository_item_then_stateless_content_is_shown_and_repo_name_is_visible() {
        // Given — ViewModel provides Success state with a Repository item
        launchComponent(state = AutoCompleteState.Success(items = listOf(repoItem)))

        // When
        composeRule.waitForIdle()

        // Then — stateless overload is delegated to
        composeRule.onNodeWithTag(TAG_AUTOCOMPLETE_STATELESS).assertIsDisplayed()
        // And — GitHub-specific row elements: avatar, repo full name, type label
        composeRule.onNodeWithContentDescription("kotlin/kotlin avatar").assertIsDisplayed()
        composeRule.onNodeWithText("kotlin/kotlin").assertIsDisplayed()
        composeRule.onNodeWithText("Repository").assertIsDisplayed()
    }

    @Test
    fun when_state_is_Empty_then_stateless_content_is_shown_and_empty_message_is_visible() {
        // Given — ViewModel provides Empty state
        launchComponent(state = AutoCompleteState.Empty)

        // When
        composeRule.waitForIdle()

        // Then
        composeRule.onNodeWithTag(TAG_AUTOCOMPLETE_STATELESS).assertIsDisplayed()
        composeRule.onNodeWithText("No results found").assertIsDisplayed()
    }

    @Test
    fun when_state_is_Error_then_stateless_content_is_shown_and_error_message_is_visible() {
        // Given — ViewModel provides Error state
        launchComponent(state = AutoCompleteState.Error("Network unavailable"))

        // When
        composeRule.waitForIdle()

        // Then
        composeRule.onNodeWithTag(TAG_AUTOCOMPLETE_STATELESS).assertIsDisplayed()
        composeRule.onNodeWithText("Something went wrong: Network unavailable").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // Interactions
    // -------------------------------------------------------------------------

    @Test
    fun when_user_taps_User_item_then_onItemSelected_is_called_with_that_item() {
        // Given
        val selectedItems = mutableListOf<GitHubItem>()
        launchComponent(
            state = AutoCompleteState.Success(items = listOf(userItem)),
            onItemSelected = { selectedItems += it },
        )
        composeRule.waitForIdle()

        // When
        composeRule.onNodeWithText("torvalds").performClick()
        composeRule.waitForIdle()

        // Then
        assert(selectedItems == listOf(userItem))
    }

    @Test
    fun when_user_taps_Repository_item_then_onItemSelected_is_called_with_that_item() {
        // Given
        val selectedItems = mutableListOf<GitHubItem>()
        launchComponent(
            state = AutoCompleteState.Success(items = listOf(repoItem)),
            onItemSelected = { selectedItems += it },
        )
        composeRule.waitForIdle()

        // When
        composeRule.onNodeWithText("kotlin/kotlin").performClick()
        composeRule.waitForIdle()

        // Then
        assert(selectedItems == listOf(repoItem))
    }
}
