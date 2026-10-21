# Future implementation blueprint and QA gates

Planning only. No stage in this document was implemented during Phase 04A.
The eight specifications are ready for design review, not blanket authorization
to add every pictured feature. [OPEN_DECISIONS.md](OPEN_DECISIONS.md) separates the
redesign-only path from functional additions. [Gap classifications](FEATURE_GAP_MATRIX.md)
must travel with later work items.

## Phase 04A delivery and actual verification

Created exactly these Markdown documents under docs/phase04a:

1. REFERENCE_ANALYSIS.md
2. DESIGN_TOKENS.md
3. COMPONENT_SPEC.md
4. SCREEN_SPECIFICATIONS.md
5. NAVIGATION_UX.md
6. FEATURE_GAP_MATRIX.md
7. IMPLEMENTATION_PLAN.md
8. OPEN_DECISIONS.md

With explicit user permission, also preserved the two unmodified reference images:
`references/01-launch-unlock-home-tasks.png` and
`references/02-vault-details-editor.png`. Their original/copy SHA-256 values match;
the provenance table is in REFERENCE_ANALYSIS.md. These are documentation inputs,
not runtime assets, designs implemented in Figma, or screenshots of a new build.

Executed during this documentation phase: visual inspection of all seven supplied
concepts; read-only source/document/Git inspection; image hash/dimension checks;
sRGB contrast calculation for the proposed token pairs; documentation link/source
anchor/content checks and starting-file hash/Git-state comparison. Structural
documentation checks are not Compose, device, security or integration tests.

Final checks: all 123 starting project files are SHA-256-identical, HEAD is unchanged
and nothing is staged. Only these eight Markdown files and two approved PNG copies
are added. Checked 31 local links and 56 explicit source references; code fences are
balanced and all 53 gap rows contain the required nine columns. These checks validate
documentation structure/provenance only, not proposed runtime behavior.

No Gradle tests, lint, APK build, live emulator/device flow, TalkBack session,
installed update or signing verification was performed for Phase 04A. The 88
tests/0 lint errors/60 warnings/debug build success belong to the previous Phase 03
report. Do not relabel them as fresh design validation or evidence the proposal works.

## Global implementation constraints

Before any later edit, obtain explicit authorization for that stage and its open
decisions. Inspect current dirty/untracked work and take a recoverable local source
snapshot with hashes, excluding secrets/signing/runtime data. Preserve newer work
even when it changes the files listed here; no reset, stash, automatic commit,
force push, history rewrite or extraction over a dirty checkout.

Keep the authoritative [compatibility inventory](../phase02/COMPATIBILITY.md)
frozen: com.taskpro.app packages/namespace/applicationId, TaskProApp/TaskProTheme,
taskpro.db schema/migrations, DataStore settings keys/enums, encryption/KDF/AAD/
Keystore/recovery/device formats, clipboard ownership, backup format markers,
notification/reminder identities, record IDs and signing configuration. UI-stage
work should not require a schema migration, new framework or dependency upgrade.

Only synthetic data enters fixtures, previews, test screenshots or visual diffs.
Do not export a user's live Vault to make realistic screenshots. Security-sensitive
content stays under the current authenticated session; the Stage E visual pass
does not weaken masking, clipboard ownership, lifecycle or URL intent predicates.

## Stage A - Token foundation

- Dependencies/approvals: D02 palette/shape/type; D12 background coexistence; use
  existing theme-mode selection. Final logo D11 is not needed to start colors.
- Expected existing files: `app/src/main/java/com/taskpro/app/ui/theme/Color.kt`,
  `Theme.kt`, `Type.kt`, `StatusColors.kt`; `ui/screens/WorkspaceBackdrop.kt`;
  `ui/components/GradientBackground.kt`. Add UI-only token definitions under ui/theme
  if they reduce literal duplication; retain internal TaskProTheme name.
- Components: semantic colors, spacing, radii, typography and motion provider;
  explicit module accent, not global theme mutation.
- Preserve: SYSTEM/LIGHT/DARK and PLAIN/GRADIENT/AURORA keys/values, bundled font
  weights, task status mapping and stored group/category colors. No preference reset.
- Security: no data/auth changes; locked light theme must retain secure-window
  and status/navigation-bar visibility. Do not use dynamic palette to weaken contrast.
- Tests required: token pair calculations, theme-switch semantic/color tests,
  status palette tests, EN/FA scaling, old preference loading, full existing suite.
