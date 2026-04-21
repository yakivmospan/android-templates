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

