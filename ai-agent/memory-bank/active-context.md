# Active Context

**Current focus:**
Phase 4 — Presentation layer: `AutoCompleteViewModel<T>`.

**Recent decisions:**
- Library module (`autocomplete`) is completely self-contained; the `app` module only integrates it as a dependency.
- All core types are generic `<T>`: `AutoCompleteDataSource<T>`, `AutoCompleteViewModel<T>`, `AutoCompleteState<T>`, `AutoCompleteComponent<T>`. The library imposes no item model on callers.
- `AutoCompleteComponent<T>` is fully generic with no callbacks — click handling is the caller's responsibility via `Modifier.clickable` inside `itemContent`.
- `GitHubAutoCompleteComponent` is the opinionated default wrapper: bundles `GitHubItemRow` as `itemContent` and exposes `onItemSelected: (GitHubItem) -> Unit`.
- No GitHub API token — unauthenticated requests only.
- Minimum 3 characters before triggering a search (debounce 300 ms inside ViewModel).
- Results are paginated. `AutoCompleteDataSource<T>` interface takes `page: Int` (1-based); empty result signals end of data. `pageSize` is a constructor param on `GitHubAutoCompleteDataSource` (default 50), not part of the interface.
- `AutoCompleteState.Success<T>` carries `isLoadingMore` (bottom spinner) and `hasMore` (gates further page loads).
- `AutoCompleteViewModel` resets to page 1 on query change; exposes `loadMore()` to append next page.
- `AutoCompleteComponent` calls `viewModel.loadMore()` when the last list item becomes visible.
- Ktor chosen for networking (KMP-ready for future migration).
- Mockk chosen for mocking in unit tests.

**Open questions:** none.

**Next steps (phased):**
- Phase 1 — Dependency setup: Ktor, coroutines, serialization, lifecycle-viewmodel, mockk in `libs.versions.toml` + `autocomplete/build.gradle.kts`.
- Phase 2 — Domain: `AutoCompleteDataSource<T>` interface (`search(query, page)`) + `AutoCompleteState<T>` (`Idle/Loading/Success(items, isLoadingMore, hasMore)/Error`).
- Phase 3 — Data: `GitHubItem` model + `GitHubAutoCompleteDataSource` (Ktor, concurrent, page + pageSize params, merge, sort).
- Phase 4 — Presentation: `AutoCompleteViewModel` (StateFlow, debounce 300 ms, flatMapLatest, min 3 chars, page reset on query change, `loadMore()` appends next page).
- Phase 5 — UI: `AutoCompleteComponent<T>` (text field, loading, paginated list with bottom spinner on `isLoadingMore`, empty state, error state) + `GitHubAutoCompleteComponent` (default GitHub UI with `onItemSelected`).
- Phase 6 — Integration: wire `GitHubAutoCompleteDataSource` + ViewModel into `MainActivity`; add INTERNET permission.
- Phase 7 — Unit tests: `AutoCompleteViewModelTest` (Mockk + TestDispatcher), `GitHubAutoCompleteDataSourceTest` (MockEngine).
- Phase 8 — UI tests: `AutoCompleteComponentTest` (Compose UI Test, one test per state).