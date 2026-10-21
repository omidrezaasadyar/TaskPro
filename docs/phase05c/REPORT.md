# Phase 05C - Tasks Timeline and Groups

Scope: Screen 03 only, with approved D05 Timeline and D09 task-draft/group-rename
behavior. Phase 05D, Vault redesign, publication and Git operations are excluded.

## Preservation

- Branch: `main`; HEAD: `faadf57eb4911089181bde52fe93f2b90a710266`.
- Starting inventory: 213 tracked/nonignored files, including 27 modified tracked
  files and 136 untracked files; nothing staged. Existing work was not discarded.
- Verified private recovery copy: `phase05c-20261021/baseline-8dd91ef7acdf` beneath
  the local Codex visualization directory. It contains source copies, SHA-256
  manifest, original Git status/HEAD, staged/unstaged patches and recovery notes.
- Corresponding ZIP SHA-256:
  `BE353E489F481AE658175E110EC720E8EB39137200F12B59C57F812E2859A7B7`.
- Git metadata, ignored local files, SDK configuration, signing material, secrets
  and runtime databases were excluded. The archive and copied source hashes were
  verified before application edits. No preservation blocker was found.
- Final scope is checked against that snapshot, not just HEAD: earlier phases
  contain valuable uncommitted and untracked implementation.

## Implementation

### Timeline and dates

The new `tasks` route is the default Tasks destination. The existing `home` group,
`item/{itemId}`, status and search routes remain available. Timeline/Groups use a
working segmented selector; Groups is one tap away. Explicit Tasks-tab and Home
Tasks-card selection return to Timeline. Explicit chooser selection retains the
existing saved-stack behavior. Back from Groups returns to Timeline, and nested
task screens retain the selected Tasks bottom tab.

`TasksViewModel` combines the existing joined Room task/group projection, live
group counts and the existing lifecycle-scoped clock helper. It has loading,
failure and retry states. No task database, persisted type, index or settings key
was added. Home and chooser visual sources are unchanged.

- Default: All dates, all three real statuses. Date/status/notes/due filters are
  AND-combined, and reset controls remain accessible.
- Date strip: seven Gregorian local dates, centered on the current selection.
  Narrow/large-font layouts scroll horizontally with wider cells; every day has
  a full-date accessibility label. Persian uses localized compact weekday labels,
  not a Jalali calendar.
- Calendar toolbar menu: Choose date, Today, Previous week, Next week. Calendar
  picker selection switches to that local day. All dates and Undated stay visible.
- Local-day membership is `[local midnight, next local midnight)`. Calendar-day
  addition handles 23/25-hour DST days; the visible feed re-evaluates the device
  zone and clock while observed. Reading/filtering never rewrites `dueAt`.
- Overdue: an unfinished Pending or Snoozed task with `dueAt < now`, consistent
  with Home's unfinished-task definition. It is not a persisted status or proof
  that an alarm is currently armed.
- Selected day/Today: dated tasks in the selected local day not already Overdue.
- Earlier dates: older dated tasks not already Overdue, principally completed
  tasks. Upcoming: dates after the selected day. Undated: `dueAt == null`.
- Every eligible task appears once. Each section sorts by actual dueAt then
  stable ID; null dates sort by ID. Manual group positions are never projected
  back into storage.
- Status menu: All statuses, Pending, Completed, Snoozed. Task filters: With notes
  (`notes.isNotBlank`) and Has due date (`dueAt != null`), not an invented reminder
  delivery state or independent Notes entity.

No recurrence, priority levels, tags, subtasks, Vault links, completion timestamps
or completion-history claims were introduced.

### Groups, task rows and editor

Groups retain existing IDs, relationships, live counts, creation, sharing/export,
confirmed cascade deletion, status lists and task search. A reachable Rename group
menu calls the existing `HomeViewModel.renameItem` and repository behavior. Custom
stored group colors remain usable; otherwise the Tasks accent is used.

