# TimEd Mobile App: Usability & Performance Testing Plan

This document outlines a plan for testing the usability and performance of the TimEd mobile application.

---

## 🧪 Epic 1: Usability & Performance Testing

*Goal: Validate the application's user-friendliness and technical performance through structured testing and analysis.*

### ➡️ Task 1.1: Usability Testing
- **Description:** Ensure the application is intuitive, efficient, and easy to use for the target audience (faculty members).
- **Sub-Tasks:**
    - [x] **Heuristic Evaluation:** Conduct an expert review of the app against established usability principles (e.g., Nielsen's Heuristics) to identify obvious usability issues.
    - [x] **User Testing Sessions:**
        - [x] **Recruit Participants:** Find 3-5 faculty members to participate in testing sessions.
        - [x] **Define Test Scenarios:** Create scripts for key user flows:
            - First-time user onboarding and tutorial.
            - Performing a daily time-in with face detection.
            - Joining an event using both QR code and manual code entry.
            - Submitting an excuse letter.
            - Changing a password.
        - [x] **Conduct & Analyze:** Observe users as they perform tasks, note their feedback and pain points, and synthesize findings into actionable improvements.
    - [x] **Review In-App Copy:** Check all instructions, error messages, and dialog text for clarity, tone, and helpfulness.

### ➡️ Task 1.2: Performance Profiling & Optimization
- **Description:** Identify and fix performance bottlenecks to ensure the app is fast, responsive, and efficient.
- **Sub-Tasks:**
    - [x] **Analyze App Startup Time:**
        - [x] **Remove Splash Screen Delay:** Eliminate the hardcoded 5-second delay in `SplashActivity` and run network/session checks concurrently with animations.
        - [x] **Optimize Home Screen Load:** Use Android Studio's profiler to analyze and parallelize the initial data-loading network calls on `HomeActivity`.
    - [x] **Profile UI Rendering Performance:**
        - [x] **Identify Jank:** Use the "Profile GPU Rendering" tool to find and fix instances of stuttering or dropped frames, especially on `HomeActivity` and camera screens.
        - [x] **Analyze Overdraw:** Use the "Debug GPU Overdraw" tool to find and reduce unnecessary rendering layers in complex XML layouts.
        - [x] **Benchmark Custom Views:** Specifically measure the performance impact of `LavaLampView` and other custom animated views.
    - [x] **Monitor Memory and Battery Usage:**
        - [x] **Detect Memory Leaks:** Use the Android Studio Memory Profiler to check for leaks, especially in Activities that use the camera (`TimeInActivity`, `TimeInEventActivity`).
        - [x] **Optimize Image Handling:** Analyze bitmap creation and memory usage during camera capture and image processing to prevent crashes.
        - [x] **Assess Battery Impact:** Monitor battery consumption during prolonged use of camera features and location services.