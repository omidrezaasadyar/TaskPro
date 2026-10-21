# Phase 05C-R1 - Tasks Timeline Visual Refinement

Date: 2026-10-21. Scope: Tasks presentation only. Stopped for owner visual approval.

## Preservation

- Branch `main`; HEAD `faadf57eb4911089181bde52fe93f2b90a710266`, unchanged.
- Starting tree: 249 tracked/nonignored files, 33 modified tracked files and 172 untracked files; nothing staged. These changes belong to earlier work, not this phase.
- Before application edits, a private recovery copy, manifest, Git status, HEAD, staged/unstaged patches and ZIP were created. Source/copy/archive hashes were verified. No reset, clean, stash, commit, push, install or publication was performed.
- Recovery folder: `phase05c-r1-20261021/baseline-794f285d2c01` (private, off-repository).
- Sibling ZIP SHA-256: `2C82194F291C24425497A0F5B90E6A12C39E038140C29C84EAA723AC2E2ABB12`.
- Snapshot excludes ignored build/runtime files, databases, local SDK configuration, signing keys and Git metadata. Filename/signature checks found no preservation blocker; these checks are not a guarantee that every possible secret can be recognized. The snapshot remains private, outside the repository.
- Final hash comparison: **241 of 249 starting files are byte-identical; only the eight files listed below changed. No starting file was deleted.** New files are limited to the filter panel and this phase's review artifacts.

## Application Changes

| File | Change |
| --- | --- |
| `app/src/main/java/com/taskpro/app/ui/screens/tasks/TimelineScreen.kt` | Compact Timeline/Groups tabs, 48dp date strip, filter-sheet entry, active-filter dot/accessibility state and wrapping scope caption; tighter section spacing; opt-in dense Timeline rows. |
| `app/src/main/java/com/taskpro/app/ui/screens/tasks/TimelineFilterPanel.kt` (new) | Material 3 bottom sheet with date/status choices, notes/due toggles, reset, close and the existing Search action. No new persistence or query layer. |
| `app/src/main/java/com/taskpro/app/ui/screens/tasks/ManagedTaskRow.kt` | Forward optional `dense` presentation flag, default false. Existing actions untouched. |
| `app/src/main/java/com/taskpro/app/ui/components/shared/TaskComponents.kt` | Opt-in compact-row vertical padding of 4dp instead of 8dp; body remains at least 48dp after padding. Other consumers retain existing defaults. |
| `app/src/main/res/values/tasks_strings.xml` | Four localized filter-panel/accessibility labels; existing keys and values preserved. |
| `app/src/main/res/values-fa/tasks_strings.xml` | Persian equivalents of those four labels. |

## Test And Documentation Changes

- `app/src/test/java/com/taskpro/app/tasks/TimelineInteractionTest.kt`: adapted existing filter tests to the panel; added default-header/reset checks, retained search, active-scope indication and saved-state restoration tests. Existing keyboard tab test retained.
- `app/src/test/java/com/taskpro/app/tasks/TaskEditorTest.kt`: retained normal-row independence test and added the dense Timeline equivalent, including touch-target checks.
- `app/src/test/java/com/taskpro/app/tasks/TasksVisualTest.kt`: retained all ten earlier render cases; added English/Persian/200% text-size filter-sheet captures, actual dialog-window composition, active-filter states and layout checks. Correct native locale/font-scale configuration is applied for dialog windows.
- `docs/phase05c-r1/create-comparison.ps1`: reproducible crop/comparison only; no modification of application pixels.
- `docs/phase05c-r1/visual-review/`: 26 native-rendered PNGs, one labeled comparison, SHA-256 manifest and [visual QA](visual-review/design-qa.md).
- This report. Earlier phase reports and screenshots are unchanged.

## Behavioral Preservation

- **Same query semantics:** `TimelineFilter`, `timelineSections`, date calculations, sorting and status matching are unchanged. Filters still combine as before; this is a relocation of controls, not a new search/query implementation.
- Default remains **All dates / All statuses**, without notes/due restrictions. All dates also resets the selected date exactly as before. Undated retains the date-strip anchor. Selected date uses the existing selected date, or Today when no date has been selected.
- Notes and Has due date retain their existing meanings. No new reminder predicate was invented. The existing status choices remain Pending, Completed and Snoozed.
- Panel selections apply immediately. Close/dismiss does not clear them. Reset explicitly returns all filters and date selection to their defaults. Nondefault scopes remain visible in the caption and filter-icon accessibility state after closing.
- Gregorian calendar, seven-day selection, Today and previous/next-week actions are retained. The calendar remains the way to choose an arbitrary date.
- Timeline/Groups callbacks and routes are unchanged. The shared selector is visually updated in Groups too; group rows, manual ordering and drag/drop behavior are unchanged.
- Task body, completion checkbox and overflow menu remain independent. Pending-task editing, completed/snoozed status navigation, group associations, delete confirmation and reminder actions retain their existing callbacks.
- New Task action, explicit group selection, bottom navigation, loading/error/empty states and read-only/busy/drag guards remain intact.
- No database/schema, DAO, repository, ViewModel, migration, IDs, routes, preferences, authentication, encryption, backup, reminder or notification code changed. Phase 05A, Phase 05B and Vault source files are byte-identical to the starting snapshot.

## Executed Validation

Environment: Windows; installed JDK 21.0.8 and Android SDK; existing ignored `.gradle/local-sdk.init.gradle`. Native Compose tests use Robolectric SDK 28 with native graphics. All samples are synthetic; no real Vault records or credentials were used.

