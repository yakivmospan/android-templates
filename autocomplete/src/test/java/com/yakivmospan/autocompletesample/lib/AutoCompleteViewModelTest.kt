package com.yakivmospan.autocompletesample.lib

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutoCompleteViewModelTest {

    // -------------------------------------------------------------------------
    // Infrastructure
    // -------------------------------------------------------------------------

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val dataSource = mockk<AutoCompleteDataSource<String>>()

    private val testMinQueryLength = 3
    private val testDebounceMillis = 300L

    private lateinit var viewModel: AutoCompleteViewModel<String>

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AutoCompleteViewModel(
            dataSource = dataSource,
            minQueryLength = testMinQueryLength,
            debounceMillis = testDebounceMillis,
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Sends a QueryChanged event and advances time past debounce + IO.
     * The leading [advanceUntilIdle] ensures [AutoCompleteViewModel.subscribeToQueryChanges]
     * has started collecting before we emit the query, so [drop(1)] only skips
     * the empty-string seed value and not the query itself.
     */
    private fun TestScope.sendQuery(query: String) {
        advanceUntilIdle() // let subscribeToQueryChanges() start and consume the seed ""
        viewModel.onEvent(AutoCompleteEvent.QueryChanged(query))
        advanceTimeBy(testDebounceMillis) // advance past debounce window
        advanceUntilIdle() // IO completes
    }

    private fun mockSuccessSearch(vararg pages: List<String>) {
        var callCount = 0
        coEvery { dataSource.search(any(), any()) } answers {
            pages.getOrElse(callCount++) { emptyList() }
        }
    }

    private fun mockErrorSearch(error: Throwable = RuntimeException("network error")) {
        coEvery { dataSource.search(any(), any()) } throws error
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    fun `when query meets min length then emits Loading then Success`() = testScope.runTest {
        // Given
        mockSuccessSearch(listOf("kotlin"))
        val states = mutableListOf<AutoCompleteState<String>>()
        val job = launch { viewModel.state.collect { states.add(it) } }
        advanceUntilIdle() // let subscribeToQueryChanges() start; collects seed "" (dropped)

        // When
        viewModel.onEvent(AutoCompleteEvent.QueryChanged("kot"))
        advanceTimeBy(testDebounceMillis) // past debounce → onQueryChange fires, Loading emitted
        advanceUntilIdle()               // IO completes → Success emitted

        // Then — exact 3-step sequence: Idle (initial) → Loading → Success
        assertEquals(3, states.size)
        assertEquals(AutoCompleteState.Idle, states[0])
        assertEquals(AutoCompleteState.Loading, states[1])
        assertEquals(AutoCompleteState.Success(items = listOf("kotlin"), isLoadingMore = false, hasMore = true), states[2])
        job.cancel()
    }

    @Test
    fun `when search returns items then state is Success with correct items`() = testScope.runTest {
        // Given
        mockSuccessSearch(listOf("kotlin", "java"))

        // When
        sendQuery("kot")

        // Then
        assertEquals(
            AutoCompleteState.Success(items = listOf("kotlin", "java"), isLoadingMore = false, hasMore = true),
            viewModel.state.value,
        )
    }

    @Test
    fun `when loadMore is triggered then appends next page items`() = testScope.runTest {
        // Given
        mockSuccessSearch(listOf("alpha"), listOf("beta"))
        sendQuery("abc")

        // When
        viewModel.onEvent(AutoCompleteEvent.LoadMore)
        advanceUntilIdle()

        // Then
        assertEquals(
            AutoCompleteState.Success(items = listOf("alpha", "beta"), isLoadingMore = false, hasMore = true),
            viewModel.state.value,
        )
    }

    @Test
    fun `when loadMore is triggered then isLoadingMore is true during fetch`() = testScope.runTest {
        // Given — collector must start before sendQuery so it captures all state transitions
        mockSuccessSearch(listOf("alpha"), listOf("beta"))
        val states = mutableListOf<AutoCompleteState<String>>()
        val job = launch { viewModel.state.collect { states.add(it) } }
        sendQuery("abc")

        // When
        viewModel.onEvent(AutoCompleteEvent.LoadMore)
        advanceUntilIdle()

        // Then — exact sequence: Idle → Loading → Success → Success(isLoadingMore=true) → Success(full)
        assertEquals(5, states.size)
        assertEquals(AutoCompleteState.Idle, states[0])
        assertEquals(AutoCompleteState.Loading, states[1])
        assertEquals(AutoCompleteState.Success(items = listOf("alpha"), isLoadingMore = false, hasMore = true), states[2])
        assertEquals(AutoCompleteState.Success(items = listOf("alpha"), isLoadingMore = true, hasMore = true), states[3])
        assertEquals(AutoCompleteState.Success(items = listOf("alpha", "beta"), isLoadingMore = false, hasMore = true), states[4])
        job.cancel()
    }

    @Test
    fun `when last page is returned then hasMore is false`() = testScope.runTest {
        // Given — second page returns empty list signalling end of data
        mockSuccessSearch(listOf("alpha"), emptyList())
        sendQuery("abc")

        // When
        viewModel.onEvent(AutoCompleteEvent.LoadMore)
        advanceUntilIdle()

        // Then
        assertEquals(
            AutoCompleteState.Success(items = listOf("alpha"), isLoadingMore = false, hasMore = false),
            viewModel.state.value,
        )
    }

    @Test
    fun `when clear event sent then state resets to Idle`() = testScope.runTest {
        // Given
        mockSuccessSearch(listOf("kotlin"))
        sendQuery("kot")

        // When
        viewModel.onEvent(AutoCompleteEvent.Clear)
        advanceUntilIdle()

        // Then
        assertEquals(AutoCompleteState.Idle, viewModel.state.value)
    }

    @Test
    fun `when search event sent then re-runs search immediately`() = testScope.runTest {
        // Given — first search resolves, then a second call is expected after Search event
        mockSuccessSearch(listOf("kotlin"), listOf("kotlin", "java"))
        sendQuery("kot")

        // When
        viewModel.onEvent(AutoCompleteEvent.Search)
        advanceUntilIdle()

        // Then — second search result replaces first
        assertEquals(
            AutoCompleteState.Success(items = listOf("kotlin", "java"), isLoadingMore = false, hasMore = true),
            viewModel.state.value,
        )
    }

    // -------------------------------------------------------------------------
    // Error conditions
    // -------------------------------------------------------------------------

    @Test
    fun `when data source throws then state is Error with message`() = testScope.runTest {
        // Given
        val errorMessage = "network error"
        mockErrorSearch(RuntimeException(errorMessage))

        // When
        sendQuery("kot")

        // Then
        assertEquals(AutoCompleteState.Error(errorMessage), viewModel.state.value)
    }

    @Test
    fun `when loadMore data source throws then previous Success state is restored`() = testScope.runTest {
        // Given — first page succeeds, second page throws
        val previousSuccess = AutoCompleteState.Success(items = listOf("alpha"), isLoadingMore = false, hasMore = true)
        mockSuccessSearch(listOf("alpha"))
        sendQuery("abc")
        mockErrorSearch() // override mock — next call throws

        // When
        viewModel.onEvent(AutoCompleteEvent.LoadMore)
        advanceUntilIdle()

        // Then — silent failure: previous Success is restored with isLoadingMore=false
        assertEquals(previousSuccess, viewModel.state.value)
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    fun `when query is shorter than min length then state is Idle`() = testScope.runTest {
        // Pattern B — homogeneous inputs, same assertion for each
        val shortQueries = listOf("", "a", "ab")
        advanceUntilIdle() // start subscriber
        for (query in shortQueries) {
            // Given / When
            viewModel.onEvent(AutoCompleteEvent.QueryChanged(query))
            advanceTimeBy(testDebounceMillis)
            advanceUntilIdle()

            // Then
            assertEquals(AutoCompleteState.Idle, viewModel.state.value)
        }
    }

    @Test
    fun `when query changes rapidly then only last query triggers search`() = testScope.runTest {
        // Given
        mockSuccessSearch(listOf("result"))
        advanceUntilIdle() // start subscriber

        // When — 3 queries fired within the debounce window; only the last should pass
        viewModel.onEvent(AutoCompleteEvent.QueryChanged("abc"))
        viewModel.onEvent(AutoCompleteEvent.QueryChanged("abcd"))
        viewModel.onEvent(AutoCompleteEvent.QueryChanged("abcde"))
        advanceTimeBy(testDebounceMillis)
        advanceUntilIdle()

        // Then — data source called exactly once with the last query only
        coVerify(exactly = 1) { dataSource.search("abcde", 1) }
        assertEquals(
            AutoCompleteState.Success(items = listOf("result"), isLoadingMore = false, hasMore = true),
            viewModel.state.value,
        )
    }

    @Test
    fun `when query changes then loadMore job is cancelled`() = testScope.runTest {
        // Given — per-query stubs so the result is deterministic regardless of loadMore timing
        coEvery { dataSource.search("abc", any()) } returns listOf("page1")
        coEvery { dataSource.search("xyz", 1) } returns listOf("new-query-result")
        sendQuery("abc") // state = Success(["page1"])

        // When — trigger loadMore then immediately change query before debounce fires
        viewModel.onEvent(AutoCompleteEvent.LoadMore)
        viewModel.onEvent(AutoCompleteEvent.QueryChanged("xyz"))
        advanceTimeBy(testDebounceMillis) // debounce fires → cancelLoadMore → Loading for new query
        advanceUntilIdle()               // new query IO runs

        // Then — final state is the new query's result; old loadMore result is not visible
        assertEquals(
            AutoCompleteState.Success(items = listOf("new-query-result"), isLoadingMore = false, hasMore = true),
            viewModel.state.value,
        )
    }

    @Test
    fun `when data source returns empty list then state is Empty`() = testScope.runTest {
        // Given
        mockSuccessSearch(emptyList())

        // When
        sendQuery("abc")

        // Then
        assertEquals(AutoCompleteState.Empty, viewModel.state.value)
    }

    @Test
    fun `when loadMore is called but hasMore is false then no additional fetch`() = testScope.runTest {
        // Given — page 2 is empty → hasMore=false
        mockSuccessSearch(listOf("alpha"), emptyList())
        sendQuery("abc")
        viewModel.onEvent(AutoCompleteEvent.LoadMore)
        advanceUntilIdle() // state = Success(["alpha"], hasMore=false)

        // When — attempt another loadMore; guard should reject it
        viewModel.onEvent(AutoCompleteEvent.LoadMore)
        advanceUntilIdle()

        // Then — state unchanged; total search calls = 2 (initial + one page), never a 3rd
        coVerify(exactly = 2) { dataSource.search(any(), any()) }
        assertEquals(
            AutoCompleteState.Success(items = listOf("alpha"), isLoadingMore = false, hasMore = false),
            viewModel.state.value,
        )
    }

    @Test
    fun `when loadMore is called while already loading more then no additional fetch`() = testScope.runTest {
        // Given
        mockSuccessSearch(listOf("alpha"), listOf("beta"))
        sendQuery("abc") // callCount = 1

        // When — fire LoadMore twice before IO runs; second call must be a no-op
        viewModel.onEvent(AutoCompleteEvent.LoadMore) // sets isLoadingMore=true, schedules IO
        viewModel.onEvent(AutoCompleteEvent.LoadMore) // guard: isLoadingMore == true → return
        advanceUntilIdle()

        // Then — search called exactly twice (initial + one loadMore, not two)
        coVerify(exactly = 2) { dataSource.search(any(), any()) }
        assertEquals(
            AutoCompleteState.Success(items = listOf("alpha", "beta"), isLoadingMore = false, hasMore = true),
            viewModel.state.value,
        )
    }

    @Test
    fun `when query changes then page resets to 1`() = testScope.runTest {
        // Given — load page 1 then page 2 for first query
        mockSuccessSearch(listOf("alpha"), listOf("beta"), listOf("gamma"))
        sendQuery("abc")                           // search("abc", 1) → ["alpha"], callCount=1
        viewModel.onEvent(AutoCompleteEvent.LoadMore)
        advanceUntilIdle()                         // search("abc", 2) → ["beta"], callCount=2

        // When — new query must reset page counter to 1
        sendQuery("xyz")                           // search("xyz", 1) → ["gamma"], callCount=3

        // Then — new query requested page 1, not page 3
        coVerify(exactly = 1) { dataSource.search("xyz", 1) }
        assertEquals(
            AutoCompleteState.Success(items = listOf("gamma"), isLoadingMore = false, hasMore = true),
            viewModel.state.value,
        )
    }
}
