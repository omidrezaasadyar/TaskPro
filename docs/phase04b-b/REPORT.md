# Phase 04B-B: Shared UI Components

Completed 2026-10-21. Stage B only: reusable Compose presentation components,
debug-only specimens, regression tests and visual evidence. **No existing screen
was redesigned or wired to these components.** Installing the debug APK does not
expose a new component-gallery page. Phase 05 has not started. No commit, push,
publication, branch/history change or release was performed.

## Preservation and verified baseline

- Branch `main`, HEAD `faadf57eb4911089181bde52fe93f2b90a710266` unchanged.
  Starting work included 25 modified tracked files and 61 untracked files.
  Nothing was staged, stashed, discarded, reset or overwritten.
- Before editing, copied all 138 tracked/non-ignored project files and recorded
  Git status/HEAD, patches, lengths and SHA-256 values. Source copies and every
  archived source entry were verified against their starting bytes.
- Private recovery directory:
  `phase04b-b-20261021/baseline-473ec1d686c7` (private, off-repository).
  Sibling ZIP SHA-256:
  `EA23C355421C4BEF84E55C42CA93E84B378368704D0843AD7F5221B8FBE38837`.
- Excluded ignored files, runtime databases, Git metadata, caches, local SDK
  settings and signing material. Filename/type and high-confidence credential
  checks passed; they are not a guarantee that arbitrary source has no secrets.
  The snapshot stays local/private and is not an application-data backup.
- Independently reran the baseline before implementation: **101 debug tests
  passed**, zero failures/errors/skips, debug build succeeded and lint succeeded
  with 0 errors / 60 warnings. Some unchanged build/lint inputs were incrementally
  reused by Gradle; test execution was explicitly forced with `--rerun`.
- Final SHA-256 comparison: **all 138 starting files remain byte-identical**;
  no missing files. The 29 new files below are the entire Stage B footprint.

## Components implemented

Twenty-one public production composables, grouped into six files rather than a
separate abstraction/file for every element:

| Group | Exact components | Behavior / boundary |
| --- | --- | --- |
| General | `SectionHeader`, `SearchField`, `FilterChip`, `PrimaryActionButton`, `EmptyState`, `ErrorState`, `LoadingState`, `ConfirmationDialog` | Controlled query/clear, optional scoped actions, checkbox semantics, stable selected/busy geometry, real retry/confirm/cancel callbacks and nullable truthful progress. |
| Workspace | `WorkspaceCard`, `BottomNavigationBar`, `ModuleAccentTile` | Module accents, destination/selected/locked presentation, three labeled tabs; callbacks only, no routes or authentication. |
| Tasks | `TaskRow`, `TaskStatusPresentation` | Independent edit, completion, snooze, status, menu and move-up/down callbacks; optional owner-supplied drag handle; drag/busy editing suppression. |
| Vault | `VaultGroupCard`, `VaultItemCard`, `SecureFieldCard`, `SecureInputField`, `SecurityIndicator`, `PasswordGeneratorPanel` | Folder direct/subgroup counts, independent menus/favorites, safe credential controls, typed custom fields and separate candidate generation/application. |
| Shared utilities | `IconAction`, `GroupIcon` | 48-dp commands/tooltips and contrast-adjusted custom category tiles. Existing pure category-icon mapping is reused without moving or modifying it. |

`ModuleAccent` and `WorkspaceTab` are UI-only enums, never persisted.
`GeneratorOptions` and `GeneratedPasswordCandidate` are transient presentation
objects, not database/backup entities. Candidate `toString()` is redacted.

### Token usage and responsive behavior

- Uses the unchanged Stage A palettes through `LocalVaurivoColors`: flat canvas/
  surfaces, lavender Tasks, mint Vault, on-accent foregrounds, semantic text/error/
  warning colors and subtle versus interactive borders. No gradient or theme reset.
- Repeated frames use 16-dp corners, fields/actions 12 dp, icon utilities 8 dp.
  Uses shared 16-dp padding, 8/12/16/24-dp gaps, 1-dp borders, zero card elevation,
  48-dp targets, 56-dp primary actions and 80-dp navigation content minimum.
