package com.yakivmospan.autocompletesample.lib

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AutoCompleteComponentTest {

    @get:Rule
    val composeRule = createComposeRule()

    // -------------------------------------------------------------------------
    // Infrastructure
    // -------------------------------------------------------------------------

    private val mockViewModel: AutoCompleteViewModel<String> = mockk(relaxed = true)

    // -------------------------------------------------------------------------
    // Reusable test data
    // -------------------------------------------------------------------------

    private val defaultItems = listOf("kotlin/kotlin", "kotlinx/coroutines", "JetBrains/kotlin")

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
        state: AutoCompleteState<String> = AutoCompleteState.Idle,
        query: String = "",
    ) {
        every { mockViewModel.state } returns MutableStateFlow(state)
        every { mockViewModel.query } returns MutableStateFlow(query)
        composeRule.setContent {
            AutoCompleteComponent(
                viewModel = mockViewModel,
                itemContent = { item ->
                    androidx.compose.material3.Text(text = item)
                },
            )
        }
    }

    // -------------------------------------------------------------------------
    // State rendering
    // -------------------------------------------------------------------------

    @Test
    fun when_state_is_Idle_then_stateless_content_is_shown_and_no_spinner_or_message() {
        // Given — ViewModel provides Idle state
        launchComponent(state = AutoCompleteState.Idle)

        // When
        composeRule.waitForIdle()

        // Then — stateless overload is delegated to
        composeRule.onNodeWithTag(TAG_AUTOCOMPLETE_STATELESS).assertIsDisplayed()
        // And — no loading spinner
        composeRule.onNodeWithTag(TAG_LOADING_INDICATOR).assertDoesNotExist()
        // And — no empty/error message
        composeRule.onNodeWithText("No results found").assertDoesNotExist()
    }

    @Test
    fun when_state_is_Loading_then_stateless_content_is_shown_and_progress_indicator_is_visible() {
        // Given — ViewModel provides Loading state
        launchComponent(state = AutoCompleteState.Loading)

        // When
        composeRule.waitForIdle()

        // Then
        composeRule.onNodeWithTag(TAG_AUTOCOMPLETE_STATELESS).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_LOADING_INDICATOR).assertIsDisplayed()
    }

    @Test
    fun when_state_is_Loading_then_items_list_is_not_shown() {
        // Given — ViewModel provides Loading state with a known item text that must not appear
        launchComponent(state = AutoCompleteState.Loading)

        // When
        composeRule.waitForIdle()

        // Then — stateless overload rendered, loading spinner shown, no item content visible
        composeRule.onNodeWithTag(TAG_AUTOCOMPLETE_STATELESS).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_LOADING_INDICATOR).assertIsDisplayed()
        composeRule.onNodeWithText("kotlin/kotlin").assertDoesNotExist()
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

    @Test
    fun when_state_is_Success_then_stateless_content_is_shown_and_items_are_visible() {
        // Given — ViewModel provides Success state with items
        launchComponent(state = AutoCompleteState.Success(items = defaultItems))

        // When
        composeRule.waitForIdle()

        // Then
        composeRule.onNodeWithTag(TAG_AUTOCOMPLETE_STATELESS).assertIsDisplayed()
        defaultItems.forEach { item ->
            composeRule.onNodeWithText(item).assertIsDisplayed()
        }
    }

    @Test
    fun when_state_is_Success_with_isLoadingMore_true_then_loading_more_spinner_is_shown() {
        // Given — ViewModel provides Success state with isLoadingMore = true
        launchComponent(
            state = AutoCompleteState.Success(
                items = defaultItems,
                isLoadingMore = true,
            )
        )

        // When
        composeRule.waitForIdle()

        // Then
        composeRule.onNodeWithTag(TAG_AUTOCOMPLETE_STATELESS).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Loading more…").assertIsDisplayed()
    }

    @Test
    fun when_state_is_Success_with_isLoadingMore_false_then_loading_more_spinner_is_not_shown() {
        // Given — ViewModel provides Success state with isLoadingMore = false
        launchComponent(
            state = AutoCompleteState.Success(
                items = defaultItems,
                isLoadingMore = false,
            )
        )

        // When
        composeRule.waitForIdle()

        // Then
        composeRule.onNodeWithTag(TAG_AUTOCOMPLETE_STATELESS).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Loading more…").assertDoesNotExist()
    }

    // -------------------------------------------------------------------------
    // Conditional visibility — query / clear button
    // -------------------------------------------------------------------------

    @Test
    fun when_query_is_empty_then_clear_button_is_not_shown() {
        // Given — ViewModel provides empty query
        launchComponent(query = "")

        // When
        composeRule.waitForIdle()

        // Then
        composeRule.onNodeWithTag(TAG_AUTOCOMPLETE_STATELESS).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clear search").assertDoesNotExist()
    }

    @Test
    fun when_query_is_not_empty_then_clear_button_is_shown() {
        // Given — ViewModel provides non-empty query
        launchComponent(query = "kotlin")

        // When
        composeRule.waitForIdle()

        // Then
        composeRule.onNodeWithTag(TAG_AUTOCOMPLETE_STATELESS).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clear search").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // Interactions
    // -------------------------------------------------------------------------

    @Test
    fun when_user_types_text_then_QueryChanged_event_is_fired_on_ViewModel() {
        // Given
        launchComponent()
        composeRule.waitForIdle()

        // When
        composeRule.onNodeWithText("Search").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Search").performTextInput("kotlin")
        composeRule.waitForIdle()

        // Then
        verify { mockViewModel.onEvent(AutoCompleteEvent.QueryChanged("kotlin")) }
    }

    @Test
    fun when_user_clicks_clear_button_then_Clear_event_is_fired_on_ViewModel() {
        // Given
        launchComponent(query = "kotlin")
        composeRule.waitForIdle()

        // When
        composeRule.onNodeWithContentDescription("Clear search").performClick()
        composeRule.waitForIdle()

        // Then
        verify { mockViewModel.onEvent(AutoCompleteEvent.Clear) }
    }

    @Test
    fun when_user_submits_search_then_Search_event_is_fired_on_ViewModel() {
        // Given
        launchComponent(query = "kotlin")
        composeRule.waitForIdle()

        // When
        composeRule.onNodeWithText("Search").performImeAction()
        composeRule.waitForIdle()

        // Then
        verify { mockViewModel.onEvent(AutoCompleteEvent.Search) }
    }
}
