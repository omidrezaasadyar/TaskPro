# Vaurivo Phase 04A: reference analysis

Status: design proposal for review, 2026-10-20. No UI implementation is authorized
by this document. The supplied images are the primary visual references; source
code is authoritative for existing functionality. Proposed measurements below are
Android layout values, not claims that screenshot pixels equal device-independent pixels.

## Reading order

1. This reference/evidence inventory.
2. [Design tokens](DESIGN_TOKENS.md).
3. [Component specification](COMPONENT_SPEC.md).
4. [Screen specifications](SCREEN_SPECIFICATIONS.md).
5. [Navigation and accessibility](NAVIGATION_UX.md).
6. [Feature gap matrix](FEATURE_GAP_MATRIX.md).
7. [Implementation stages and QA](IMPLEMENTATION_PLAN.md).
8. [Open decisions and approval gates](OPEN_DECISIONS.md).

## Reference provenance

Both original files were accessible and all seven concepts were visually inspected.
The user explicitly approved preserving both images in this repository during this
phase. Copies were verified byte-for-byte using SHA-256; neither image was edited,
cropped, regenerated, uploaded to an external service, or installed as an app asset.
Illustrated account values are not transcribed into fixtures or treated as real data.

| Original | Stable project-relative copy | Dimensions | SHA-256 |
| --- | --- | --- | --- |
| Owner-supplied `1.png` | `docs/phase04a/references/01-launch-unlock-home-tasks.png` | 1740 x 841 px | `77C6D976391EA95271EFA074F23067833A4A990D26F74D8746433B226EC47934` |
| Owner-supplied `2.png` | `docs/phase04a/references/02-vault-details-editor.png` | 1275 x 847 px | `44414C0862C3C120B6ADA5A614365ED0611DA02B4C966BF3894F2DB5F8A2530A` |

![References 01, 02, 03 and 00](references/01-launch-unlock-home-tasks.png)

![References 04, 05 and 06](references/02-vault-details-editor.png)

### Screen map and observed composition

Coordinates are approximate outer phone bounds in the original image, not crop instructions.

| Screen | Image / bounds x,y to x,y | Visible regions and hierarchy | Design assessment |
| --- | --- | --- | --- |
| 00 Launch / Choose section | Image 1, 1345,45 to 1722,841 | Brand/version header; security symbol with restrained rings/dots; two-line headline; short description; side-by-side Tasks/Vault cards; remember-choice row. No bottom tabs. | Strong module identity; introductory prominence should not become a repeated launch barrier. Brand and version are obsolete in the mock. |
| 01 Vault Unlock | Image 1, 26,45 to 402,841 | Vault title/settings; centered lock tile/title; item-count/timeout subtitle; fingerprint rings; instruction; bottom PIN/master buttons; persistent tabs. | Clear fallbacks and calm focus. Locked item count leaks metadata; button labels and muted captions need larger-text testing. |
| 02 Home / Workspaces | Image 1, 465,45 to 842,841 | Date/greeting/settings; search; progress ring and counts; paired module cards; upcoming task rows; bottom tabs. | Useful scan order. Scope, progress denominator and privacy boundaries are undefined; some small labels are too faint for a reliable production specification. |
| 03 Tasks | Image 1, 906,45 to 1282,841 | Title/calendar/filter tools; seven-day strip; chips; Overdue/Today sections; checkbox task rows; metadata/subtask progress; extended create action; bottom tabs. | Dense but navigable. Weekly recurrence, Vault links, tags, subtask progress and completion time are product proposals, not existing data. |
| 04 Vault | Image 2, 6,45 to 382,841 | Vault title/sort/lock; search; health card; two-column colored groups; recently used row; extended Add Item; tabs. | Folder identity is strong. Health numbers and recently-used ordering lack current data; Add Item visually overlaps the last row in the reference. |
| 05 Vault item | Image 2, 445,45 to 823,841 | Back/favorite/edit/menu; account identity; stacked field cards; password meter; 2FA card/timer; website/notes; privacy footer. No tabs inside detail. | Good field separation. Copy targets need room; sensitive text must not become accessibility/preview leakage. TOTP and guaranteed clipboard clearing cannot be copied literally. |
| 06 Add item + generator | Image 2, 886,45 to 1262,841 | Dimmed parent; rounded modal top/handle; title/Cancel; record-type row; title/group and username; generator controls; full-width Save. | Strong form/action hierarchy. Screenshot shows only part of the required data model. Sheet must expand for keyboard/custom fields; type selector cannot imply payment/TOTP support. |

