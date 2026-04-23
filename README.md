# Auto complete Android Sample

<img src="showcase.gif" alt="showcase" width="400"/>

A reusable native Android autocomplete component/library that fetches matching GitHub users and repositories for a given query
string, demonstrating clean architecture, separation of concerns, and testability.
This is a Kotlin based Android project targeting Android with min 24 API.

* [/app](./app/) contains Auto Complete application sample code.

* [/autocomplete](./autocomplete/) contains Auto Complete Component library, made with Kotlin and Compose.

### Build Application

To build and run the development version of the Sample Android app, use the run configuration from the run widget
in your IDE's toolbar or build it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :assembleDebug
  ```

## Tech stack

- Kotlin
- Jetpack Compose
- Ktor for network requests (maybe we will migrate to KMP later)
- Coil for image loading
- Coroutines for async operations
- Mockk for testing
- ComposeUI Test for UI testing

## Architecture

We will avoid sample app architecture completely here (it could be a simpler linear or more complex clean architecture).

Our main focus is on the autocomplete component, so we will implement it in a way that it can be easily reused across different
screens and projects.
We will create a composable function that encapsulates all the logic and UI for the autocomplete feature, making it easy to
integrate into any screen without tight coupling to specific app architecture.

So our library must:
- Provide a clear and simple API for integration.
- Encapsulate all the logic related to fetching and displaying autocomplete results.
- Be flexible enough to allow customization of the UI and behavior without modifying the core logic.
- Provide possibility to change the data source (e.g., switch from GitHub API to another API) without affecting the UI layer,
  adhering to the principles of separation of concerns and modularity.
- Follow Dependency Inversion Principle to be easily testable and maintainable.

### Layer diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         UI Layer                                │
│                                                                 │
│  GitHubAutoCompleteComponent          AutoCompleteComponent<T>  │
│  ┌─────────────────────────┐          ┌──────────────────────┐  │
│  │ Built-in GitHubItemRow  │          │ itemContent slot     │  │
│  │ onItemSelected callback │─────────▶│ (caller-provided)    │  │
│  └─────────────────────────┘          └──────────────────────┘  │
│                  │                              │                │
│                  └──────────────┬───────────────┘                │
│                                 │ observes state / sends events  │
├─────────────────────────────────┼───────────────────────────────┤
│                   Presentation Layer                            │
│                                 ▼                               │
│              AutoCompleteViewModel<T>                           │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  StateFlow<AutoCompleteState<T>>                         │   │
│  │  ┌──────┐ ┌─────────┐ ┌───────┐ ┌─────────┐ ┌───────┐  │   │
│  │  │ Idle │ │ Loading │ │ Empty │ │ Success │ │ Error │  │   │
│  │  └──────┘ └─────────┘ └───────┘ └─────────┘ └───────┘  │   │
│  │                                                          │   │
│  │  Events: QueryChanged · Search · LoadMore · Clear        │   │
│  │  • debounce (300 ms) + distinctUntilChanged              │   │
│  │  • flatMapLatest — cancels previous in-flight search     │   │
│  │  • pagination — page tracked internally, reset on query  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                 │ suspend search(query, page)   │
├─────────────────────────────────┼───────────────────────────────┤
│                      Domain Layer                               │
│                                 ▼                               │
│              AutoCompleteDataSource<T>  ◀── interface           │
│                                 │                               │
├─────────────────────────────────┼───────────────────────────────┤
│                       Data Layer                                │
│                                 ▼                               │
│         GitHubAutoCompleteDataSource : AutoCompleteDataSource<GitHubItem>
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Ktor HttpClient                                         │   │
│  │  GET /search/users  ──┐                                  │   │
│  │                       ├─ coroutineScope async/await      │   │
│  │  GET /search/repos ───┘  merge + sort alphabetically     │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Component relationships

```
AutoCompleteDataSource<T>          (domain interface — data layer boundary)
        ▲
        │ implements
GitHubAutoCompleteDataSource       (Ktor-based, searches users + repos concurrently)
        │
        │ injected into
AutoCompleteViewModel<T>           (state machine: debounce, pagination, cancellation)
        │
        │ observed by
AutoCompleteComponent<T>           (generic Compose UI — itemContent slot for rows)
        │
        │ wrapped by
