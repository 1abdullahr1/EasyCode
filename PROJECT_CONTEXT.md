# Project Context & AI Assistant Onboarding Guide

This document defines the working preferences, architectural foundation, and development lifecycle for this application codebase. Any AI assistant working on this project must read and strictly adhere to the guidelines below.

---

## 1. Core Principles & User Preferences

1. **Strictly Zero Emojis**:
   - Never use emojis anywhere: code, commit messages, comments, logs, documentation, or chat responses.
   - Use clean, professional language and native vector iconography only.
2. **Concise & Direct Responses**:
   - Provide direct summaries, actionable implementation details, and verification steps. Avoid excessive conversational filler.
3. **Clickable File Links**:
   - Always link referenced files, directories, classes, and methods using the `file://` URL scheme with forward slashes (e.g. `[app/build.gradle.kts](file:///D:/My%20Projects%2014%20aug/antigravity%20cli/Andriod/ide/app/build.gradle.kts)`).
4. **Preserve Working Architecture**:
   - Do not perform destructive refactors without careful incremental verification. Keep working features intact while extending or rebranding the application.

---

## 2. Environment & Build Strategy

- **Operating System**: Windows (PowerShell shell).
- **No Local Compilation / No Local Toolchain Installations**:
  - The local Windows environment does not have Java, Gradle, Cargo, Rust, or Node installed.
  - Never execute `./gradlew`, `gradle build`, `cargo build`, or package managers on the host machine.
  - Never attempt to download or install JDK, Android SDK, or build tools locally.
- **Cloud CI with GitHub Actions**:
  - All builds and packaging (Android APK, Desktop releases) are handled in the cloud using GitHub Actions runners.
  - Workflows reside in `.github/workflows/`:
    - `build-android.yml`: Compiles the Android debug APK and uploads the artifact `vu-lecture-hub-debug-apk`.
    - `build-desktop.yml`: Compiles the cross-platform desktop binary.
  - **Standard Cloud Build Lifecycle**:
    1. Make required edits to code and resources.
    2. Commit and push changes:
       ```powershell
       git add .
       git commit -m "Your descriptive commit message"
       git push origin main
       ```
    3. Monitor workflow status via GitHub CLI:
       ```powershell
       gh run list --limit 3
       ```
    4. Download compiled artifacts:
       ```powershell
       gh run download <run-id> -n <artifact-name> --dir output_android
       ```

---

## 3. Application Architecture & Key Patterns

### Android App
- **Language**: Kotlin 2.0+
- **Min SDK**: 26 (Android 8.0 Oreo) | **Target SDK**: 34 / 35
- **Screen Navigation**:
  - Hardware-accelerated transitions via `ViewPager2` synchronized with `BottomNavigationView`.
  - Configured with `offscreenPageLimit = 2` to retain active tab fragments in the GPU view hierarchy for zero-lag swipe and tap animations.
  - Managed by `MainPagerAdapter` extending `FragmentStateAdapter`.
- **List Optimization**:
  - All `RecyclerView` instances use `setHasFixedSize(true)` and `DiffUtil` adapters to prevent unnecessary measure and layout passes.
- **Threading**:
  - Data processing and JSON parsing run on `Dispatchers.Default`.
  - UI updates and adapter submissions run on `Dispatchers.Main`.
- **Startup & Splash**:
  - Lightweight vector emblem branding (`@drawable/ic_launcher_foreground`).
  - Fast launch transition (500 ms delay max) without heavy JSON Lottie animations.

### Desktop App (Optional / Shared)
- Located in `desktop/` directory.
- Built via GitHub Actions runner workflow `.github/workflows/build-desktop.yml`.

---

## 4. How to Adapt / Customize This Template for a New App

When re-architecting or transforming this codebase for a new application:
1. **Package Name & App Identity**:
   - Update `applicationId` and `namespace` in `app/build.gradle.kts`.
   - Update `android:label` and icons in `app/src/main/AndroidManifest.xml` and `res/values/strings.xml`.
2. **Tab Layout & ViewPager2**:
   - Update bottom navigation menu items in `app/src/main/res/menu/bottom_nav_menu.xml`.
   - Update `MainPagerAdapter.kt` to instantiate the new top-level fragments.
3. **Data Layer & Models**:
   - Replace models and repositories in `app/src/main/java/com/vu/lecturehub/data/` with the new data schemas and API/local ingestion services.
4. **Cloud Repository Setup**:
   - Connect the fresh local repository to the new remote GitHub repository:
     ```powershell
     git remote add origin https://github.com/<user>/<new-repo-name>.git
     git push -u origin main
     ```

---

## 5. Quick Primer Prompt for New AI Chat Sessions

When opening a new chat for this project, you can start with:

```text
Please read GEMINI.md and PROJECT_CONTEXT.md in the project root to understand the environment constraints, zero-emoji policy, cloud CI build process, and app architecture before starting.
```
