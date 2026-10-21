# Phase 04B-A: Design System Stage A

Completed 2026-10-21. Scope: tokens and existing Compose theme integration only.
Stage B, screen redesigns, new navigation, artwork and new features were not started.
No commit, push, publication or release was performed.

## Preservation and baseline

- Starting branch: `main`; HEAD: `faadf57eb4911089181bde52fe93f2b90a710266`.
- The checkout already contained 23 modified tracked files and 56 untracked files.
  None were staged, discarded, stashed or reset.
- Before editing, preserved all 133 tracked/non-ignored source, asset, test and
  documentation files in a private local recovery directory:
  `phase04b-a-20261020/baseline-7ed5cf88fe90` (private, off-repository).
- The sibling `.zip` has SHA-256
  `951DE5259FD1764DB961624A38C0D685D2CA10F91ABA3A733571BB21AF688F26`.
  Recovery includes file copies, per-file SHA-256/size manifest, Git status/HEAD,
  unstaged diff and recovery instructions. Copies and archive entries were verified
  against their source bytes before implementation.
- Ignored files, Git metadata, databases, runtime information, local SDK settings
  and signing material were excluded. File-type/name and credential-pattern checks
  passed; these checks are not a guarantee that arbitrary source contains no secrets.
  This is a private source recovery snapshot, not a user-data or off-device backup.
- Final baseline comparison: 131 files byte-identical, only the two theme files
  below changed, no starting files missing. Five new files were added in this stage.

## Exact Stage A changes

Paths below are repository-relative. Other existing working-tree changes predate
this stage and are not part of its implementation.

| File | Change and purpose |
| --- | --- |
| `app/src/main/java/com/taskpro/app/ui/theme/VaurivoColors.kt` | New immutable semantic palette, exact approved Dark/Light values and `LocalVaurivoColors`. |
| `app/src/main/java/com/taskpro/app/ui/theme/VaurivoTokens.kt` | New spacing, layout, shape, stroke, elevation and duration/easing constants. No shared components. |
| `app/src/main/java/com/taskpro/app/ui/theme/Theme.kt` | Map semantic colors into Material 3, provide palette and shapes through the existing `TaskProTheme`. Preserve theme selection and status-bar handling. |
| `app/src/main/java/com/taskpro/app/ui/theme/Type.kt` | Explicit approved sizes, line heights, weights and zero tracking using the same bundled Vazirmatn fonts. |
| `app/src/test/java/com/taskpro/app/ui/theme/VaurivoTokensTest.kt` | Six token, contrast, typography, geometry, motion and existing group-color regression tests. |
| `app/src/test/java/com/taskpro/app/ui/theme/ThemeFoundationTest.kt` | Seven native Robolectric rendering/theme/preferences tests using synthetic content. |
| `docs/phase04b-a/REPORT.md` | This implementation and validation report. |

## Token and integration summary