The existing name dialog now uses bounded responsive width and simple string
input state. This fixes a verified rename-dialog layout/idling failure. Create
actions reuse the approved shared primary button with an optional compact mode;
its existing default behavior is unchanged. The action occupies reserved space,
so it does not cover task rows. It replaces the old repositionable group-add FAB
presentation with a fixed accessible New group action.

Both task views use the approved shared TaskRow through a small management wrapper.
Text/body, completion checkbox, overflow and manual-order affordances are distinct.
Notes are a two-line summary; the editor retains their complete value. Timeline
has no drag handler. Pending bodies edit; completed/snoozed Timeline bodies open
the existing owning status list. Status-list bodies do not pretend to be editable.
Their completion/reopen/delete actions remain available, with delete confirmation.

Group pending lists retain long-press drag. Header controls are outside the lazy
task list so its item indexes match the local task indexes. All actions are
disabled during dragging; release has a short edit-suppression guard. Overflow
also offers Move up/Move down. Reordering updates only pending-task positions in
one transaction, never stale titles, notes, dates or status.

The existing AddTaskSheet serves creation and editing:

- Timeline creation requires explicit selection of an existing group. There is no
  automatic first-group choice. Group creation retains its owning group context.
- Existing title, full notes and date/time are prefilled. Clean Cancel closes;
  changed Cancel, dismissal or Back requests Keep editing / Discard changes.
- Dirty-state and saving guards are read through updated state by the remembered
  sheet, including swipe/accessibility dismissal. No draft autosave was added.
- Save is disabled in flight. The form waits for the suspend save, retains the
  draft on failure and closes on success. Inputs, date/time and Cancel respect
  the busy state; controls reflow/scroll around the keyboard.
- A transactional DAO edit loads the current Pending row and changes only title,
  notes and dueAt. ID, group, creation date, current status and latest position
  survive. Deleted or no-longer-Pending edits fail without recreating a row.
- Date clearing still cancels the reminder. New date picks correctly translate
  Material's UTC calendar-day token to the chosen local Gregorian date while
  retaining the existing local time. This corrects west-of-UTC off-by-one-day
  selection; stored dates are not migrated or rewritten.

### Reminders and security

Existing scheduler, receivers, notification channel/action identifiers,
PendingIntent identity and reminder popup are unchanged. The repository reuses
the same schedule/cancel logic after edits. Completing cancels; reopening a future
task rearms; list Snooze changes status/cancels without changing dueAt. Popup
Snooze 10 min changes dueAt and rearms, including boot rescheduling.

No Vault source, master/recovery logic, encryption/AAD/Keystore metadata, clipboard
policy, secure-window rules, session timing or backup formats changed. Navigation
continues to consume only the existing Vault lock boolean outside Vault.

## Exact File Changes

Existing baseline files modified:

| Repository-relative path | Reason |
| --- | --- |
| `app/src/main/java/com/taskpro/app/data/TaskDao.kt` | Atomic field-only pending edit and position-only reorder; no schema change. |
| `app/src/main/java/com/taskpro/app/data/TaskRepository.kt` | Fresh-ID status action and edit/scheduler integration. |
| `app/src/main/java/com/taskpro/app/ui/AppViewModelProvider.kt` | Register TasksViewModel. |
| `app/src/main/java/com/taskpro/app/ui/components/NameDialog.kt` | Responsive group dialog and reliable input state. |
| `app/src/main/java/com/taskpro/app/ui/components/shared/GeneralComponents.kt` | Optional compact primary action; defaults preserved. |
| `app/src/main/java/com/taskpro/app/ui/components/shared/TaskComponents.kt` | Optional compact metadata/menu/body state and accessible body sizing. |
| `app/src/main/java/com/taskpro/app/ui/navigation/NavGraph.kt` | Timeline integration and Tasks-tab ancestry, preserving old routes. |
| `app/src/main/java/com/taskpro/app/ui/navigation/Screen.kt` | Add `tasks`; do not rename existing routes. |
| `app/src/main/java/com/taskpro/app/ui/screens/home/HomeScreen.kt` | Groups presentation and reachable existing management callbacks. |
| `app/src/main/java/com/taskpro/app/ui/screens/item/AddTaskSheet.kt` | Explicit group selection, guarded asynchronous save, dirty dismissal, reflow and date conversion. |
| `app/src/main/java/com/taskpro/app/ui/screens/item/ItemDetailScreen.kt` | Shared task rows, status access, drag/index safety and accessible reorder. |
| `app/src/main/java/com/taskpro/app/ui/screens/item/ItemDetailViewModel.kt` | Editor and fresh-ID action entry points. |
| `app/src/main/java/com/taskpro/app/ui/screens/item/ItemStatusListScreen.kt` | Truthful shared status rows and flat task canvas. |
| `app/src/test/java/com/taskpro/app/dashboard/DashboardNavigationTest.kt` | Add the approved Groups tap to existing group-access assertions. |
| `app/src/test/java/com/taskpro/app/vault/WorkspaceLaunchTest.kt` | Same selector adaptation; prior preservation/security assertions retained. |
| `app/src/test/java/com/taskpro/app/vault/WorkspaceNavigationTest.kt` | Same selector adaptation; Vault-session assertions retained. |