- Standard Material interaction sources/ripples provide press/focus/hover feedback.
  Shared focus borders use semantic Tasks/Vault focus colors. Selected and busy
  icons reserve space so labels do not shift; controls grow instead of shrinking
  text. No custom or looping animation/library was introduced. Loading is static.
- Typography inherits the unchanged Vazirmatn roles/zero tracking. Deliberately
  revealed passwords use platform monospace. Technical fields use LTR runs;
  prose uses content direction, without adding formatting characters to values.
- Field commands sit beside values when space/type size permits, otherwise below.
  Chips/toggles wrap, folder composition is owner-controlled, confirmations scroll
  with stacked commands. A safe Cancel action precedes confirmation. Destructive
  confirmation is outlined/error-colored, not an ordinary primary save action.
- Existing SYSTEM/LIGHT/DARK and PLAIN/GRADIENT/AURORA implementations/defaults
  remain byte-identical. Flat debug specimens do not override app preferences.

### Security and integration contracts

- Secure fields require explicit owner-supplied `active` and opaque `sessionKey`.
  Lock/inactive state removes content, not a blurred copy. Session/context changes
  reset reveal/input state. The display binding invalidates retained UI callbacks
  on session change or disposal; it is **not** authentication or repository access
  control. An owner must still authorize/generation-check every operation.
- Owners must key the protected subtree by Vault generation and record/editing
  context, clear drafts/candidates/search on lock and reject stale asynchronous
  results. Continue the existing secure-window and `vaultActivity` lifecycle/touch
  boundary. The reusable controls do not own, unlock, extend or persist a session.
- `sensitive || FieldType.SECRET` is always secret, even when the flag is false.
  Detail masks are fixed-length. Secret input semantics expose a fixed mask,
  password state and explicit editing/focus actions, never raw text/layout results.
  Revealing visually still does not publish the secret through merged/unmerged
  text, editable-text or content-description semantics. No read-aloud feature added.
- Secret editing retains ephemeral cursor/selection state, replacement/insertion
  actions, read-only/disabled/busy modes, no autocorrect and password keyboard types.
  The optional keyboard override supports phone and numeric-password/PIN layouts;
  an ordinary keyboard override cannot downgrade a secret to plain email/text.
  There is no trimming, digit conversion or new PIN/password validation policy.
- `SecureFieldCard` calls Open only for URL type, not secret, and
  `VaultDocument.validUrl(value)`. Reveal cannot change classification. No new
  intent launcher, auto-linking or URL parser was added. The original Phase 02
  implementation and six production-screen intent regression tests are unchanged.
- Copy delegates the exact original value to the owner, which must use existing
  `VaultClipboard` and localized non-secret feedback. Controls never bypass owned
  clipboard cleanup or promise that clearing always succeeds.
- Generator options preserve default 24, range 12-128 and character classes.
  Generation is a callback, not crypto inside the component. A candidate must match
  current options, length and session to be shown/applied. Generate/options/cancel
  never update the form password; only explicit Use Password does. The debug-only
  specimen owner reuses existing `VaultCrypto.generate`, unchanged.
- `SecurityIndicator` presents supplied local 0-4 guidance only, not a vault-wide
  health/breach score or a new strength estimator. Caller text/errors/action labels
  must be localized and non-secret; never pass exception dumps or values as labels.
- No secret-bearing navigation, saveable state, repositories, persistence, logging,
  analytics or network operations were introduced. Managed strings/framework input
  buffers cannot be guaranteed physically zeroized; disposal is not a memory-forensics
  guarantee. Real TalkBack/IME and non-debuggable device enforcement remain gates.

## Exact file inventory

All paths are repository-relative. **No pre-existing file was modified.**

