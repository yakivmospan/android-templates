# Active Context

**Current focus:**
Phase 8 complete — all UI instrumented tests implemented and passing.

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

**UI test patterns established:**
- Always test the ViewModel-connected overload: `AutoCompleteComponent(viewModel = mockVm, ...)` and `GitHubAutoCompleteComponent(viewModel = mockVm, ...)`.
- `TAG_AUTOCOMPLETE_STATELESS` (internal) on root `Column` of stateless `AutoCompleteComponent`. Every test asserts this tag is displayed — proves delegation from ViewModel-connected overload to stateless overload.
- `TAG_LOADING_INDICATOR` (internal) on `CircularProgressIndicator` in `LoadingContent`. Used to assert spinner presence/absence.
- `GitHubAutoCompleteComponent` tests assert `TAG_AUTOCOMPLETE_STATELESS` inherited from inner `AutoCompleteComponent` — no separate tag needed.
- `mockk-android` added to `androidTestImplementation`. `packaging` block excludes JUnit Jupiter `META-INF/LICENSE.md` conflict brought in transitively.
- `mockk(relaxed = true)` for `AutoCompleteViewModel<T>`; `state`/`query` stubbed via `MutableStateFlow`. `createComposeRule()` (JUnit4). `launchComponent()` helper stubs ViewModel and calls `setContent`.
- Backtick test names with spaces are **not** allowed in Android instrumented tests (ART VM restriction). Use `snake_case`: `when_X_then_Y`.
- Every part of the test name must have a corresponding assertion — e.g. "no spinner or message" requires both `TAG_LOADING_INDICATOR.assertDoesNotExist()` and `onNodeWithText(...).assertDoesNotExist()`.
- `assertDoesNotExist()` is a method on `SemanticsNodeInteraction` — no import needed.
- Interaction tests for `GitHubAutoCompleteComponent` use a lambda capture (`mutableListOf`) rather than mockk `verify`, since `onItemSelected` is a plain callback, not a ViewModel event.

**Open questions:** none.

**Next steps:** Project complete — all unit tests (Phase 7) and UI instrumented tests (Phase 8) implemented.