- Visual acceptance: flat charcoal/neutral canvas, reference lavender/mint, no new
  broad gradient; tested light counterpart; readable focus/outline/status labels.
- Rollback: compare with stage-specific private source snapshot and reverse only
  its token/presentation patch after review. Keep all later/user changes and stored
  values; do not roll back the whole dirty checkout or migrate user preferences.

## Stage B - Shared Compose components

- Dependencies: approved A and component contracts; D09 for new discard/generator
  apply behavior, D13 for any explicit secret-reading policy.
- Expected existing files: `ui/components/TaskCard.kt`, `NameDialog.kt` and private
  Vault field/choice/toggle helpers in `ui/screens/vault/VaultComponents.kt`.
  Proposed new UI-only files: WorkspaceCard, SectionHeader, SearchField,
  PrimaryActionButton, BottomNavigationBar, generic state components, VaultGroupCard,
  VaultItemCard and SecureFieldCard under existing `ui/components` subpackages.
  Colocate simple private primitives instead of introducing a class for every token.
- Components: C01-C18 as needed by real screens. C02 is defined but not rendered
  until D03 supplies approved real state; no decorative nonfunctional controls.
- Preserve: callback ownership, field classification, all field types and resource
  IDs. Components receive state/callbacks and do not directly access DB/crypto.
- Security: secure wrappers must not merge decrypted text into contentDescription;
  retain input password semantics and exact clipboard values.
- Tests required: role/state/callback tests, child action versus parent tap,
  48-dp targets, keyboard focus, field types, no secret semantics, stable dimensions.
- Visual acceptance: coherent controls/spacing, no nested cards, full labels at
  320 dp/2x type in EN/FA; independent focus/copy/reveal/menu controls.
- Rollback: leave new components unreferenced and revert only stage wiring after
  diff review; do not delete untracked files owned by earlier work or user changes.

## Stage C - Home and workspace presentation

- Dependencies: A/B; D01 launch/route/preference approval; D03 aggregates and D04
  federated search are independent functional approvals, not implied by C.
- Expected files: `ui/screens/WorkspaceScreen.kt`, `ui/navigation/NavGraph.kt`,
  `ui/navigation/Screen.kt` only if reviewed routing is truly necessary; proposed
  `ui/screens/workspace/HomeDashboard.kt` and UI presentation state. Keep existing
  `ui/screens/home/HomeScreen.kt` serving Tasks groups. EN/FA strings change together.
  Any preference/query/backup change gets a separate approved subtask, not a visual diff.
- Components: WorkspaceCard, SectionHeader, SearchField, BottomNavigationBar,
  optional ProgressSummaryCard only with real approved data.
- Preserve: start-up RestoreRecovery gate, Tasks group access, singleTop/state
  behavior, locked/setup Vault routing and existing away timeout. No record counts
  or names on Home under the recommended metadata-free contract.
- Security: no combined plaintext index, secret preview or implicit unlock. Search
  baseline routes to task search; Vault search routes into authenticated Vault.
- Tests required: cold/return launch, tab switching and Back, restoring missing
  route/entity, locked module navigation and task preservation, no Vault data on
  Home; D03 day boundaries/time-zone/status tests if approved. Preference defaults
  and backup compatibility tests only if new persistence separately approved.
- Visual acceptance: 00/02 hierarchy faithful to references without a repeated
  intro barrier under approved policy; truthful metrics or omitted metric regions;
  both modules accessible at compact/expanded/large type.
- Rollback: restore prior selector presentation/callback wiring without erasing
  Tasks or Vault. If an approved preference was added, old code must safely ignore
  it; never delete DataStore or reinitialize the database to revert a launch choice.

## Stage D - Tasks interface

- Dependencies: A/B and approved C route labels; D05 date-feed/filter semantics if
  requested. D07 recurrence/subtasks/tags/links remain excluded unless separately
  approved with their own compatibility design, not bundled into this stage.
- Expected files: `ui/screens/home/HomeScreen.kt`, `HomeViewModel.kt` presentation,
  `ui/screens/item/ItemDetailScreen.kt`, `AddTaskSheet.kt`, `ItemStatusListScreen.kt`,
  `ui/screens/search/SearchScreen.kt`, `ui/components/TaskCard.kt` and drag wrappers
  only for demonstrated interaction safety. Business repositories/DAOs stay unchanged
  for the redesign-only path; new global queries require D03/D05 work approval.
- Components: TaskRow/C06, SectionHeader, filter controls, state components,
  labelled New task, responsive shared AddTaskSheet.
