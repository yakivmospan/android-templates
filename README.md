# Auto complete Android Sample

A reusable native Android autocomplete component/library that fetches matching GitHub users and repositories for a given query
string, demonstrating clean architecture, separation of concerns, and testability.
This is a Kotlin based Android project targeting Android with min 24 API.

* [/app](./app/) contains Auto Complete application sample code.

* [/autocomplete](./autocomplete/) contains Auto Complete Component library, made with Kotlin and Compose.

### Build Application

To build and run the development version of the Sample Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:

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

## Tradeoffs:

- Checking overall general errors, including parsing. In a real app error handling should be based on http code handling and api error parsing.
- Pagination is done purely. In real app data source should return Paginated data object with size, total count and next page
  info. In this sample we just fetch next page when user scrolls to the end of the list.
- Github Data source is just sorting new pages data and adds it to the end of existing list.
  So firt page result is sorted A-Z, and then next page sorted A-Z is started after last page Z items. With current UX where user
  scrolls to the end of the list and fetches next page, this looks bad. In real app we may want to change UI of autocomplete to
  have actual page numbers so user can jump between them and see next result sorted from the first item again, or we need an API
  that has a sorting implemented under the hood.