## Shared visual language

Use deep green-charcoal canvases, slightly lighter flat surfaces, fine separators,
near-white text, compact bold titles and restrained outlined icons. Lavender marks
Tasks; mint marks Vault; coral/amber carry urgency, not arbitrary task priority.
Repeated items may use rounded frames; whole screen sections remain unframed and
are never nested floating cards. Use ordinary Compose/Material icon assets already
available, styled to the reference rather than applying a new generic template.

Approximate reference cues: 16-20 dp screen margins, 12-16 dp internal spacing,
roughly 16 dp repeated-card corners, 12 dp fields/buttons, 20-24 dp modal top
corners, 20-24 dp icons, and a clear bottom action zone. These are proposed optical
translations; screenshots do not establish original dp, font family, touch bounds
or exact color values. [DESIGN_TOKENS.md](DESIGN_TOKENS.md) makes them explicit.

The larger hero treatment is confined to optional introduction 00. Operational
Home, Tasks and Vault remain compact. Retain only local, low-opacity emphasis on
00 and the fingerprint target; no full-page gradients, extra illustrations, glow
blobs, animated backgrounds or new Lottie dependency. Existing user-selected
background styles cannot be silently removed; see decision D12.

## Inconsistencies and recommended corrections

- 00 omits Home navigation while 01-04 expose it. Treat 00 as an explicit chooser
  or approved first-run introduction, not a second compulsory dashboard.
- The reference title says TaskPro and version 1.0.0; use Vaurivo and runtime
  BuildConfig. Do not reuse that symbol as an approved permanent logo.
- 01 shows 38 items while locked; 02 repeats Vault item/group counts. Recommended
  privacy contract: no Vault content/counts on locked surfaces; Home stays metadata-free.
- 01 conflates inactivity with one minute away. Existing foreground timeout defaults
  to 120 seconds; away timeout is 60 seconds. Distinguish them in copy.
- Health score 82, weak/reused/old counts, 2FA timer, weekly badge, task link and
  completion time are not verified current capabilities. Hide, do not simulate them.
- Low-emphasis captions, completed rows and dark checkbox outlines may be too faint.
  Retain the hierarchy but use tested semantic colors and 48-dp hit regions.
- Treat custom group colors as identity accents, never the only identifier or
  uncontrolled text color. Names and icons remain readable at any chosen RGB value.
- The editor/detail layouts omit phone/reference/custom fields, tags, timestamps,
  duplicate/delete, recovery and category management. Progressive disclosure may
  organize these controls but must not remove them.
- A literal two-column grid or PIN/master row can fail at 320 dp and 200% font size.
  Stack when content needs it; never shrink fonts to keep the screenshot silhouette.

## Verified repository evidence

Paths and line numbers refer to the inspected working tree, not just committed HEAD.
Use these evidence IDs throughout the other documents; recheck anchors before coding.