- Preserve: create/edit same-ID/no duplicate, title/notes/dueAt prefill, cancel,
  all status actions, group identity/create/share/delete, search title AND notes,
  pending manual drag order and reminder/clearing/rearming semantics.
- Security/data: no new task types or inferred completedAt. Do not write sorted/
  filtered order back as manual order; independent checkbox/icon/body/drag targets.
- Tests required: add/edit/cancel database assertions, no duplicate save, no status
  mutation on edit tap, child action propagation, long-press/drag release, accessible
  reorder, completed/snoozed/reopen/resume, share/export, date clearing and reminders.
- Visual acceptance: compact reference-like rows, full readable notes/group/date,
  statuses not color-only; all workflows still discoverable; FAB never covers last row.
- Rollback: revert reviewed presentation changes only; stable IDs/positions already
  saved remain valid. Do not reset databases, clear task rows or reset preferences.

## Stage E - Vault interface

- Dependencies: A/B/C navigation; D08 record presets and D09 editor behavior only
  if explicitly approved. D06 health/access metrics and D07e TOTP remain separate
  functional/security projects. D13 governs read-aloud changes.
- Expected files: `ui/screens/vault/VaultScreen.kt`, `VaultUnlockScreen.kt`,
  `VaultEditors.kt`, `VaultComponents.kt`, `VaultLogin.kt`, `VaultMasterSetup.kt`
  display/wiring only; matched EN/FA resources. Reuse VaultRepository/Crypto/
  DeviceUnlock/Models/Store without a schema or encryption change.
- Components: folder/item/secure-field cards, SecureInputField, PasswordGeneratorPanel,
  confirmation/state components, existing category browser/security/recovery forms.
- Preserve: fresh master setup and mandatory recovery confirmation, old-envelope
  preservation on explicit new-vault creation, master/recovery/device setup errors,
  category hierarchy/colors/icons/order, record IDs/custom types/tags/favorites,
  sorting/search, duplicate/delete, all lock/session/deadline/binding behavior.
- Security: Phase 02 external-open whitelist stays exact; keep owned sensitive
  clipboard, no auto-linking secrets, no decrypted saved-state models, no hidden
  display of locked counts. Authentication is the existing OS CryptoObject flow,
  not an animated UI success. Lock supersedes discard prompts and removes drafts.
- Tests required: all existing VaultExternalIntentTest cases, masks before/after
  reveal/copy, secret custom URL/SECRET flags, scope queries and stale callbacks,
  group CRUD/move/cycle/delete preservation, record full-field same-ID edits,
  recovery/key rotation, master change, PIN cooldown, unavailable/cancelled biometrics,
  background/return deadline and process death. Device enforcement tests are mandatory.
- Visual acceptance: 01/04/05/06 clearly match reference hierarchy, no fabricated
  score/TOTP, group grid reflows, title/copy commands readable, secure editor can
  reach all fields and Save with IME and 2x FA text; no secret in screenshots.
- Rollback: reverse only visual/wiring patch after comparison. Existing encrypted
  records remain untouched; never rotate keys, regenerate recovery or delete the
  Vault as a method of reverting UI. Lock before swapping presentation in tests.

## Stage F - Settings and supporting screens

- Dependencies: A/B and reviewed navigation/security placement; D12 preserved
  background policy and approved terminology. No backup-format modification.
- Expected files: `ui/screens/settings/SettingsScreen.kt`, `BackupSection.kt`,
  display portions of `SettingsViewModel.kt` only when needed; Vault security/
  recovery dialogs in existing Vault UI files; `ui/screens/reminder/ReminderPopupScreen.kt`
  style only if included. Existing locale resource files in EN/FA.
- Components: SectionHeader, settings rows, ConfirmationDialog, SecureInputField,
  Loading/Error states; existing category/security controls.
- Preserve: all theme/background/About information, FULL/TASKS/VAULT exports,
  Android file picker, last-backup/share, password separation, v1 and v2 import,
  available-module preview, explicit replacement, no merge, interrupted-restore
  journal, reminder Done/Snooze/Dismiss and notifications/permissions.
- Security: no misleading merge or rollback promise; no secret-bearing shared
  diagnostics. Device unlock remains device-only. Keep all original backup/AAD/
  notification identifiers and signing/update identity.
- Tests required: external document-provider save/open/cancel/share, full/selective/
  legacy restore, invalid/tampered/wrong-password input, rollback/journal replay,
  insufficient space, task-only/vault-only isolation and reminder rearm. Device
  save/reinstall/restore uses external synthetic-data backups, never real accounts.
