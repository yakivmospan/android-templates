# Project Brief

**Project name:** AutoComplete Sample

**Project Goal:**
Create a reusable native Android autocomplete component/library that fetches matching GitHub users and repositories for a given query string, demonstrating clean architecture, separation of concerns, and testability.

**Core requirements:**
- Minimum 3 characters to trigger search
- Fetch GitHub users and repositories simultaneously
- Combine and sort results alphabetically (by repository name / profile login)
- Limit results to 50 items per request
- Show visual feedback for loading, empty, and error states
- Handle rapid input changes (debounce / cancellation)
- Reusable component — not hardcoded to a single screen
- Meaningful unit and UI tests

**Coding style:**
- Kotlin idiomatic style
- Jetpack Compose for UI
- Coroutines & Flow for async/reactive logic
- Dependency Inversion Principle — data source is abstracted behind interfaces
- No existing autocomplete library used

**Timeline:** MVP implementation — focused on minimal but complete, fully functional solution.

# Product Context

**Purpose:**
Provide a plug-and-play Jetpack Compose autocomplete component backed by the GitHub Search API, packaged as a standalone Android library module (`autocomplete`). The sample `app` module demonstrates real-world integration.

**Target users:**
Android developers who need to embed a search/autocomplete UI in their apps and want a reference implementation that is easy to customise and extend.

**Problem statement:**
Building a well-behaved autocomplete widget from scratch is repetitive and error-prone: developers have to handle debounce, concurrent request cancellation, combined data sources, alphabetical merging, and multiple UI states. This library solves all of that in one reusable composable.

**User experience:**
- User types in a search field; results begin appearing after 3 characters.
- A loading indicator is shown while the network request is in flight.
- Results are shown as a combined, alphabetically-sorted list of GitHub users and repositories.
- If no results are found an empty-state message is displayed.
- If a network error occurs an error-state message is displayed.
- Rapid typing cancels the previous in-flight search so only the latest query is resolved.

# Tech Context

**Primary language:** Kotlin

**Runtime:** Android (min SDK 24 / API 24, target SDK 36, compile SDK 36)

**Framework:** Jetpack Compose (Material 3)

**Key libraries:**
| Library | Version | Role |
|---|---|---|
| Kotlin | 2.0.21 | Language |
| Jetpack Compose BOM | 2024.09.00 | UI |
| androidx.activity-compose | 1.13.0 | Compose host |
| androidx.lifecycle-runtime-ktx | 2.10.0 | Lifecycle integration |
| Ktor (to be added) | latest stable | HTTP client for GitHub API |
| kotlinx.serialization (to be added) | latest stable | JSON parsing |
| Kotlin Coroutines (to be added) | latest stable | Async operations |
| Mockk (to be added) | latest stable | Mocking in tests |
| Compose UI Test junit4 | (via BOM) | UI testing |
| JUnit 4 | 4.13.2 | Unit testing |
| Espresso | 3.7.0 | Instrumented tests |

**DB / Storage:** None (all data is fetched live from GitHub API; no local persistence required)

**Network:** GitHub REST Search API
- `GET https://api.github.com/search/users?q={query}&per_page=50`
- `GET https://api.github.com/search/repositories?q={query}&per_page=50`
- Both requests fired concurrently; results merged and sorted.

**Build tools:**
- Gradle (Kotlin DSL) with version catalog (`libs.versions.toml`)
- AGP 9.0.1
- Android Gradle Plugin wrapper

**Development approach:**
- Test-driven where practical — unit tests for data source logic (Mockk), UI tests for component states (Compose UI Test).
- Incremental implementation: domain model → data source interface → Ktor implementation → Compose UI → tests.


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

# Active Context

**Current focus:**
Initial implementation of the `autocomplete` library module — defining the domain model, data source interface, Ktor-based GitHub implementation, and the Compose `AutoCompleteComponent`.

**Recent decisions:**
- Library module (`autocomplete`) is completely self-contained; the `app` module only integrates it as a dependency.
- `AutoCompleteDataSource` interface is the main extension point — allows swapping GitHub API for any other source.
- Minimum 3 characters before triggering a search (debounce to be applied inside the component).
- Results are merged from users + repositories, sorted alphabetically, capped at 50.
- Ktor chosen for networking (KMP-ready for future migration).
- Mockk chosen for mocking in unit tests.

**Open questions:**
- Should `AutoCompleteItem` be a sealed class with `User` / `Repository` subtypes, or a flat data class with a `type` field?
- Authentication for GitHub API? (Public API has rate limiting; a token header may be needed.)
- Should the component expose a callback when an item is selected?

**Next steps (phased):**
- Phase 1 — Dependency setup: Ktor, coroutines, serialization, lifecycle-viewmodel, mockk in `libs.versions.toml` + `autocomplete/build.gradle.kts`.
- Phase 2 — Domain: `AutoCompleteItem` (sealed: User/Repository) + `AutoCompleteState` (Idle/Loading/Success/Error).
- Phase 3 — Data: `AutoCompleteDataSource` interface + `GitHubAutoCompleteDataSource` (Ktor, concurrent, merge, sort, limit 50).
- Phase 4 — Presentation: `AutoCompleteViewModel` (StateFlow, debounce 300 ms, flatMapLatest, min 3 chars).
- Phase 5 — UI: full `AutoCompleteComponent` (text field, loading, list, empty state, error state).
- Phase 6 — Integration: wire `GitHubAutoCompleteDataSource` + ViewModel into `MainActivity`; add INTERNET permission.
- Phase 7 — Unit tests: `AutoCompleteViewModelTest` (Mockk + TestDispatcher), `GitHubAutoCompleteDataSourceTest` (MockEngine).
- Phase 8 — UI tests: `AutoCompleteComponentTest` (Compose UI Test, one test per state).

# Progress

**Current status:** Planning phase complete — scaffolding exists, core implementation not yet started.

**Completed:**
- Project scaffolding: `app` and `autocomplete` Gradle modules created.
- `MainActivity` embedding a placeholder `AutoCompleteComponent`.
- Placeholder `AutoCompleteComponent` composable (renders a static title string).
- README with plan, tech stack, and architecture notes.
- Memory bank initialized.

**In progress:**
- (nothing active yet)

**Pending:**
- [ ] Phase 1: Add Ktor, coroutines, serialization, lifecycle-viewmodel, mockk dependencies.
- [ ] Phase 2: `AutoCompleteItem` + `AutoCompleteState` domain classes.
- [ ] Phase 3: `AutoCompleteDataSource` interface + `GitHubAutoCompleteDataSource` (Ktor).
- [ ] Phase 4: `AutoCompleteViewModel` with StateFlow, debounce, flatMapLatest.
- [ ] Phase 5: Full `AutoCompleteComponent` Compose UI (all states).
- [ ] Phase 6: App module integration + INTERNET permission.
- [ ] Phase 7: Unit tests (ViewModel + DataSource).
- [ ] Phase 8: UI instrumented tests (all four states).

**Known issues:**
- GitHub public API rate limit (60 req/hour unauthenticated) — may need an optional API token parameter.
- Compose BOM version `2024.09.00` is older than the current latest; consider upgrading.