| ID | Exact source and entry point | Verified facts |
| --- | --- | --- |
| E01 | `app/src/main/java/com/taskpro/app/ui/navigation/Screen.kt:4`; `app/src/main/java/com/taskpro/app/ui/navigation/NavGraph.kt:32` | workspace/home/vault/search/settings/item/status routes; workspace start; saved-tab switching. `home` currently means task groups. |
| E02 | `app/src/main/java/com/taskpro/app/MainActivity.kt:32`, `:80` | Edge-to-edge; restore recovery gates navigation; app lifecycle manages Vault foreground and owned clipboard cleanup. |
| E03 | `app/src/main/java/com/taskpro/app/ui/screens/WorkspaceScreen.kt:33` | Selector, resource-backed brand, two module cards, entrance/press/hover feedback; no remember-choice preference. |
| E04 | `app/src/main/java/com/taskpro/app/ui/screens/home/HomeScreen.kt:88`, `:234`; `app/src/main/java/com/taskpro/app/ui/screens/home/HomeViewModel.kt:16` | Task-group listing/counts/create/share/delete; rename method/dialog exist but no reachable assignment to renameTarget was found. Not a working dashboard or working rename entry point. |
| E05 | `app/src/main/java/com/taskpro/app/ui/screens/item/ItemDetailScreen.kt:79`, `:202`, `:247`; `app/src/main/java/com/taskpro/app/ui/screens/item/ItemDetailViewModel.kt:48`; `app/src/main/java/com/taskpro/app/ui/screens/item/AddTaskSheet.kt:52` | Pending manual order, status routes, separate status actions, tap-to-edit, shared add/edit fields. Editing status-list cards is not wired. |
| E06 | `app/src/main/java/com/taskpro/app/data/Task.kt`; `app/src/main/java/com/taskpro/app/data/TaskDao.kt:20`, `:28`; `app/src/main/java/com/taskpro/app/data/TaskRepository.kt:44` | title/notes/dueAt/status/position/createdAt, stable Long IDs; search title AND notes; reminders tied to dueAt; no recurrence, subtasks, tags, completion timestamp or Vault link. |
| E07 | `app/src/main/java/com/taskpro/app/ui/components/TaskCard.kt:50`; `app/src/main/java/com/taskpro/app/ui/components/DragDropList.kt:102` | Direction-aware title/notes, optional card click, independent controls, long-press reorder. |
| E08 | `app/src/main/java/com/taskpro/app/ui/screens/search/SearchViewModel.kt:24`; `app/src/main/java/com/taskpro/app/data/TaskDao.kt:28` | Task search only, debounced 200 ms; no cross-module results. |
| E09 | `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultScreen.kt:52`, `:125`, `:207` | Secure-window lifecycle; categories/folders, search/sort/tags/favorites, counts; sidebar at 840 dp, local detail/editor states. |
| E10 | `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultScreen.kt:346`; `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultEditors.kt:104` | Nested categories; collapse/search/rename/move/reorder/delete; icons, RGB editing, optional starters; category deletion keeps records uncategorized. |
| E11 | `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultScreen.kt:399`, `:412`, `:424` | Mask/reveal/copy; title/category/tags/timestamps; edit/duplicate/delete. External action only on permitted non-sensitive valid URL fields. |
| E12 | `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultEditors.kt:36` | Generic record editor preserves UUID; seven standard fields, category, tags/favorite and typed custom fields; local zxcvbn 0-4 estimate; no record-type discriminator or discard confirmation. |
| E13 | `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultComponents.kt:49`, `:142`, `:152`; `app/src/main/java/com/taskpro/app/vault/VaultModels.kt:7`, `:29`, `:89` | Secret input semantics/reveal; generator 12-128, default 24; TEXT/SECRET/URL/EMAIL/NUMBER/DATE/NOTES custom types; search excludes password, standard notes and secret custom values. |
| E14 | `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultUnlockScreen.kt:48`, `:99`, `:142`, `:218` | Strong biometric CryptoObject, optional PIN/master, setup, cooldown/errors, cancellation; existing locked screen is dark even in light app theme. |
| E15 | `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultMasterSetup.kt:23`; `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultLogin.kt:32`, `:89`, `:115` | Explicit master creation, protected previous-vault backup, recovery/reset and mandatory saved-key confirmation; recovery management is not a dashboard button. |
| E16 | `app/src/main/java/com/taskpro/app/vault/VaultRepository.kt:11`, `:45`, `:100`, `:182`, `:225`; `app/src/main/java/com/taskpro/app/vault/VaultDeviceUnlock.kt:81`, `:116` | Session generation checks, key clearing, encrypted revision-checked saves, reauthentication; foreground/away lock; persistent PIN cooldown. |
| E17 | `app/src/main/java/com/taskpro/app/vault/VaultClipboard.kt:21`, `:35` | Sensitive token-owned clipboard; 30-second best-effort cleanup only if ownership AND contents match. No clearing guarantee. |
| E18 | `app/src/main/java/com/taskpro/app/ui/screens/settings/SettingsScreen.kt:57`; `app/src/main/java/com/taskpro/app/ui/screens/settings/BackupSection.kt:50`, `:115`; `app/src/main/java/com/taskpro/app/ui/screens/settings/SettingsViewModel.kt:82` | Theme/background/About, backup scopes/picker/password/share/last date, replace confirmation and recovery gate. |
| E19 | `app/src/main/java/com/taskpro/app/backup/BackupRepository.kt:40`, `:79`, `:182`; `app/src/main/java/com/taskpro/app/backup/WorkspaceBackup.kt`; `app/src/main/java/com/taskpro/app/backup/BackupFormat.kt` | Encrypted v2 and plaintext/checksummed v1, selective replacement, transactional DB writes and durable preference/reminder journal; no merge. |
| E20 | `app/src/main/java/com/taskpro/app/data/AppDatabase.kt:11`, `:47`; `app/src/main/java/com/taskpro/app/settings/SettingsRepository.kt:15`, `:31` | Room 3 taskpro.db with migrations; settings/theme/background/last-backup store only, no module preference. |
| E21 | `app/src/main/java/com/taskpro/app/ui/theme/Theme.kt:23`, `:34`; `app/src/main/java/com/taskpro/app/ui/theme/Type.kt:15`; `app/src/main/java/com/taskpro/app/ui/theme/Color.kt:6`; `app/src/main/java/com/taskpro/app/ui/theme/StatusColors.kt:18` | Fixed, not dynamic, app-selected light/dark; bundled Vazirmatn regular/medium/bold; persisted task statuses distinct from accents. |
| E22 | `app/src/main/java/com/taskpro/app/ui/screens/WorkspaceBackdrop.kt:19`; `app/src/main/java/com/taskpro/app/ui/components/GradientBackground.kt:20`; `app/src/main/java/com/taskpro/app/util/TextDirection.kt:8`; `app/src/main/java/com/taskpro/app/util/DateFormat.kt:9` | Existing gradient/pattern preferences, first-strong task direction and locale Gregorian-style date formatting; no explicit Persian calendar preference. |
| E23 | `app/src/main/java/com/taskpro/app/ui/screens/reminder/ReminderPopupScreen.kt:69`; `app/src/main/java/com/taskpro/app/notification/ReminderScheduler.kt`; `app/src/main/AndroidManifest.xml` | Independent reminder activity, Done/Snooze 10 min/Dismiss; existing permissions/action/channel identity; no inbound Vault deep-link declaration. |
| E24 | `app/src/test/java/com/taskpro/app/vault/VaultExternalIntentTest.kt:52`; `app/src/test/java/com/taskpro/app/vault/WorkspaceNavigationTest.kt:23`; `app/src/test/java/com/taskpro/app/branding/BrandingCompatibilityTest.kt:38` | Existing regression assets to preserve, not proof of a new design implementation. |
| E25 | `app/src/main/res/values/strings.xml`; `app/src/main/res/values-fa/strings.xml`; matching vault/recovery/unlock strings; `app/src/main/res/values-fa/vault_dashboard_strings.xml` | Vaurivo/Tasks/Vault and Persian equivalents; existing runtime version labels and localized warnings. |

### Prior authoritative reports

Read [Phase 02 report](../phase02/REPORT.md), [compatibility inventory](../phase02/COMPATIBILITY.md),
[branding map](../phase02/BRANDING_MAP.md), [Phase 03 report](../phase03/REPORT.md)
and [launcher specification](../phase03/LAUNCHER_ASSETS.md). Their reported 88 passing
tests, successful build and 60 lint warnings are historical Phase 03 results, not
tests rerun in Phase 04A. The new design has not been built or live-audited.

## Scope and evidence limits

Starting branch main; HEAD faadf57eb4911089181bde52fe93f2b90a710266. There are 123
tracked/non-ignored starting files, including valuable dirty/untracked work.
Starting file hashes and Git state were recorded read-only for final comparison.
No existing code, tests, build configuration, resources or earlier documentation
may change in this phase. Only the eight requested Markdown files and the two
explicitly approved reference copies belong to this phase.

Visual inspection establishes composition, not live interaction, exact typography,
accessibility compliance, biometric behavior or measured original contrast. This
is a reference/source design specification, not a physical-device usability audit.
No application tests, lint or build are claimed executed for this documentation phase.
