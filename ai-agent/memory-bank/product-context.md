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