New application/resource files:

- `app/src/main/java/com/taskpro/app/ui/screens/tasks/TimelineState.kt`
- `app/src/main/java/com/taskpro/app/ui/screens/tasks/TasksViewModel.kt`
- `app/src/main/java/com/taskpro/app/ui/screens/tasks/TimelineScreen.kt`
- `app/src/main/java/com/taskpro/app/ui/screens/tasks/ManagedTaskRow.kt`
- `app/src/main/res/values/tasks_strings.xml`
- `app/src/main/res/values-fa/tasks_strings.xml`

New tests under `app/src/test/java/com/taskpro/app/tasks/`:

- `TimelineStateTest.kt`
- `TasksDataTest.kt`
- `TaskEditorTest.kt`
- `TasksNavigationTest.kt`
- `TimelineInteractionTest.kt`
- `TasksVisualTest.kt`

New documentation/evidence: this `REPORT.md`, `create-comparison.ps1`,
`visual-review/VISUAL_REVIEW.md` and the 21 exact PNG filenames listed in that
visual review (20 native captures plus one comparison). No earlier documentation
or reference image is overwritten.

## Validation

Baseline was executed before edits: **181 tests passed**, no failures/errors/skips;
debug assembly, release-test compilation and lint succeeded with 0 errors and
60 warnings. Local toolchain: JDK 21.0.8, Android SDK 35 with the existing ignored
local SDK init script/build-tools 36 workaround. CI configuration was not changed.

