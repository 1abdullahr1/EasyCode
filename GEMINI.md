# Development Rules & Environment Guidelines

## 1. Zero Emojis Policy
- Strictly NO emojis anywhere in the codebase, commit messages, comments, pull requests, documentation, or chat responses.
- Use enterprise-grade, clean vector symbols and clear professional language only.

## 2. Environment & Build Constraints
- Operating System: Windows (PowerShell shell).
- STRICTLY NO LOCAL BUILDS OR COMPILER INSTALLATIONS:
  - Do NOT run `./gradlew`, `gradle`, `cargo`, `rustc`, `npm`, `node`, or `pip` locally on the host machine.
  - Do NOT attempt to install Java, Android SDK, Gradle, Rust, or Node locally.
  - The local machine is an editing and git control environment only.
- All builds are performed on GitHub Actions Cloud CI:
  - Android APK builds run via `.github/workflows/build-android.yml`.
  - Desktop builds run via `.github/workflows/build-desktop.yml`.
  - To build and test: commit changes, push to GitHub (`origin/main`), check run status via `gh run list`, and download artifacts via `gh run download`.

## 3. Communication Style & Links
- Keep all responses concise, direct, and actionable.
- Always provide clickable GitHub-style markdown links with the `file://` scheme and forward slashes for every file, class, function, or directory mentioned.

## 4. Architectural Standards
- Modern Android Kotlin 2.0+ with Material 3 styling.
- Navigation: `ViewPager2` backed by `FragmentStateAdapter` and `BottomNavigationView` with `offscreenPageLimit = 2` for hardware-accelerated 60/120fps transitions.
- Performance: `setHasFixedSize(true)` on `RecyclerView`s, DiffUtil callbacks, and background processing on `Dispatchers.Default`.
- Splash / Launch: Native vector emblem with minimal launch delay; avoid heavy JSON animation libraries at startup.