All 20 opaque colors and the scrim in each palette match the approved
[color contract](../phase04a/DESIGN_TOKENS.md#color-contracts), without substituting
a different design direction. Principal Dark values:

| Role | Value |
| --- | --- |
| Canvas | `#0C1110` |
| Surface | `#131B19` |
| Raised surface | `#1B2522` |
| Tasks accent | `#ADB8FF` |
| Vault accent | `#5EDFB2` |
| Primary text | `#E7EEEC` |
| Secondary text | `#A5B3AC` |
| Interactive outline | `#71827A` |
| On-accent text | `#0C1110` |

- Material primary is Tasks, secondary is Vault, tertiary is semantic success.
  Container, text, inverse, outline, error and surface roles are explicitly mapped;
  wallpaper/dynamic colors are not introduced. Surface tint is transparent so
  elevation does not introduce an unrelated color cast.
- Semantic scrims are black at 56% Dark / 32% Light. Material's scrim color stays
  opaque black because its components apply their own opacity. Passing the
  pre-dimmed semantic scrim there would compound that opacity. Future custom
  overlays can consume the semantic scrim directly.
- Material low/normal containers use surface; high/highest/bright use raised
  surface. Consequently default Material filled cards can use raised surface,
  not the ordinary semantic card color. Exact reference card colors will be
  selected by Stage B components rather than editing screens now.
- All 15 typography roles explicitly use bundled Vazirmatn 400/500/700, zero
  letter spacing and scalable `sp`. Sizes/line heights are 28/36 display,
  24/32 screen headings, 20/28 detail headings, 16/24 primary content,
  14/20 section/action labels, 14/22 supporting text and 12/18 captions.
  No font files or secret-value typography/semantics were changed.
- Spacing: 4/8/12/16/20/24/32/40/48 dp. Compact/expanded margins: 16/24 dp.
  Card/control/utility radii: 16/12/8 dp; sheet top corners: 24 dp.
  Touch-target/action-height conventions: 48/56 dp. Border: 1 dp;
  focus/offset: 2/2 dp; card/overlay elevation: 0/4 dp.
- Duration tokens: press 120, fast 160, standard 180, entrance 220, sheet 240,
  pulse 2000 ms. Ease-out, standard and linear easings use existing Compose APIs.
  These are definitions, not new animations. Future consumers must honor system
  animation scale/reduced motion; no new pulse or transition runs in this stage.
- Constants do not forcibly resize every existing control. Some Material shapes
  have component-specific defaults; token-aware shared controls are Stage B work.
  No new framework or dependency was added.

## Compatibility verification

The original `SYSTEM/LIGHT/DARK` resolution remains intact. Tests exercise both
device appearances, explicit overrides and all nine combinations with
`PLAIN/GRADIENT/AURORA`. Rendering never rewrites settings, including the synthetic
last-backup timestamp. Fresh preference defaults remain `SYSTEM` and `GRADIENT`.
All three background render paths remain available and are pixel-tested as distinct.

Byte comparisons confirm no change to repositories, settings keys/defaults/enums,
Room entities/database/migrations, backup formats, encryption/AAD/KDF/Keystore
identifiers, master-password/recovery/session logic, clipboard handling, reminder
identifiers/scheduling, resources, fonts, navigation, manifest, dependencies or
launcher/splash assets. `Color.kt` and `StatusColors.kt` remain untouched: stored
RGB values, deterministic group colors and legacy task-status presentation persist.
The Phase 02 external-intent fix remains unchanged and its six tests pass.

Built APK inspection confirms `com.taskpro.app`, Vaurivo label, version `2.0.4`
(code `9`), min SDK 24 and target SDK 35. No production signing/update installation
or physical-device user-data migration was performed or claimed.

## Executed validation

Local environment: Windows, JDK 21.0.8, Android platform 35 and installed build
tools 36.0.0 selected by the existing ignored `.gradle/local-sdk.init.gradle`.
No toolchain/dependency configuration was edited. CI uses a different installed
toolchain (JDK 17/build tools 35); this report does not assert a CI run.

Baseline, before any implementation: **88 tests passed**, no failures/errors/skips;
lint passed with 0 errors and 60 warnings; debug build passed.

Focused new tests: **13 passed**. The first rendering-fixture run failed six
assertions because Robolectric reported paragraph width larger than the intrinsic
Text node width. Giving the fixture labels explicit available width resolved the
measurement mismatch. No production screen or clipping assertion was removed.

Final full command (test execution explicitly rerun):

```powershell
$env:JAVA_HOME='C:\Program Files\Android\openjdk\jdk-21.0.8'
$env:ANDROID_HOME='C:\Program Files (x86)\Android\android-sdk'
.\gradlew.bat -I .gradle/local-sdk.init.gradle :app:testDebugUnitTest --rerun :app:lintDebug :app:assembleDebug --console=plain
```

Result: **BUILD SUCCESSFUL**, **101 tests passed**, 14 suites, zero failures,
errors or skips. All original 88 tests passed again plus 13 new tests.

| Suite | Passed |
| --- | ---: |
| BackupFormatTest / BackupRepositoryTest | 9 / 5 |
| BrandingCompatibilityTest | 8 |
| ReminderPopupScreenTest | 5 |
| VaurivoTokensTest / ThemeFoundationTest | 6 / 7 |
| VaultCryptoTest / VaultDeviceUnlockTest | 6 / 8 |
| VaultExternalIntentTest / VaultFirstRunTest | 6 / 5 |
| VaultIntegrationTest | 17 |
| VaultScreenTest / VaultUnlockUiTest | 14 / 4 |
| WorkspaceNavigationTest | 1 |

- Exact Dark/Light colors, scrims, typography, dimensions, shapes, duration/easing
  values and stable group colors pass regression checks.
- Independent sRGB contrast calculations check 64 active text pairs at >=4.5:1
  and 18 required outline/focus pairs at >=3:1, including base/raised surfaces,
  selected containers and filled module actions where applicable. Decorative
  borders and disabled colors are not claimed to meet active-control targets.
  This is not certification of every existing alpha blend, status color or screen.
- Native Robolectric 320-dp fixtures render Dark/Light English at scale 1.0,
  Dark/Light Persian RTL at 2.0, Dark Persian at 1.3 and Light English at 1.5.
  Visible label layouts have no reported overflow, primary target height is >=56 dp,
  and sampled pixels match all five principal canvas/surface/accent colors exactly.
  Four fixtures were visually inspected: `dark-en`, `light-en`, `dark-fa-large`,
  `light-fa-large`, under `app/build/reports/theme-foundation/`.
- Existing synthetic chooser Dark and Vault login Light test captures were also
  visually inspected under `app/build/reports/workspace/`. Their legacy layouts
  and local palettes remain; these captures are not proposed replacement screens.
- Lint: **0 errors, 60 warnings**, identical baseline warning signatures
  (ID/severity/message/file, not just total). Counts: GradleDependency 33,
  UnusedResources 13, AndroidGradlePluginVersion 3, ConstantLocale 3,
  MonochromeLauncherIcon 2, ObsoleteSdkInt 2, and one each OldTargetApi,
  PluralsCandidate, UseTomlInstead and VectorRaster.
- Read-only SDK `package.xml` marshalling warnings occurred both before and after.
  `git diff --check` passes; existing LF/CRLF advisories are not new source changes.
- Baseline, focused and final test XML plus baseline/final lint XML are preserved
  privately beside the recovery snapshot under `validation/`.
- Debug artifact: `app/build/outputs/apk/debug/app-debug.apk`; SHA-256
  `C30D64B84EE8E550F8FDFDF2DF51CF4E494C79168532D5ADE74F3F5ED062F621`.
  It was built locally, not installed, uploaded or released.

## Reference fidelity and boundaries

Both exact approved reference PNGs and all eight Phase 04A design documents were
reviewed. Dark tokens and geometry match the approved specification. Light tokens
are its semantic counterpart, not a claim to reproduce a supplied Light screenshot.
The scrim and Material container mappings above are documented integration choices.

The reference flat Dark canvas is available through existing PLAIN selection;
GRADIENT remains the stored default and existing selections are not reset. The
current `WorkspaceBackdrop`, screen-local Vault accent and hardcoded dark unlock
palette were deliberately not rewritten. Consequently Stage A does not make the
current screens pixel-identical to the references, nor fix the existing Dark-only
unlock appearance under Light mode. Those require later authorized screen work.
No design departure or decorative gradient was added to compensate for that gap.

Physical-device/emulator interaction, Motorola biometric behavior, TalkBack output,
hardware-keyboard focus, actual reduced-motion transitions and release-signing
compatibility are **unverified** in this stage. Synthetic native rendering does not
replace those checks. Existing screen-specific sizing and color hardcodes still
need individual review; token contrast alone does not certify their accessibility.

The approved D01 chooser-on-each-genuinely-new-launch decision supersedes the old
Phase 04A first-launch recommendation, but no lifecycle/navigation behavior was
implemented now. Rotation, Recents and foreground return must not become new
launches in a later implementation. D03-D10 future functionality, authenticated
Vault-only search, security behavior and D11 pending Geometric V artwork remain
guidance only, not authorization to start them here.

## Recommended Phase 04B-B scope

After review and explicit approval, implement Stage B shared, token-aware UI
primitives from `COMPONENT_SPEC.md`: bounded icon commands, growing action buttons,
search/field styles, neutral cards/folder tiles, section headers and focus/state
presentation. Validate Dark first, maintain Light and EN/FA parity, test long labels,
48/56-dp targets and font scaling, and honor reduced motion. Secret fields must
retain existing masking, explicit reveal/copy and no automatic secret announcements.
Do not combine that scope with navigation, screen redesign, new feature work or
launcher artwork. Stage A stops here for user review.