Final validation uses:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\openjdk\jdk-21.0.8'
$env:ANDROID_HOME='C:\Program Files (x86)\Android\android-sdk'
.\gradlew.bat -I .gradle/local-sdk.init.gradle :app:testDebugUnitTest --rerun :app:lintDebug :app:assembleDebug :app:compileReleaseUnitTestKotlin --console=plain
```

Results are recorded from the final XML/build output, not estimated:

- Full suite: **230 passed**, 29 suites, 0 failures/errors/skips (181 retained plus
  49 new tests).
- `lintDebug`: **0 errors, 66 warnings**. The 60 baseline warning signatures remain;
  six additional UnusedResources warnings identify retained legacy task UI assets/
  strings: illustration_empty_tasks, drawer_subtitle, add_new_item, item_name_label,
  rename_item, add_first_task. These are deliberately not deleted in this phase.
- `assembleDebug`: successful. `compileReleaseUnitTestKotlin`: successful.
- Existing SDK package.xml marshalling/read-only messages remain environmental.
- `git diff --check`: no whitespace errors; existing Windows line-ending conversion
  advisories are not a source rewrite or build failure.

Executed new tests:

| Suite | Tests | Verification |
| --- | ---: | --- |
| TimelineStateTest | 9 | Unique sections, undated/status/notes/due filters, boundaries, stable order, Gregorian year crossing, 23/25-hour DST days and zone changes. |
| TasksDataTest | 9 | Real in-memory Room: same-ID edits, stale status/deletion rejection, stale reorder safety, explicit group creation, AlarmManager identity/rearm/clear/status/boot behavior, distinct popup snooze, counts/rename/cascade, text sharing, UTC/local picker conversion. |
| TaskEditorTest | 10 | Prefill, clean cancel, dirty keep/discard, sheet accessibility dismissal, explicit group selection, duplicate-tap guard, failed-save retry, due clearing, independent row actions, drag-disabled actions, truthful non-editable completed body/reopen. |
| TasksNavigationTest | 8 | MainActivity: default Timeline/one-tap Groups/Back/tab ancestry, real rename/create/delete, Timeline creation, same-ID edit/cancel, accessible manual moves and actual pointer long-press drag release without edit. |
| TimelineInteractionTest | 3 | Working calendar/scope/status/notes/due controls and keyboard activation of Groups. |
| TasksVisualTest | 10 | Production native Android/Compose captures, reachable last rows, no text-height clipping, width checks where native metrics are reliable, and 48-dp enabled controls. |

All existing backup/restore, migration, legacy-backup, master/recovery, encrypted
storage, external-intent, device-unlock/session, chooser and Home tests ran in the
full suite. Existing Room migration coverage remains in BackupRepositoryTest.
Robolectric runs use synthetic app data only; no installed user's database was
opened. Baseline and final JUnit/lint evidence is preserved privately.

Failures encountered during implementation were not hidden: small selector/body
targets, action overlap, stale remembered sheet guards, group dialog width and
animated create-label semantics were corrected. Popup-only test selectors avoid
matching task metadata behind a menu. Native RTL combining-mark paragraph metrics
proved unreliable; those text runs receive direct pixel review instead of a false
claim of automated width verification. Physical Back is not inferred from an
Activity dispatcher call while a modal owns the event; Cancel and actual sheet
dismissal paths are executed, with onDismiss/Back guards also inspected.

## Compatibility and Delivery

No change to `com.taskpro.app`, namespace/packages, `taskpro.db`, Room version 3 or
its migrations/entities, DataStore keys/values, backup identifiers, encryption,
reminder identities or signing configuration. Existing Task IDs and group
relationships are not migrated. Existing manual positions change only through
the user's reorder/status actions. Build version remains 2.0.4 / code 9.

Final source comparison: 16 of the 213 starting files intentionally modified,
197 byte-identical, none missing; 36 new files (52 phase-scoped changes in total).
MainActivity, chooser/Home visual sources, Vault, backup, settings, reminder
platform sources, manifests, Gradle and CI files match the recovery baseline.

Final debug APK SHA-256:
`C403698CD4EFA0A4E6EE88F9DD742D8926B2E5405056608DA13D5CA5CB87D9C1`.
The last full validation completed successfully in 53 seconds. This is a local
debug artifact, not a signed release or published download.

Visual files and reference comparison findings: [Visual Review](visual-review/VISUAL_REVIEW.md).
These are **native Robolectric Android/Compose renders**, not phone/emulator screen
captures, illustrative replacements or Phase 04B component previews. No device was
attached when ADB was checked; the configured SDK has no emulator executable.

## Remaining Limits and Phase 05D Readiness

- Owner review of the delivered reference comparison is still required. Necessary
  differences are documented; the design is not described as a pixel-identical mock.
- Physical TalkBack, real IME/hardware keyboard, Android 12/OEM alarm delivery,
  permission revocation, system bars/cutouts, real rotation/process death during a
  save, foldables and system reduced-motion settings need device QA. Host rendering
  and simulated lifecycle tests do not certify these platform behaviors.
- No new draft persistence, completion history, Jalali calendar, recurrence,
  priority/tag/subtask model or completed/snoozed editor is introduced.
- Room edits/reorders are transactional; Android AlarmManager is a separate OS
  service, not part of a Room transaction. Existing OS permission/delivery limits
  remain, including the need to verify rare scheduler-failure behavior on device.
- No release signing, JDK-17 CI execution, installation, remote CI run, commit,
  push, publication or release creation was performed.

Phase 05C is ready for owner visual review and physical-device QA. Its storage and
navigation foundations are suitable for a separately approved Phase 05D, but no
Phase 05D work has started. Do not proceed automatically.
