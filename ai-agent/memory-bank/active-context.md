# Active Context

**Current focus:**
Phase 7 complete — all unit tests implemented and passing. Phase 8 next: UI instrumented tests.

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

**Unit test patterns established:**
- `AutoCompleteViewModelTest`: `StandardTestDispatcher` shared between `testScope`, `viewModelScope` (via `Dispatchers.setMain`), and `ioDispatcher`. `sendQuery` helper calls `advanceUntilIdle()` before emitting the query to ensure `subscribeToQueryChanges()` has started (avoiding `drop(1)` eating the query). Per-query `coEvery` stubs used when call-count ordering is not deterministic (e.g. loadMore + query change race). All assertions use exact state equality — no `any {}` predicates.
- `GitHubAutoCompleteDataSourceTest`: `MockEngine`-backed `buildDataSource` helper; URL-capture pattern for param assertions; 500 + empty body triggers serialization failure, which is the "both fail" path.

**Open questions:** none.

**Next steps (phased):**
- Phase 8 — UI tests: `AutoCompleteComponentTest` (Compose UI Test, one test per state).