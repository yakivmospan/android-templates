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

