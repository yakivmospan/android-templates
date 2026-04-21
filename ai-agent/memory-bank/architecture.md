# Architecture

**Architecture style:**
Modular library architecture with clear separation of concerns. No app-level architecture pattern (no MVVM/MVI imposed on the sample app). The library itself is self-contained and follows the Dependency Inversion Principle.

---

## Generic design

All core library types are parameterised by `T` — the item type returned by the data source and rendered by the UI. This makes the component fully reusable with any data model:

```
AutoCompleteComponent<T>(viewModel, itemContent: @Composable (T) -> Unit)
       ↓ observes StateFlow<AutoCompleteState<T>>
       ↓ calls loadMore() on last-item visible
AutoCompleteViewModel<T>(dataSource: AutoCompleteDataSource<T>)
       ↓ calls suspend search(query, page): List<T>
AutoCompleteDataSource<T>   ← interface
       ↑ implemented by
GitHubAutoCompleteDataSource : AutoCompleteDataSource<GitHubItem>
```

---

## Components

### Component 1 — `AutoCompleteComponent<T>` (UI layer)
- Composable: `fun <T> AutoCompleteComponent(viewModel: AutoCompleteViewModel<T>, itemContent: @Composable (T) -> Unit, modifier: Modifier = Modifier)`.
- `itemContent` slot — callers supply item row UI including any click handling via `Modifier.clickable`.
- Observes `StateFlow<AutoCompleteState<T>>` via `collectAsStateWithLifecycle()`.
- Forwards user input to `viewModel.onQueryChanged(query)`.
- Detects when the last item in the list becomes visible (`LazyListState.isLastItemVisible`) and calls `viewModel.loadMore()`.
- Renders a bottom loading indicator when `state.isLoadingMore == true`.
- Contains no business logic — pure UI. No callbacks, no opinions on interaction.

### Component 2 — `AutoCompleteViewModel<T>` (presentation layer)
- `class AutoCompleteViewModel<T>(dataSource: AutoCompleteDataSource<T>) : ViewModel()`.
- Exposes `val state: StateFlow<AutoCompleteState<T>>`.
- Debounces query input (min 3 chars, 300 ms) using `Flow.debounce` + `flatMapLatest`; resets to page 1 on each new query.
- Cancels the previous in-flight search automatically on query change.
- Exposes `fun onQueryChanged(query: String)` — resets pagination and starts fresh search.
- Exposes `fun loadMore()` — appends next page to `Success.items`; no-op if `hasMore == false` or already loading.
- Tracks current page internally; increments on each successful `loadMore()`.
- Provides a `Factory(dataSource)` companion for non-DI instantiation.

### Component 3 — `AutoCompleteDataSource<T>` (domain interface)
- `interface AutoCompleteDataSource<T> { suspend fun search(query: String, page: Int): List<T> }`.
- `page` is 1-based. The data source returns an empty list when no more results are available.
- `pageSize` is an implementation detail — not part of the interface.

### Component 4 — `GitHubItem` (GitHub domain model)
- Sealed class specific to the GitHub implementation.
- Subtypes: `data class User(val id: Long, val login: String, val avatarUrl: String?)` and `data class Repository(val id: Long, val fullName: String, val avatarUrl: String?)`.
- Computed `val name: String` on the sealed class for alphabetical sorting.

### Component 5 — `GitHubAutoCompleteDataSource` (data layer)
- `class GitHubAutoCompleteDataSource(httpClient: HttpClient, pageSize: Int = 50) : AutoCompleteDataSource<GitHubItem>`.
- `pageSize` maps to `per_page` on both GitHub endpoints; `page` maps to the `page` query param.
- Fires `/search/users` and `/search/repositories` concurrently for the given page, merges, sorts by `name`.
- Returns empty list when both endpoints return empty results (signals `hasMore = false` to the ViewModel).

### Component 6 — `AutoCompleteState<T>` (state model)
- Sealed class:
  - `object Idle`
  - `object Loading` — initial search in progress
  - `data class Success<T>(val items: List<T>, val isLoadingMore: Boolean = false, val hasMore: Boolean = true)` — results loaded; `isLoadingMore` drives bottom spinner; `hasMore` gates further `loadMore()` calls
  - `data class Error(val message: String)`

### Component 7 — `GitHubAutoCompleteComponent` (GitHub-specific UI, default implementation)
- Composable wrapping `AutoCompleteComponent<GitHubItem>` with a built-in `GitHubItemRow` for `itemContent`.
- Signature: `fun GitHubAutoCompleteComponent(viewModel: AutoCompleteViewModel<GitHubItem>, onItemSelected: (GitHubItem) -> Unit, modifier: Modifier = Modifier)`.
- `onItemSelected` wired via `Modifier.clickable` inside the default `GitHubItemRow`.
- Renders avatar, display name, and a User/Repository type badge per row.

### Component 8 — `app` module (integration sample)
- `MainActivity` wires `GitHubAutoCompleteDataSource` → `AutoCompleteViewModel<GitHubItem>` → `GitHubAutoCompleteComponent(onItemSelected = { ... })`.

---

## Project structure
```
autocomplete-sample/
├── app/                            # Sample application module
│   └── src/main/.../MainActivity.kt
├── autocomplete/                   # Library module
│   └── src/main/.../lib/
│       ├── AutocompleteComponent.kt          # Generic Compose UI entry point <T>
│       ├── AutoCompleteViewModel.kt          # Presentation / state logic (generic <T>)
│       ├── AutoCompleteState.kt              # UI state sealed class (generic <T>)
│       ├── AutoCompleteDataSource.kt         # Domain interface (generic <T>)
│       ├── GitHubItem.kt                     # GitHub-specific domain model
│       ├── GitHubAutoCompleteDataSource.kt   # Ktor-based AutoCompleteDataSource<GitHubItem>
│       └── GitHubAutoCompleteComponent.kt    # Default GitHub UI with onItemSelected callback
└── gradle/
    └── libs.versions.toml
```

## External dependencies
| Dependency | Purpose |
|---|---|
| Jetpack Compose BOM 2024.09.00 | UI toolkit |
| androidx.lifecycle-viewmodel-ktx | `AutoCompleteViewModel` base class + `viewModelScope` |
| androidx.lifecycle-viewmodel-compose | `viewModel()` factory in Compose |
| Ktor (client-android + content-negotiation + kotlinx-serialization) | HTTP networking |
| Kotlin Coroutines | Async operations & Flow |
| Mockk | Unit test mocking |
| Compose UI Test (junit4) | UI / instrumented tests |
| JUnit 4 | Unit testing |

