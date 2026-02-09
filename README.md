# Two Layers Architecture Template

### Architecture Diagram

```
┌───────────────────────────────────────────────────┐
│                      App                          │     
│      Intro point, Dependencies Managment          │
│       Concreate Services Implementation           │
│               (depends on all)                    │
│                                                   │
│    ┌─────────────────────────────────────────┐    │
│    │          Presentation Layer             │    │
│    │  UI<>ViewModels<>Services(Navigation)   │    │ 
│    └─────────────────────────────────────────┘    │ 
│                       ↓                           │
│    ┌─────────────────────────────────────────┐    │
│    │              Core Layer                 │    │
│    │ Domain<>Entities<>Data<>Infrastructure  │    │
│    └─────────────────────────────────────────┘    │
│                                                   │
│               (Shared between all)                │
│    ┌─────────────────────────────────────────┐    │
│    │            Common Layer                 │    │
│    │       Utils, Constants, Extentions      │    │
│    └─────────────────────────────────────────┘    │
│                                                   │ 
└───────────────────────────────────────────────────┘
```

### Architecture Overview

This project is implemented using Layered Architecture, which separates the application into distinct layers with specific responsibilities. The main layers are as follows:

**App Layer:** `app` package.
- This layer serves as the entry point of the application. It initializes the necessary dependencies and sets up the application environment.
- `service` package - contains data and service-related classes implementations, that can be unique for each platform or app variant (controlled by dependency injection). Good example is a Console.
- This layer has dependencies on all other layers.

**Presentation Layer:** `presentation` **package.**
- This layer is responsible for the UI components and user interactions. It includes screens, view models, and UI state management.
- It contains of sub-packages like `screens`, `components`, and `viewmodels`.
- This layer depends on the Data Layer and Common Layer.

**Core Layer:** `core` **package.**
- This layer has all other responsibilities, including data management, business logic and infrastructure interaction.
- It contains of sub-packages like `entity`, `repository`, and `service`, `storage`, `api`, `interactors`.
- Even though it has a lot of responsibilities, it is using Dependency Inversion Principle to depend only on abstractions. So everything inside this layer, all its subpackages are decoupled, testable and reusable.

**Common Layer:** `common` **package.**
- This layer contains shared utilities, constants, and helper functions that are used across multiple layers of the application.
- It promotes code reusability and reduces duplication.

### Key Principles Followed

- ViewModels knows about UI, UI knows about ViewModels.
- ViewModels knows about Interactors (if available), Entities, Repositories (Interfaces), Infrastructure Services (Interfaces).
- Interactors knows about Repositories(Interfaces), Infrastructure Services (Interfaces), Entities.
- Repositories knows about Data Sources (Interfaces), Entities.