| New file | Purpose |
| --- | --- |
| `app/src/main/java/com/taskpro/app/ui/components/shared/ComponentFoundation.kt` | Shared interaction/focus/card/icon/module styling and transient display guard. |
| `app/src/main/java/com/taskpro/app/ui/components/shared/GeneralComponents.kt` | Eight general components. |
| `app/src/main/java/com/taskpro/app/ui/components/shared/WorkspaceComponents.kt` | Workspace selection and navigation presentation. |
| `app/src/main/java/com/taskpro/app/ui/components/shared/TaskComponents.kt` | Task row/status presentation and distinct action contracts. |
| `app/src/main/java/com/taskpro/app/ui/components/shared/VaultCards.kt` | Folder/item/secure-field cards, category tile and local security indicator. |
| `app/src/main/java/com/taskpro/app/ui/components/shared/SecureFormComponents.kt` | Typed private input, keyboard policy and controlled generator presentation. |
| `app/src/main/res/values/component_strings.xml` | English public labels and count plurals. |
| `app/src/main/res/values-fa/component_strings.xml` | Matching Persian labels/plurals. |
| `app/src/debug/java/com/taskpro/app/ui/components/shared/SharedComponentPreviews.kt` | Stateful synthetic specimens and seven Studio previews; excluded from release. |
| `app/src/test/java/com/taskpro/app/ui/components/shared/SharedComponentTest.kt` | Ten general/action/focus/large-dialog regressions. |
| `app/src/test/java/com/taskpro/app/ui/components/shared/SecureComponentTest.kt` | Eleven classification, semantics, callback, clipboard, keyboard and generator regressions. |
| `app/src/testDebug/java/com/taskpro/app/ui/components/shared/ComponentVisualTest.kt` | Eleven native render/target/text/color checks. Debug source set avoids release tests depending on debug-only fixtures. |
| `docs/phase04b-b/REPORT.md` | This implementation/validation report. |
| `docs/phase04b-b/VISUAL_REVIEW.md` | Matching reference regions, viewed captures and justified deviations. |

The other fifteen new files are under `docs/phase04b-b/evidence/`, copied unchanged
from native test output and verified using SHA-256:

```text
dark-folder-pair.png
dark-generator-candidate.png
dark-generator.png
dark-states.png
dark-vault.png
dark-workspace.png
en-workspace-large-scrolled.png
en-workspace-large.png
fa-generator-large-scrolled.png
fa-generator-large.png
fa-vault-large-scrolled.png
fa-vault-large.png
light-fa-vault-large-scrolled.png
light-fa-vault-large.png
light-vault.png
```

## Executed verification

Windows / JDK 21.0.8, installed Android platform 35 and build tools 36.0.0 through
the existing ignored `.gradle/local-sdk.init.gradle`. No dependency/toolchain file
was changed. CI JDK 17/build-tools 35 parity was not run remotely.

