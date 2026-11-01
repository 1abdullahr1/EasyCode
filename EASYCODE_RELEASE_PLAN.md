# EasyCode: completion and Google Play release plan

Created: 1 November 2026  
Status: Implementation in progress; initial correctness fixes passed cloud tests and debug build.  
Product reference: JSFiddle. Platform: Android. Scope: EasyCode in `app/`.

## 1. Product goal

Make EasyCode a dependable pocket playground for HTML, CSS and JavaScript. A user should be able to start from a blank fiddle or template, edit code, run it, inspect errors, save it, duplicate it and share an export without losing work.

Use JSFiddle's edit/run/result workflow as inspiration, adapted for touch screens. Keep EasyCode's own name, assets and interface. JSFiddle documentation is the reference for panel and editor concepts, not a promise of feature parity. [Reference](https://docs.jsfiddle.net/)

### Proposed first-release scope

- HTML, CSS and JavaScript panels with a preview and console.
- Reliable local projects, automatic draft recovery, manual save and duplicate/fork.
- Usable mobile editing: highlighting, line numbers, undo/redo, indentation, find/replace and symbol keys.
- Explicit Run, optional debounced auto-run, preview reset and full-screen preview.
- Phone tabs and usable split view for larger screens and landscape.
- Templates and configurable external CSS/JS libraries.
- HTML export plus a portable project backup/import format that preserves separate panels and resources.
- Working themes, font sizes and word wrap.
- Accessible UI, clear errors, privacy information and a signed Play release bundle.

Proposed defaults: local-first, no account, no ads, no analytics and no paid features for version 1.0. These are planning assumptions to confirm before implementing monetization or adding data services.

### Later releases

Public shareable URLs, cloud sync, collaboration, public profiles, community templates, Git integration, npm packages, terminal/backend execution, TypeScript/Sass and arbitrary multi-file projects. These need additional architecture and should not block the first release. File sharing in 1.0 must not be described as hosted URL sharing.

## 2. Current baseline

This baseline is from source inspection, not a device test. An existing debug APK has not been verified against the current source.

| Area | Observed status |
| --- | --- |
| Native Android app | Kotlin, XML views, ViewPager2, Material components, Room and WebView |
| Identity | `com.easycode.ide`, version `1.0.0`; debug adds `.debug` |
| Platform | Minimum API 26; compile/target API 35 |
| Core workflow | Editing, preview, console, local save/load, resources and six templates have implementation code |
| Editor correctness | Undo/redo/format update the visible editor without synchronizing the shared code state |
| Draft safety | Manual save exists; persistent unsaved drafts and replacement prompts are missing |
| Settings | Theme works in code; font size, wrap and auto-run controls are not connected |
| Split view | Separate editor/preview fragments exist, but symbol-key routing and hidden preview execution need correction |
| Export | Debug FileProvider authority mismatch; failures are caught without a useful user message |
| Automation | `.github/workflows/build.yml` builds a debug APK; release AAB/signing and release checks need adding |
| Documentation | Template/VU references remain; `desktop/` contains a separate VU app |

## 3. Work rules and tracking

- **Mandatory: all Android builds and automated tests run on GitHub Actions, never locally.** The local Windows machine does not have the required development environment configured. Do not run local Gradle builds/tests or install Java, Android SDK, Gradle, emulator or other compiler toolchains to complete this plan.
- Use the local machine for source editing, read-only inspection and Git operations. Commit/push authorized changes, monitor the GitHub Actions run, and download its APK/AAB artifacts. Manual testing on an available physical Android device uses the APK built by GitHub Actions; emulator/instrumentation automation must run on GitHub Actions runners.
- Preserve existing work. Use focused changes and review diffs before commits.
- Keep desktop/VU work outside the EasyCode release scope; do not delete it as part of cleanup.
- A checked item means implemented and verified, with evidence recorded below. Reading code alone does not establish that a feature works on a device.
- Release gates take priority over adding features. Do not ship with known data loss, editing-state inconsistencies, broken export or unrecoverable preview crashes.
- This document authorizes no publishing, store submission, account creation, purchases or external messages. Those are future execution steps.

| Milestone | Depends on | Status | Evidence / build / remaining issues |
| --- | --- | --- | --- |
| M0 Baseline and scope | None | In progress | Baseline `558ace2`; GitHub Actions run 36973287619 succeeded. Device review pending. |
| M1 Correctness and recovery | M0 | In progress | Editing synchronization, project history reset and export fixes implemented; cloud/device validation pending. |
| M2 Editor and project workflow | M1 | Not started | |
| M3 Preview and resources | M1 | Not started | |
| M4 UI and accessibility | M2, M3 | Not started | |
| M5 Release engineering and disclosures | M0; finalize after M4 | Not started | |
| M6 Beta and publication | M1–M5 | Not started | |

## 4. M0 — Establish a reproducible baseline

- [x] Record repository revision, existing changes, dependency versions and current CI result.
- [ ] Produce a fresh debug APK through CI and record its commit and artifact.
- [ ] Install on a physical phone and exercise every visible control; log reproducible defects.
- [ ] Confirm 1.0 scope, app name/package, developer account type, pricing, support email and privacy hosting location.
- [ ] Inventory tests, dependency licenses, permissions and third-party network requests.
- [ ] Document the actual Android architecture and correct stale workflow/package references.

Acceptance: a reproducible baseline build, a device findings list and an agreed 1.0 scope. Account-dependent release requirements are recorded before scheduling launch.

## 5. M1 — Fix correctness and protect user work

- [ ] Establish one authoritative project state. Typing, undo, redo, formatting, loading and templates must update it consistently.
- [ ] Make Run, Save and Export consume the same current snapshot.
- [ ] Reset undo history when changing projects; preserve expected cursor/selection behavior and prevent cross-project undo.
- [ ] Persist drafts automatically, restore after process death and show clear saved/saving/error states. Define and test the autosave delay.
- [ ] Track unsaved changes and protect New, Load, Template and destructive Clear actions.
- [ ] Keep the active project and draft identities consistent after save/delete/duplicate.
- [ ] Handle database failures with actionable feedback; define Room migrations before schema changes.
- [ ] Fix provider authority with an application-ID-aware manifest value and verify both debug and release export.
- [ ] Add targeted regression tests for state synchronization, save/load, draft recovery and project switching.

Acceptance: edit → undo/redo/format → run → save → reopen → export all use the same content. Killing/restarting the app restores the latest persisted draft. Replacement actions cannot silently discard unsaved work.

Primary files: `ui/editor/EditorFragment.kt`, `ui/editor/UndoRedoManager.kt`, `ui/EasyCodeViewModel.kt`, database/repository classes, `util/FileUtils.kt`, `AndroidManifest.xml` (Kotlin paths relative to `app/src/main/java/com/easycode/ide/`).

## 6. M2 — Complete the editor and fiddle workflow

- [ ] Add local duplicate/fork, rename, search and sort by recent update/title.
- [ ] Implement font size, word wrap and auto-run preferences with persistence and immediate application.
- [ ] Add find/replace, go-to-line, indentation/outdent and predictable bracket/tag assistance.
- [ ] Evaluate retaining the current editor versus adopting a maintained Android editor component. Record license, APK size, accessibility and large-file behavior before choosing.
- [ ] Replace the simple formatter with language-aware formatting, or remove the action from 1.0 until it is reliable. Never silently modify code semantics.
- [ ] Route symbol keys and shortcuts to the focused visible editor in both layout modes.
- [ ] Add structured project backup/import with a versioned schema, size validation and useful invalid-file errors.
- [ ] Add Save HTML through Android's document picker as well as Share HTML. Explain that externally hosted resources remain network-dependent.
- [ ] Include project title and preserve code/resource ordering in exports; keep preview-only bridge code out of production exports where possible.

Acceptance: users can create, rename, duplicate, find, export, import and resume projects. Import/export round trips preserve all three panels and resources. Every displayed editor setting changes behavior.

## 7. M3 — Make preview, libraries and console dependable

- [ ] Ensure exactly one active preview executes each run, including after repeated split/tab switches.
- [ ] Define execution modes explicitly: normal JavaScript, ES modules and supported React/JSX templates. Validate compiler output for each; do not assume wrapping arbitrary code in `try` is compatible with module syntax.
- [ ] Validate all six templates and replace misleading template descriptions or unsupported examples.
- [ ] Add resource URL validation, enable/disable, ordering and removal; prefer HTTPS and pinned versions for bundled examples.
- [ ] Show load failures and offline explanations. Provide at least two fully offline templates.
- [ ] Debounce auto-run and stop obsolete pending runs. Keep manual Run available.
- [ ] Reset/unload the page when clearing or switching projects. Clean up WebViews and callbacks when views are destroyed.
- [ ] Add preview recovery for renderer failures and runaway code. Investigate actual WebView capabilities before promising that any infinite loop can be stopped instantly.
- [ ] Limit console history and message length; batch updates to avoid a full UI refresh on every log.
- [ ] Capture syntax errors, runtime errors and rejected promises. Distinguish generated-page line numbers from editor lines; map them where feasible.
- [ ] Review file/content access, navigation handling, cleartext traffic, bridge exposure and shared preview storage. Disable unnecessary access and keep the native bridge minimal.
- [ ] Prevent one project's localStorage/cookies from unexpectedly exposing another project's state; define isolation/reset behavior.

Acceptance: one Run means one execution; clearing stops the previous page's activity; log-heavy code remains manageable; a failed resource has a visible explanation; the user can recover their project after a preview failure.

## 8. M4 — Finish the mobile experience

- [ ] Choose a consistent navigation design; avoid redundant tab bars taking space from the editor.
- [ ] Make split view resizable and allow switching HTML/CSS/JS while it is active.
- [ ] Add full-screen preview, keyboard-aware controls and clear Run/Save status.
- [ ] Handle back navigation, orientation changes, system bars, edge-to-edge layout and Android keyboard insets.
- [ ] Test small phones, landscape, tablets and hardware keyboards.
- [ ] Check TalkBack labels/focus, touch target sizes, contrast and larger system fonts.
- [ ] Keep code colors readable in both themes; fix formatting and line-number alignment with wrapping enabled.
- [ ] Add concise onboarding, an offline explanation, About, support, privacy and license links.
- [ ] Replace inherited VU branding in Android resources and documentation with intentional EasyCode assets after checking references.

Acceptance: all primary tasks are usable on a small phone with the keyboard open and on a larger split-view screen. No clipped controls, unreadable themes or dead controls remain.

## 9. M5 — Build a publishable release

### Engineering

- [ ] Raise compile/target SDK to the submission requirement and use a compatible Android Gradle Plugin/Gradle/JDK combination. Current official guidance requires API 36 for new phone/tablet submissions from 31 August 2026. Recheck before upload. [Target API requirements](https://developer.android.com/google/play/requirements/target-sdk)
- [ ] Exercise the target-SDK behavior changes, including system bars, navigation and large-screen behavior.
- [ ] Add CI checks for lint, meaningful unit/instrumentation tests and release compilation.
- [ ] Produce a signed Android App Bundle using CI secrets and Play App Signing; protect and back up the upload key. Never commit signing material.
- [ ] Set version code/name deliberately; verify debug and release coexist and that a release upgrade preserves stored projects.
- [ ] Inspect dependency/native-library contents and verify 16 KB page-size compatibility, including transitive SDKs. Test on a suitable environment. [Android guidance](https://developer.android.com/guide/practices/page-sizes)
- [ ] Review release permissions, debug flags, shrinker behavior if enabled, licenses and artifact contents.
- [ ] Record source revision, artifact hash, version and test evidence for each release candidate.

### Store and disclosure work

- [ ] Confirm developer verification and applicable Play Console account requirements.
- [ ] Prepare original icon, feature graphic, screenshots from the release candidate, short/full description, support email and release notes.
- [ ] Describe only verified features; explain local storage and external-library internet requirements.
- [ ] Publish an accessible privacy policy and link it in-app and in Play Console. Audit actual app/SDK/network behavior before completing Data safety; do not infer “no data collected” from local project storage alone. [User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311)
- [ ] Complete Data safety, content rating, target audience, ads declaration and app access instructions accurately.
- [ ] Review applicable Play policies for a code playground with user-authored JavaScript and remote libraries. Document execution boundaries and network behavior for reviewers; do not assume acceptance.
- [ ] Provide local data deletion and document Android backup behavior. If accounts are added later, assess account-deletion requirements before release.

Acceptance: a signed AAB passes relevant Play Console checks; required declarations match observed app behavior; listing assets match the tested release candidate.

## 10. M6 — Beta, release gates and publication

- [ ] Distribute an internal build and inspect the Play pre-launch report where available.
- [ ] Run the matrix below on physical devices and CI/emulator environments as appropriate.
- [ ] Recruit closed testers if required. Personal accounts created after 13 November 2023 currently need at least 12 testers opted in continuously for 14 days before applying for production access. Record real feedback and testing activity; production access is a separate application. [Official testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en)
- [ ] Triage feedback and rerun affected checks after fixes. Avoid changing unrelated features during release-candidate testing.
- [ ] Resolve all release-blocking bugs and review any accepted minor issues explicitly.
- [ ] Review the exact release artifact, listing, disclosures, countries and pricing before store submission.
- [ ] Submit when explicitly authorized; monitor review results and address actionable feedback.
- [ ] After launch, monitor Android vitals, support reports and reviews. Prepare a patch release process with increasing version codes and compatible database migrations.

### Minimum verification matrix

| Scenario | Required result |
| --- | --- |
| Edit/undo/redo/format then run/save/export | All outputs reflect the visible code |
| Process death, backgrounding, restart | Persisted draft restored; no accidental overwrite |
| Project switch, template load, deletion | Correct identity and explicit handling of unsaved work |
| Import/export including Unicode and malformed inputs | Accurate round trip; safe, readable failures |
| Debug and signed release HTML sharing | Correct provider, readable file in receiving apps |
| Tabs/split/orientation/keyboard changes | Correct editor routing; one active preview |
| Plain JS, modules, JSX and every template | Documented modes work; unsupported syntax is explained |
| Offline or failed CDN | Local examples work; resource failures are visible |
| Syntax error, rejected promise, repeated logging, runaway code | Useful diagnostics and recoverable editing state |
| Minimum API 26 and current Android versions | Core workflow and platform behavior pass |
| Small phone, tablet, dark/light, TalkBack, larger fonts | Primary actions remain usable |
| Existing release upgraded to candidate | Saved projects and preferences survive |
| Larger snippets and long sessions | No unresolved crash, freeze or runaway memory growth |

Measure performance on a named mid-range device. Proposed engineering targets: visible typing response under 100 ms for a 50 KB panel, no sustained main-thread stalls during logging, and stable memory after 30 preview resets. Record measured results rather than marking these targets achieved by inspection.

## 11. Execution order and completion definition

Start with M0, then M1. Build M2 and M3 on the corrected project state; finish M4 after their behavior is stable. Account setup, policy drafting and CI preparation can progress alongside development, but finalize declarations against the release candidate. Begin any required closed-testing period as soon as a stable eligible beta is ready.

No fixed launch date is promised. Estimate remaining effort after the baseline device review; account verification, tester availability and Play review are external dependencies.

Version 1.0 is complete when the agreed scope works, all release gates pass, the store materials are accurate, the required testing is complete and the approved signed release is published. Completing this plan document or generating an APK does not mean the app is publication-ready.

### First implementation batch

1. Capture a fresh baseline and reproducible cases for editing-state, export and split-view defects.
2. Fix editor-to-state synchronization and add focused regression coverage.
3. Implement draft persistence and unsaved-change protection.
4. Fix debug/release export and verify both variants through CI/device testing.
5. Record evidence, update this checklist and proceed to the remaining milestones.

### Implementation log — 1 November 2026

- Baseline: commit `558ace26707800b0e24623fd2dc9a3eb87d782a3`, main branch, remote `1abdullahr1/EasyCode`. Only this plan was untracked before implementation; tracked source had no existing changes.
- Latest baseline cloud build: https://github.com/1abdullahr1/EasyCode/actions/runs/36973287619 (success).
- Recorded dependencies: AGP 8.7.3, Kotlin 2.0.21, KSP 2.0.21-1.0.28; compile/target API 35, minimum API 26.
- Initial changes: Undo/Redo/Format synchronize shared code; formatting enters undo history; new/load/template actions reset editor history; provider authority follows application ID; export failures produce visible feedback.
- Added JVM regression tests for history boundaries and compiler composition/resource handling. CI now runs tests before producing the debug APK and uploads reports.
- Cloud validation: commit `e9f7f19` passed tests and debug APK compilation at https://github.com/1abdullahr1/EasyCode/actions/runs/41285253843. Artifacts downloaded to `output_android/e9f7f19/`.
- Validation still required: physical-device editor and export checks, signed release provider verification. Draft persistence, replacement prompts and the remaining milestones are unfinished.

### Decisions to settle during M0

- Confirm free/no-ads/no-account launch assumptions and the intended audience.
- Confirm developer account type/date and access to required testers.
- Confirm package/name ownership, support contact and privacy hosting.
- Choose the editor approach after measuring the existing implementation.
- Decide whether hosted shareable links are essential for launch. If yes, revise scope for hosting, privacy, abuse handling, deletion and operating costs before implementation.

