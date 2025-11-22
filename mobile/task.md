# TimEd Mobile App: Frontend & UX Task Plan

This document outlines a development plan from the perspective of a mobile developer focused on UI, UX, and frontend functionality. The primary goal is to modernize the user experience, improve the developer experience for UI work, and polish user-facing features.

---

## ✨ Epic 1: UI/UX Modernization & Feature Polish (Top Priority)

*Goal: Create a modern, responsive, and delightful user experience by migrating to Jetpack Compose and refining key user interactions.*

### ➡️ Task 1.1: Full UI Migration to Jetpack Compose
- **Description:** The current XML-based UI is functional but outdated. Migrating to Jetpack Compose will significantly improve UI development speed, maintainability, and enable more sophisticated animations and state-driven UIs.
- **Sub-Tasks:**
    - [ ] **Establish Compose Theme:** Create a `Theme.kt` that perfectly mirrors the app's brand identity (`colors.xml`, `dimens.xml`, `styles.xml`) to ensure visual consistency during the migration.
    - [ ] **Build a Component Library:** Convert common XML views (e.g., branded buttons, text input fields, dialogs from `UiDialogs.kt`) into a library of reusable `@Composable` functions.
    - [ ] **Migrate Screens (Phased Approach):**
        - [ ] **Phase 1 (Onboarding):** Convert the `NewUserWelcomeActivity`, `NewUserFeatureActivity`, and `NewUserFinalStepActivity` screens. These are relatively simple and a great starting point.
        - [ ] **Phase 2 (Core Features):** Convert `ProfileActivity`, `EventDetailActivity`, and `ExcuseLetterActivity`. This will involve handling more complex data display and user input.
        - [ ] **Phase 3 (Complex Screens):** Tackle the most complex screens, `HomeActivity` and `TimeInEventActivity`, which involve navigation, nested scrolling, and camera previews.

### ➡️ Task 1.2: Enhance Visuals and User Experience
- **Description:** Polish the existing UI by improving animations, responsiveness, and adding a dark theme.
- **Sub-Tasks:**
    - [ ] **Implement Full Dark Mode:** The current `DayNight` theme is a stub. Define a complete dark theme color palette and ensure all UI components, including custom views like `LavaLampView`, adapt correctly.
    - [x] **Refine Animations:** The app has an extensive set of XML-based animations in `/res/anim` and `/res/animator`. The foundation is solid. The next step is to migrate these to Compose's animation APIs for better performance and interactivity.
    - [x] **Improve Responsiveness:** The app already uses different `dimens.xml` files for various screen widths (`sw600dp`, `sw720dp`), showing that initial work on responsiveness is complete. This can be enhanced further with adaptive layouts in Compose.
    - [ ] **Redesign Dialogs:** The current dialogs are functional but can be improved. Design and implement a new set of beautiful and consistent dialogs using Compose for errors, success messages, and confirmations.

### ➡️ Task 1.3: Improve Onboarding & Tutorial Flow
- **Description:** The current tutorial system in `HomeActivity` is complex and tightly coupled to the View. Refactor it from a UX perspective to be more intuitive and less intrusive.
- **Sub-Tasks:**
    - [ ] **Decouple Tutorial Logic:** Create a dedicated `TutorialManager` or `ViewModel` to handle the state and flow of all tutorials (Quick Tour, Attendance, Event).
    - [ ] **Context-Aware Highlighting:** Instead of a blocking overlay, create a more subtle, context-aware highlighting system in Compose that draws attention to UI elements without preventing user interaction with the rest of the app.
    - [x] **Make Tutorials Skippable & Resumable:** The core logic for this is implemented. The `showTutorialDialog` allows skipping, and the logic in `HomeActivity`'s `onCreate` attempts to resume tutorials, showing the feature is functional.

---

## 🏗️ Epic 2: Frontend Architecture & State Management

*Goal: Refactor the client-side architecture to support a modern, reactive UI and make state management simple and predictable.*

### ➡️ Task 2.1: Implement MVVM for UI State Management
- **Description:** Managing state directly in Activities is error-prone. MVVM will provide a clean separation between UI and business logic, which is essential for a stable Compose UI.
- **Sub-Tasks:**
    - [ ] **Create ViewModels:** Introduce a `ViewModel` for every screen or major feature.
    - [ ] **Manage State with `StateFlow`:** Use `StateFlow` within the ViewModels to hold and expose the UI state (e.g., list of events, user profile data, loading status).
    - [ ] **Connect UI to ViewModels:** In Activities (and later, in Composables), collect the state from the `StateFlow` to drive the UI reactively.

### ➡️ Task 2.2: Implement Repository Pattern for Clean Data Handling
- **Description:** Abstracting data sources will make the ViewModels simpler and independent of where the data comes from (network, cache, etc.).
- **Sub-Tasks:**
    - [ ] **Create Repository Interfaces:** Define interfaces for each data domain (e.g., `EventRepository`, `UserRepository`).
    - [ ] **Implement Repositories:** Create concrete implementations of the repositories that handle the logic of fetching data from Firebase and the remote API.
    - [ ] **Integrate with ViewModels:** Refactor ViewModels to depend on the repository interfaces, not on direct Firebase or network calls.

---

## ⚙️ Epic 3: Platform Health & Backend Collaboration

*Goal: Address underlying issues in the backend and testing that directly impact the frontend's stability and ability to deliver features.*

### ➡️ Task 3.1: Backend Improvements (for Frontend Stability)
- **Description:** Issues in the backend directly affect the mobile app's performance and reliability.
- **Sub-Tasks:**
    - [ ] **Flag Inefficient `deleteUser`:** The `deleteUser` method in `UserService` is a performance risk that could cause the app to hang or fail. Collaborate with the backend team to prioritize its optimization using a Cloud Function.
    - [ ] **Ensure Consistent Validation:** Work with the backend team to ensure all API endpoints and service methods return structured error messages (like the pattern used in `ExcuseLetterService`) instead of generic exceptions.

### ➡️ Task 3.2: Security & Robustness
- **Description:** Sensitive information should not be stored in version control.
- **Sub-Tasks:**
    - [x] **Validate `.gitignore`:** The `.gitignore` files are correctly configured to ignore sensitive files like `google-services.json` and `local.properties`.
    - [ ] **Secure API Keys:** Move any hardcoded API keys to a secure location like `local.properties` and access them via `BuildConfig`.

### ➡️ Task 3.3: Testing
- **Description:** A lack of tests makes UI refactoring and feature development risky.
- **Sub-Tasks:**
    - [ ] **Compose UI Tests:** Write UI tests for new Jetpack Compose screens to verify layout and interactions.
    - [ ] **ViewModel Unit Tests:** Write unit tests for the new ViewModels to ensure UI logic is correct.