- Visual acceptance: same typography/surfaces/focus as modules, long Persian
  confirmation text scrolls, explicit replace-selection summary, honest success/error
  and backup-password/master-password labels; no dashboard recovery-key buttons.
- Rollback: keep previous backup/recovery logic and UI callbacks available while
  reverting display patch; never replace user backup files, clear the journal or
  uninstall to roll back presentation.

## Stage G - Cross-screen integration and QA

- Dependencies: approved completed A-F subsets, reference acceptance record and
  resolution/deferral of every D item. No release authorization is implied.
- Expected files: existing `app/src/test` suites plus targeted new Compose/UI
  regression tests and synthetic screenshot fixtures. Proposed UI/theme files may
  receive small verified integration corrections; no broad architecture cleanup.
- Components: integrated root insets/navigation/state wiring; no new framework.
- Preserve: all Phase 02/03 compatibility contracts and all earlier tests, including
  method/field behavior rather than only updated text assertions.
- Security: use a non-debuggable, owner-signed test release for enforcement/update
  checks before production. No real secrets in debug/test artifacts. Independently
  review authenticated UI data flow; do not mistake passing screenshots for a crypto audit.
- Tests: full existing plus approved new suite, lintDebug, assembleDebug, clean CI
  JDK17/build-tools35 parity where available, device test matrix below. Record exact
  commands/results/toolchain and distinguish inherited warnings/new failures.
- Visual acceptance: approve reference/state comparison sheets and accessibility
  results; no lost features, clipped text, overlapping commands or UI-only fake metrics.
- Rollback: keep an approved known-good signed package and source snapshot; test
  in-place update/backout policies with the owner. No automatic downgrade, uninstall,
  data restore, commit/push or release publication. Stop for review at each stage.

## Screenshot and interaction testing strategy

Recommend both deterministic screenshot comparison AND Compose UI/behavior tests.
Screenshots catch spacing, hierarchy, theme/bidi/clipping; behavior tests catch
save/duplicate/gesture/security failures. Neither substitutes for the other.

Use existing Robolectric/Compose infrastructure first; do not introduce a screenshot
framework without need/approval. Freeze clock, locale, theme, font/display scale,
viewport and synthetic dataset. Keep original 00-06 references beside comparison
sheets; compare corresponding content regions, not mock device bezels/status time.
The reference has unsupported features: approved omissions are annotated, not
treated as missing pixels to simulate. New goldens must have explicit review,
not wholesale acceptance after a failing run.

| Test dimension | Required cases in later implementation |
| --- | --- |
| Layout/theme | 320/360/400/600/840/1000 dp, short landscape, split screen, system/app light/dark, cutout, gesture and 3-button bars. |
| Language/text | EN/FA, mixed-script titles/notes, long group paths/tags, long URL/secret, empty values, font 1.0/1.3/1.5/2.0; runtime Vaurivo version. |
| User input | Touch, mouse/focus/keyboard, TalkBack, IME next/done/back, modal dismissal, generator cancel, row-vs-icon/drag interactions. |
| Session | No Vault/existing locked/unlocked/pending recovery, manual/foreground/away lock, background/rotation/process death, stale callbacks, no metadata leakage. |
| Data | Existing schema1/2/3 tasks, Unicode notes, custom sensitive types, many nested groups, large/chunked Vault, same-ID edits and intentional duplicate new ID. |
| Backup | v1 and v2/full/tasks/vault, wrong keys/passwords, tampered/truncated/oversized files, selective non-erasure, failed write/import and committed journal recovery. |
| Device/security | At least API24/25 fallback validation and API31+ physical biometric/Keystore/clipboard/notification checks; enrollment changes/cancel/lockout and FLAG_SECURE behavior. |
| Motion | Normal and animator-scale0; no looping background animation or secret reveal transition; no focus loss after updates. |
| Update identity | Same com.taskpro.app + original installed signing certificate, version policy approved, install over existing synthetic data without uninstalling. |

## Readiness and stop condition

Design documentation is complete for review; product-policy decisions and real
implementation/testing remain. A/B and presentation-only portions of D/E/F can be
authorized independently of new Home metrics, search, models or TOTP. The launcher
spec from Phase 03 remains pending approved logo assets, including existing API24/25
fallback and monochrome gaps. Do not replace it with an arbitrary reference icon.

Next authorized action must come from the user: approve the design/open decisions
and explicitly choose an implementation stage. No automatic Phase 04B, coding,
commit, push, build publication or schema migration follows this document.
