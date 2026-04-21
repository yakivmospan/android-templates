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