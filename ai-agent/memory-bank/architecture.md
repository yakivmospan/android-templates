# Architecture

**Architecture style:**
Modular library architecture with clear separation of concerns. No app-level architecture pattern (no MVVM/MVI imposed on the sample app). The library itself is self-contained and follows the Dependency Inversion Principle.

---

## Components

### Component 1 — `AutoCompleteComponent` (UI layer)
- Jetpack Compose composable function (`AutoCompleteComponent`) in `autocomplete` module.
- Accepts an `AutoCompleteViewModel` (created via `viewModel()` factory or passed in for testing).
- Observes `AutoCompleteState` from the ViewModel via `collectAsStateWithLifecycle()`.
- Forwards user input to `viewModel.onQueryChanged(query)`.
- Renders: search text field, loading indicator, result list, empty state, error state.
- Contains no business logic — pure UI.

### Component 2 — `AutoCompleteViewModel` (presentation layer)
- Plain Kotlin class (extends `androidx.lifecycle.ViewModel`) living in the `autocomplete` module.
- Holds and exposes UI state as `StateFlow<AutoCompleteState>` (sealed class: `Idle`, `Loading`, `Success`, `Error`).
- Accepts an `AutoCompleteDataSource` via constructor injection (Dependency Inversion).
- Debounces query input (min 3 chars) using a `Flow` + `debounce` operator.
- Cancels the previous in-flight search automatically via `collectLatest` / `flatMapLatest`.
- Exposes a single `onQueryChanged(query: String)` entry point for the UI.

### Component 3 — `AutoCompleteDataSource` (domain interface)
- Kotlin interface defining `suspend fun search(query: String): List<AutoCompleteItem>`.
- Abstracts the underlying data provider; callers can swap GitHub API for any other source.

### Component 4 — `GitHubAutoCompleteDataSource` (data layer)
- Default implementation of `AutoCompleteDataSource`.
- Uses **Ktor** HTTP client to call GitHub Search API (`/search/users` + `/search/repositories`).
- Fires both requests concurrently (`async`/`await`), merges, sorts alphabetically, limits to 50 results.

### Component 5 — `AutoCompleteItem` (domain model)
- Sealed class / data class representing a search result item.
- Fields: `id`, `name` (display name), `type` (User | Repository), optional `avatarUrl`.

### Component 6 — `app` module (integration sample)
- `MainActivity` embeds `AutoCompleteComponent` using the default `GitHubAutoCompleteDataSource`.
- Demonstrates minimal integration boilerplate.

---

## Project structure
```
autocomplete-sample/
├── app/                            # Sample application module
│   └── src/main/.../MainActivity.kt
├── autocomplete/                   # Library module
│   └── src/main/.../lib/
│       ├── AutocompleteComponent.kt          # Compose UI entry point
│       ├── AutoCompleteViewModel.kt          # Presentation / state logic
│       ├── AutoCompleteState.kt              # UI state sealed class
│       ├── AutoCompleteDataSource.kt         # Domain interface
│       ├── AutoCompleteItem.kt               # Domain model
│       └── GitHubAutoCompleteDataSource.kt   # Ktor-based implementation
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