Before changes:

```powershell
.\gradlew.bat -I .gradle/local-sdk.init.gradle :app:testDebugUnitTest --rerun :app:lintDebug :app:assembleDebug :app:compileReleaseUnitTestKotlin --console=plain
```

Baseline: **230 tests / 29 suites, zero failures/errors/skips; lint zero errors / 66 warnings; debug build and release-test compilation passed.**

Focused command after implementation:

```powershell
.\gradlew.bat -I .gradle/local-sdk.init.gradle :app:testDebugUnitTest --tests 'com.taskpro.app.tasks.*' --console=plain
```

Result: **56 passing Tasks tests**. The first run found one Persian screenshot-harness locale mismatch; it was corrected using the actual Android dialog's locale, not by altering application translations. The rerun passed. Additional native dialog layout checks were subsequently exercised in the final full run.

Final full command: same full command as baseline, with `--rerun`. **BUILD SUCCESSFUL** (1m 17s).

| Validation | Result |
| --- | --- |
| Complete debug unit/regression suite | **237 tests, 29 suites, zero failures/errors/skips**; all earlier 230 cases retained |
| `TimelineInteractionTest` | 6 passed: panel scope/status/notes/due, All dates/Undated, calendar/week access, reset, closed-state summary, saved-state restore, Search and keyboard switching |
| `TasksNavigationTest` | All 8 existing tests passed: Timeline/Groups/navigation, group operations/ordering, actual pointer drag, create/edit/cancel flows |
| `TaskEditorTest` | 11 passed, including separate body/checkbox/overflow checks in both normal and dense modes |
| `TimelineStateTest` / `TasksDataTest` | All 18 passed: existing filter/date/DST/data/status/reminder contracts |
| `TasksVisualTest` | 13 passed; EN/FA dark, light, 320dp at 200% text, landscape, Groups, reference size and real sheet windows |
| Other regression suites | Passed, including backup, branding compatibility, Home, shared components, reminder UI, Vault crypto/unlock/recovery/integration and workspace navigation |
| Lint Debug | **0 errors, 66 warnings**, unchanged from baseline |
| Debug APK | Built successfully at `app/build/outputs/apk/debug/app-debug.apk`; not installed or published |
| Release unit-test Kotlin compilation | Passed; not a release APK build or release test execution |
| Final source/scope review | Eight existing files changed as listed, one new application file; 241 existing files unchanged; nothing staged |

Lint comparison used issue ID + message + source path, ignoring shifted line numbers: **no added or removed issues**. All 66 are inherited from Phase 05C (60 older warnings plus the six previously retained unused legacy Tasks resources). Existing SDK `package.xml` read-only marshalling messages and Git CRLF advisory messages are environmental/baseline notices, not new lint regressions. Before/after JUnit and lint XML copies are preserved privately beside the recovery snapshot.

Debug APK SHA-256: `174ABE026F5909C6C043F8769EDBC5C85DA30A82DCC4A3E529338C05ACAF9142`.

## Visual Evidence And Findings

These are **actual production Compose components rendered through Android/Robolectric native graphics**, not generated mockups and not physical-device/emulator screenshots. Activity and real Material sheet-dialog windows are drawn at their measured positions. No real data is shown.

- [Refined Dark English](visual-review/timeline-dark-en.png), 360x778.
- [Refined Dark Persian](visual-review/timeline-dark-fa.png), 360x778.
- [Open filter panel](visual-review/timeline-filters-en.png), 360x778; [Persian panel](visual-review/timeline-filters-fa.png).
- [Original reference vs refined implementation](visual-review/timeline-reference-comparison.png), 768x779. The reference Screen 03 content crop is normalized from 356x715 to 360x723; the actual comparison capture is natively 360x723. Mock status/gesture/phone areas are excluded.
- [Closed active-filter scope](visual-review/timeline-filters-en-active.png), [200% filter sheet](visual-review/timeline-filters-large.png), [200% English](visual-review/timeline-en-large.png), [200% Persian](visual-review/timeline-fa-large.png), [landscape after scrolling](visual-review/timeline-landscape-scrolled.png), [light theme](visual-review/timeline-light-en.png).

Review outcome: the oversized segmented surfaces and default filter row are gone. Tabs and date cells each lose 8dp of prior height, and the entire 48dp default filter-control row is removed. Section gaps and Timeline-only row padding are reduced. The reference's flat hierarchy, lavender day selection, separators and independent row actions are retained without reducing touch targets.

Intentional differences from Screen 03: approved Timeline/Groups tabs are retained; unsupported mock-only Vault links, recurrence badges and subtask progress are not invented; real group/full-date metadata, truthful status labels, chronological ordering and additional Upcoming/Undated sections remain. Existing fonts, icon family, header action styling and bottom navigation are not redesigned in this limited phase. Active filtering adds a compact wrapping caption only when needed. Large text wraps and dates scroll horizontally; short landscape screens require vertical list scrolling, with persistent actions still accessible.

Exact local review directory:

`docs/phase05c-r1/visual-review/`

## Limitations And Stop

- No physical-device, OEM, TalkBack, hardware keyboard or Android 12 instrumentation run was performed. Semantic keyboard/touch checks and native Android/Compose rendering are automated evidence, not a substitute for those device checks.
- No benchmark or guarantee of pixel identity with the mock is claimed. The design QA classifies preserved-product differences explicitly.
- No change to Tasks features, Vault, data formats or stored user data; no commit, push or publication. **Phase 05C-R1 stops here for visual approval.**