GitHubAutoCompleteComponent        (opinionated UI: GitHubItemRow + onItemSelected)
```

### State model

```
AutoCompleteState<T>
├── Idle          — field is empty / query too short (< 3 chars)
├── Loading       — first page in flight
├── Empty         — search returned no results
├── Success(items, isLoadingMore, hasMore)
│       isLoadingMore = true  → bottom spinner visible
│       hasMore      = false  → no further loadMore() calls
└── Error(message) — network or parse failure
```

---

## Examples

### 1 — GitHub autocomplete (simplest usage)

Use `rememberGitHubAutoCompleteViewModel()` to get a scoped ViewModel with a default
`GitHubAutoCompleteDataSource` and a default Ktor `HttpClient` wired automatically.

```kotlin
@Composable
fun MyScreen() {
    val viewModel = rememberGitHubAutoCompleteViewModel()

    GitHubAutoCompleteComponent(
        viewModel = viewModel,
        onItemSelected = { item ->
            when (item) {
                is GitHubItem.User       -> println("User: ${item.login}")
                is GitHubItem.Repository -> println("Repo: ${item.fullName}")
            }
        },
    )
}
```

---

### 2 — GitHub autocomplete with a custom HTTP client

Supply your own `HttpClient` to add auth headers, timeouts, or a mock engine.

```kotlin
@Composable
fun MyScreen() {
    // Build once — remember so it survives recomposition.
    val httpClient = remember {
        HttpClient(Android) {
            install(ContentNegotiation) { json() }
            defaultRequest {
                header("Authorization", "Bearer $GITHUB_TOKEN")
            }
        }
    }

    val dataSource = remember(httpClient) {
        GitHubAutoCompleteDataSource(
            httpClient = httpClient,
            pageSize   = 30,
        )
    }

    val viewModel: AutoCompleteViewModel<GitHubItem> = viewModel(
        factory = AutoCompleteViewModel.Factory(dataSource)
    )

    GitHubAutoCompleteComponent(
        viewModel      = viewModel,
        onItemSelected = { item -> /* handle selection */ },
    )
}
```

---

### 3 — Generic autocomplete with a custom data source

Implement `AutoCompleteDataSource<T>` for any backend and plug it into the generic
`AutoCompleteComponent<T>` with your own item row UI.

```kotlin
// 1. Your domain model
data class Country(val code: String, val name: String)

// 2. Your data source
class CountryDataSource : AutoCompleteDataSource<Country> {
    override suspend fun search(query: String, page: Int): List<Country> {
        // call your API / query Room / filter an in-memory list …
        return CountryApi.search(query, page)
    }
}

// 3. Wire it up in a composable
@Composable
fun CountryPickerScreen() {
    val viewModel: AutoCompleteViewModel<Country> = rememberAutoCompleteViewModel(
        dataSource = remember { CountryDataSource() }
    )

    AutoCompleteComponent(
        viewModel   = viewModel,
        itemContent = { country ->
            Text(
                text     = "${country.code}  ${country.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* handle selection */ }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        },
    )
}
```

---

### 4 — Stateless / preview-friendly overload

Both components expose a fully stateless overload that accepts plain values and an
`onEvent` lambda — ideal for Compose Previews and UI tests.

```kotlin
// Compose Preview
@Preview
@Composable
fun AutoCompleteSuccessPreview() {
    AutoCompleteComponent(
        query     = "kotlin",
        state     = AutoCompleteState.Success(
            items        = listOf("kotlin/kotlin", "kotlinx/coroutines"),
            hasMore      = true,
            isLoadingMore = false,
        ),
        onEvent   = {},
        itemContent = { item: String ->
            Text(
                text     = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        },
    )
}
```

---

### 5 — ViewModel with custom debounce / min-query-length

```kotlin
val viewModel: AutoCompleteViewModel<GitHubItem> = viewModel(
    factory = AutoCompleteViewModel.Factory(
        dataSource     = GitHubAutoCompleteDataSource(),
        minQueryLength = 2,      // start searching after 2 chars instead of 3
        debounceMillis = 500L,   // wait 500 ms before firing
    )
)
```

---

## Tradeoffs:

- Checking overall general errors, including parsing. In a real app error handling should be based on http code handling and api error parsing.
- Pagination is done purely. In real app data source should return Paginated data object with size, total count and next page
  info. In this sample we just fetch next page when user scrolls to the end of the list.
- Pagination has no error handling. At the moment we just show previous results. In real app we should show a message below previous loaded results with a retry button.
- Github Data source is just sorting new pages data and adds it to the end of existing list.
  So firt page result is sorted A-Z, and then next page sorted A-Z is started after last page Z items. With current UX where user
  scrolls to the end of the list and fetches next page, this looks bad. In real app we may want to change UI of autocomplete to
  have actual page numbers so user can jump between them and see next result sorted from the first item again, or we need an API
  that has a sorting implemented under the hood.