Focused component runs passed before the full suite. Final command:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\openjdk\jdk-21.0.8'
$env:ANDROID_HOME='C:\Program Files (x86)\Android\android-sdk'
.\gradlew.bat -I .gradle/local-sdk.init.gradle :app:testDebugUnitTest --rerun :app:lintDebug :app:assembleDebug :app:compileReleaseUnitTestKotlin --console=plain
```

| Check | Actual result and method |
| --- | --- |
| Full debug unit suite | **133 passed, 17 suites, 0 failures/errors/skips**, from Gradle completion and JUnit XML. Includes all 101 baseline tests plus 32 new component tests. |
| Shared interaction tests | **10 passed**: primary busy/disabled stability, search/clear, checked/disabled chip and fixed bounds, task child-action separation/drag gating, folder/item child actions, tab callbacks, focus/Enter, dialog busy/cancel/confirm, state actions, long Persian scrollable dialog at 200%. |
| Secure interaction tests | **11 passed**: hidden/revealed semantics redaction, accessible text replacement/insertion, session reset, all seven custom types/sensitivity, URL whitelist matrix, exact owned sensitive clipboard delegation, explicit candidate apply/cancel, stale session/options, length/busy guards, disposed callbacks, unnormalized multiline values and keyboard compatibility. |
| Native visual tests | **11 passed**, fifteen PNGs generated. SDK 28 native graphics; 360/400 dp normal, 280-dp English at 200%, 320-dp Persian at 200%, Dark first plus Light English/Persian. Measures enabled click target sizes and tested command text layout, not just screenshots. Explicit resolved-color checks cover state text outside a Surface. |
| Token/preferences | Existing Stage A token/contrast and all theme/background compatibility tests passed unchanged in the full suite; new components consume those same tokens. |
| Existing app regression | All backup, branding/identifiers, reminders, crypto, device-unlock, first-run/recovery, session/integration, Vault-screen and navigation tests passed unchanged. This does not claim every physical workflow was exercised. |
| Lint | **0 errors / 60 warnings**. Baseline/final XML compared by ID/severity/message/file: no warning-signature changes. |
| Debug build | **Succeeded**; local APK only. |
| Release test compilation | **Succeeded** for `compileReleaseUnitTestKotlin`. No release test execution, signed release APK or publication claimed. |
| Scope/data safety | SHA-256 comparison of all 138 starting files, Git HEAD/index checks, source review and whitespace/conflict checks. No changes to existing production paths. |

Inherited lint counts: GradleDependency 33, UnusedResources 13,
AndroidGradlePluginVersion 3, ConstantLocale 3, MonochromeLauncherIcon 2,
ObsoleteSdkInt 2, and one each OldTargetApi, PluralsCandidate, UseTomlInstead,
VectorRaster. Read-only SDK package.xml marshalling warnings and the existing
Undo-icon deprecation also remain. Existing LF/CRLF advisories are not new edits.

Development checks initially caught undersized inner checkbox targets, missing
secure input accessibility actions and reveal-state reuse across type changes;
these were corrected and covered by the passing tests. Screenshot review caught
inherited black state text and added a color regression. Test-only fixes included
explicit test API imports/opt-in, using the actual Cancel translation and correctly
distinguishing a disposed callback from Material's current-node click delegate.
No existing assertions or production security logic were weakened to obtain a pass.

Built APK inspection (`aapt dump badging`) confirms `com.taskpro.app`, label
Vaurivo, version `2.0.4` / code `9`, min SDK 24 / target 35. Artifact:
`app/build/outputs/apk/debug/app-debug.apk`. Baseline/final JUnit/lint XML and
preservation manifests are kept privately beside the recovery snapshot.

## Fidelity, accessibility and limits

Both original reference images, all eight Phase 04A documents and the actual
Stage A code/report were inspected. [Visual review](VISUAL_REVIEW.md) identifies
matching reference regions, actual native renders, review findings and justified
deviations. This is component-level evidence, not a pixel-identical new Home/Vault
screen, a certified accessibility result or a security audit of the whole app.

Geometry grows where required for 48-dp independent commands, direct/subgroup
count labels and 200% text. Focus rings are inset rather than offset outside
frames to avoid clipping/neighbor overlap. Secure input reveal is a sibling control
so raw editable semantics can be cleared without hiding the reveal action.
Localized generator controls wrap; unsupported health/TOTP/recurrence/link regions
are omitted, not simulated. There are no new decorative gradients or illustrations.

Automated semantics, keyboard Enter/focus, target sizing and representative native
EN/FA layout checks passed. Physical TalkBack traversal/secret-entry usability,
actual IME/phone/PIN keyboards and glyph fallback, animator-scale-zero behavior,
long-press drag/release, secure-window screenshot enforcement, biometric devices,
production signing/update install and comprehensive tablet/landscape combinations
remain **unverified**. Standard Material feedback is used; no custom motion loop
needs to be disabled. Release integration needs device accessibility/security QA.

## Deferred work and Phase 05 readiness

- Optional `ProgressSummaryCard` remains deferred: Stage B adds no task aggregation
  or fabricated production fraction. Phase 05 must supply/test the approved real
  scheduled-today read model before introducing it.
- Home/chooser/Tasks/Vault/Settings redesign, real navigation and lifecycle changes,
  category-browser/menu composition and wiring these components are later work.
  Existing implementations and all fields/flows remain intact. Owners still handle
  persistence, field validation, clipboard feedback, session touch and async work.
- D01 genuine-new-launch behavior, D03 metrics, Tasks-only Home search, Timeline/
  Groups, Overview/Favorites, dirty-form confirmation and editor Use Password
  integration are not activated by these APIs. No new preference/schema is needed
  or introduced by Stage B. Permanent logo artwork and deferred features remain out.
- **Ready for Stage B review and subsequent Phase 05 Home/Workspace integration**:
  reuse these controlled primitives with existing callbacks, preserve route identity,
  restore readiness gates, background preferences and Vault lock boundaries; add
  lifecycle/navigation/real-metric tests in that separately authorized stage.
  Do not duplicate bottom system insets already handled by the navigation component.

Stop here for user review. No Phase 05 implementation, commit, push or release.
