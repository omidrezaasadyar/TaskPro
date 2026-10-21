# Vaurivo Phase 03: public brand migration

Completed scope: display-only branding and regression verification, 2026-10-20.
No redesign, schema migration, architectural modularization, version bump, commit,
push, repository rename, or release publication was performed.

## A. Implementation summary and preservation

The existing application now presents Vaurivo, with Tasks and Vault as its English
module names. Persian uses Vaurivo unchanged, with کارها and گاوصندوق for modules.
User data, encryption, backup formats and existing behavior were not migrated.

Before editing, all four Phase 02 source-of-truth documents were read, Git state
was rechecked and the Phase 02 URL-action security guard was confirmed present.
The branch remains `main`, HEAD `faadf57eb4911089181bde52fe93f2b90a710266`, remote
`https://github.com/omidrezaasadyar/TaskPro.git`. Origin synchronization was not
re-queried over the network. No Git index/history mutation was performed.

The starting tree had 23 modified tracked files and 43 untracked files. It includes
valuable uncommitted Vault, backup and test implementations. A new private snapshot
preserved all 120 tracked/non-ignored project files, Git status, HEAD, SHA-256/size
manifest, staged/unstaged patches and recovery instructions before source edits.
Copied files and ZIP entries were independently hash-verified.

Private snapshot directory, outside the repository:

```text
Private off-repository recovery snapshot (phase03-20261020/baseline-28f9cbeb1419).
```

Sibling ZIP SHA-256:
`855B98BEA2163568D13B5E1326814B7C4DE68ED9802603302ACAD6514F2444B0`.
This does not overwrite the Phase 02 baseline. Git internals, ignored caches,
runtime databases, local.properties, signing keys and sensitive local state were
excluded. A filename/type and high-confidence credential scan found no blockers;
that is not a guarantee that arbitrary source contains no secrets. Nothing was
uploaded. This is a local source snapshot, not an off-device or user-data backup.
Recover into a new empty directory and verify hashes; never extract over this
dirty checkout or blindly apply patches.

## B. Exact changed files

Relative to the fresh Phase 03 snapshot, 17 existing files changed and three new
files were added. The other 103 original files are byte-identical. The full Git
diff also includes earlier work; it must not be mistaken for this phase's changes.
Paths below are repository-relative.

| File | Phase 03 change |
| --- | --- |
| `.github/workflows/build-apk.yml` | Three release display-name/body replacements only. |
| `app/src/main/res/values/strings.xml` | English app/About/backup branding and localized share-label resource. |
| `app/src/main/res/values-fa/strings.xml` | Matching Persian app/About/backup/share presentation; Latin product name. |
| `app/src/main/res/values/vault_strings.xml` | Tasks/Vault module and scope labels; clipboard feedback branding. |
| `app/src/main/res/values-fa/vault_strings.xml` | کارها / گاوصندوق labels and localized clipboard feedback. |
| `app/src/main/res/values/recovery_strings.xml` | Recovery save instruction and runtime-version label. |
| `app/src/main/res/values-fa/recovery_strings.xml` | Matching recovery and version branding. |
| `app/src/main/res/values/unlock_strings.xml` | Tasks/Vault terminology in existing setup/error instructions. |
| `app/src/main/res/values-fa/unlock_strings.xml` | Matching Persian module terminology. |
| `app/src/main/java/com/taskpro/app/ui/screens/settings/BackupSection.kt` | Vaurivo suggested export filename and localized share ClipData label. |
| `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultMasterSetup.kt` | Vaurivo suggested previous-vault filename only. |
| `app/src/main/java/com/taskpro/app/vault/VaultClipboard.kt` | Display label from app_name; ownership, sensitive flag, cleanup and content checks unchanged. |
| `app/src/test/java/com/taskpro/app/vault/VaultScreenTest.kt` | New display expectations and explicit visible runtime-version assertion in compact Persian large-text login. |
| `app/src/test/java/com/taskpro/app/vault/WorkspaceNavigationTest.kt` | Tasks/Vault selector expectations only; existing task/session assertions retained. |
| `README.md` | Public product/module names, Persian explanation and compatibility/report links. |
| `BACKUP.md` | Product presentation; original legacy-format documentation retained. |
| `SECURE_VAULT.md` | Product/module presentation, filename explanation and unchanged legacy-import contracts. |
| `app/src/test/java/com/taskpro/app/branding/BrandingCompatibilityTest.kt` (new) | Eight focused branding, localization, clipboard and compatibility tests. |
| `docs/phase03/LAUNCHER_ASSETS.md` (new) | Existing launcher/splash inventory and actionable pending-approval specification. |
| `docs/phase03/REPORT.md` (new) | This implementation and validation report. |

## C. Branding verification

- Launcher, Android application display label and workspace heading resolve through
  app_name to Vaurivo. Home/Tasks headers using that resource inherit the new label.
- Workspace cards, bottom navigation and restore selectors use Tasks / Vault.
- Settings/About describes both existing modules; author and company are unchanged.
- Recovery save instructions and the authentication footer show Vaurivo; the footer
  retains the runtime BuildConfig version, currently `Vaurivo | v2.0.4`.
- Backup errors, version display, suggested filenames, share ClipData label,
  credential clipboard label and copy feedback use the new public name.
- Notification source contains no old product display literal. Android's app label
  changes through the manifest resource; existing generic task reminder text remains.
  Actual device notification presentation is not claimed as tested.
- Public README, backup and Vault usage documentation, and future CI release titles
  are updated. Artifact name `taskpro-debug-apk`, tags, APK path and remote stay intact.
  No remote release was changed or published.

Remaining TaskPro references are intentional internal/compatibility identifiers,
legacy-format examples, tests that explicitly assert those contracts, or historical
audit/compatibility explanations. The Phase 02 documents and occurrence inventory
are byte-identical historical records, not rewritten as though they audited Vaurivo.

## D. Protected identifiers and evidence

SHA-256 comparison against the fresh 120-file snapshot verifies these source files
are byte-identical; full-suite tests add behavioral evidence. Only the three
production Kotlin display files listed above changed.

| Contract retained | Comparison / test evidence |
| --- | --- |
| applicationId/namespace `com.taskpro.app`; version 2.0.4/code 9 | `app/build.gradle.kts` unchanged; compiled APK badging confirms ID/version/SDK values. Branding tests assert ID and R namespace. |
| Packages, `TaskProApp`, `TaskProTheme`, `Theme.TaskPro`, Gradle root name | Package declarations, internal symbols, manifest, themes and Gradle settings retained. No global rename. |
| Room `taskpro.db`, schema 3 and migration chain | AppDatabase, entities, DAOs and repositories unchanged; existing 1->3 migration and restore tests pass. |
| DataStore `settings`, `theme_mode`, `background_style`, `last_backup_at` and enums | SettingsRepository unchanged; persisted enum and legacy-backup preference tests pass. |
| AES/GCM, Argon2id profile, salts/nonces, AAD and purposes | VaultCrypto unchanged. New test decrypts backup data using literal `TaskPro:backup:data:1:argon2id:19456:2:1`; existing crypto/recovery tests pass. |
| Master/recovery/PIN/device formats and Keystore aliases | VaultCrypto, VaultDeviceUnlock, VaultModels, VaultStore and VaultRepository unchanged; eight device-wrapper tests and existing lifecycle/recovery tests pass. |
| Clipboard `taskpro.clip` and sensitive flag | Display-only diff reviewed; new ownership/cleanup test and existing Vault tests pass. |
| `com.taskpro.backup` v1 / `com.taskpro.workspace` v2 | All three backup implementation files unchanged; new independently constructed legacy input and literal encrypted-format checks pass; full/selective/rollback tests pass. |
| Notification/action/extras and task-derived IDs | Entire notification directory unchanged; literal channel/action/extra assertions and reminder restoration tests pass. |
| Navigation routes and stable record IDs | NavGraph/Screen unchanged; route literals asserted; existing module-switch and legacy ID preservation tests pass. |
| Signing, dependencies and distribution IDs | Gradle/signing configuration unchanged. Workflow diff changes only display text, not artifact ID, tags, secrets, commands or paths. Installed/production certificate continuity is NOT verified. |
| Phase 02 external-intent fix | VaultScreen.kt and VaultExternalIntentTest.kt byte-identical; all six security regression tests pass. |

No existing user database, credential file or backup was opened, overwritten,
renamed or re-encrypted. Compatibility evidence uses source comparisons and
synthetic test data, not a claim of a physical installed-app upgrade.

## E. English and Persian QA

Both locales contain all 247 string keys, including the single new share-label key.
No translation gaps or duplicate keys were found. Tests compare format argument
indexes/types across locales; existing placeholders were retained. Resource scanning
rejects retired public product/module names and incorrect Vaurivo capitalization.
Manifest supportsRtl and locale/fallback handling were not changed.
An additional structured XML comparison verified all 492 original localized string
entries still exist with their original format-argument sequences.

English launcher/module/share/version labels and Persian launcher/module/share/
version labels passed runtime resource tests. Persian product spelling stays Latin.
Natural Persian sentences retain the no-data-change, key-loss and clipboard
best-effort warnings; recovery keys, URLs and credentials retain existing direction
handling. No invisible directional controls were inserted into data or resources.

Existing Compose tests passed for light/dark workspaces, compact Persian login and
dashboard with 1.5x text, and RTL tablet rendering. The Persian login test additionally
scrolls to and asserts the full runtime-version footer is visible. Generated
Robolectric previews for light/dark workspace, English login, and compact Persian
large-text login were visually inspected: brand/module/version text is legible and
no new overlap or clipping was observed in those views. These are synthetic JVM
previews, not a physical-device, TalkBack, all-screen or all-font-size certification.
Source scan found no unintended retired display literals in production strings;
the only new hardcoded product text in Kotlin is the two deliberate filename
suggestions. Clipboard/share display labels use localized resources.

## F. Launcher identity status

All existing launcher assets, colors, theme definitions and illustrations are
unchanged. The current white tick and purple gradient remain deliberately until
the Vaurivo logo and palette are approved. No arbitrary logo or new asset dependency
was introduced. [Launcher asset specification](LAUNCHER_ASSETS.md) lists exact files,
adaptive layers, future monochrome/legacy outputs and acceptance checks, with
official Android guidance linked at the relevant requirements.

Pre-existing limitations: no monochrome layer (two existing lint warnings), and
only v26-qualified launcher icons despite minSdk 24. API 24/25 fallback resolution
needs an approved asset and device validation, not a silent unrelated fix here.
No custom splash icon/background configuration was found; platform behavior is
retained. No launcher cache, actual splash or OEM screenshot claim is made.

## G. Executed validation

Commands used JDK 21.0.8, compile SDK 35 and installed build-tools 36.0.0 through the
pre-existing ignored `.gradle/local-sdk.init.gradle`. CI remains JDK 17/build-tools
35.0.0; exact CI toolchain parity was not executed locally.

```powershell
.\gradlew.bat -I .gradle/local-sdk.init.gradle :app:testDebugUnitTest --rerun :app:lintDebug :app:assembleDebug --console=plain
.\gradlew.bat -I .gradle/local-sdk.init.gradle :app:testDebugUnitTest --tests com.taskpro.app.branding.BrandingCompatibilityTest --tests com.taskpro.app.vault.WorkspaceNavigationTest --tests com.taskpro.app.vault.VaultScreenTest --tests com.taskpro.app.vault.VaultExternalIntentTest --console=plain
```

| Check | Actual result |
| --- | --- |
| Fresh pre-change full suite | 80 tests, 11 suites, zero failures/errors; matches Phase 02. Test task explicitly rerun. |
| Pre-change lint/debug | Successful; 0 lint errors, 60 warnings. Unchanged lint/build inputs reused incrementally. |
| Post-change focused suite | 29 tests, four suites, zero failures/errors: 8 branding, 14 Vault UI, 1 navigation, 6 security. |
| Post-change full suite | 88 tests, 12 suites, zero failures/errors; all original 80 test names retained plus eight new ones. Test task explicitly rerun. |
| Post-change lint/debug | Both successful. Lint 0 errors / 60 warnings. Warning ID/severity/message/file signatures exactly match baseline, ignoring line shifts. |
| Compiled APK inspection | aapt reports EN/FA application label Vaurivo, package com.taskpro.app, versionName 2.0.4, versionCode 9, minSdk 24, targetSdk 35. |
| Source/diff review | 17 intended baseline file changes, 103 byte-identical; three intentional new files. Full Phase 03 diff reviewed, including previously untracked files. git diff --check passes. |

Final Git state: 23 modified tracked files, 46 untracked files, no staged files;
123 tracked/non-ignored project files in total. HEAD, branch and origin URL still
match the pre-change snapshot. These counts include earlier uncommitted work.

The eight new tests cover EN labels, FA labels/RTL configuration, all display
resources, translation/format parity, clipboard ownership and sensitivity, an
independent original-format legacy backup with IDs/notes/preferences, encrypted
format/KDF/literal AAD, and application/navigation/reminder/enum contracts.

Existing executed tests also cover Tasks preservation, migration, reminders,
Vault setup, encryption, master changes, recovery, device wrappers, locking,
category/record UI, clipboard, full/task-only/vault-only backups, invalid input,
wrong passwords, rollback and interrupted-restore journal replay. No new phone or
security-audit coverage is implied by those existing JVM tests.

Pre-existing lint warnings remain: GradleDependency 33, UnusedResources 13,
AndroidGradlePluginVersion 3, ConstantLocale 3, MonochromeLauncherIcon 2,
ObsoleteSdkInt 2, and one each of OldTargetApi, PluralsCandidate, UseTomlInstead,
VectorRaster. SDK package.xml read-only warnings and the existing Undo icon
deprecation surfaced again; no unrelated suppression or dependency upgrade was made.
Initial sandbox execution could not open the external Gradle cache lock; the
approved elevated rerun succeeded. This was an environment permission failure,
not a test or application failure.

Private before/focused/after XML validation copies and a per-file before/after
SHA-256 comparison CSV are beside the preservation snapshot under
`phase03-20261020/validation`; generated build reports remain under
`app/build/reports`. No secret-bearing diagnostic bundle was published.

## H. Remaining risks and limits

- This checkout still contains the substantial valuable uncommitted feature work.
  No clean remote-checkout/CI deployment is implied. Commit/publication requires
  explicit authorization and review of the entire intended working tree.
- No physical device/emulator install, same-certificate upgrade, actual Keystore/
  biometric enforcement, notification delivery, document-provider/share-sheet,
  TalkBack or uninstall/reinstall testing was performed during Phase 03.
- Signing configuration is preserved, but original installed certificate/key
  ownership and local/CI/production update continuity remain unresolved. Do not
  uninstall to bypass a signature mismatch. Do not use debug builds for real secrets.
- The version stays 2.0.4/code 9 as requested scope implies; a future published
  upgrade needs a deliberate release/version/signing decision.
- Final logo/palette, monochrome/fallback assets, splash/OEM checks and exact CI
  toolchain verification remain release gates. Existing lint warnings remain open.
- JVM rendering and passing tests are not an independent cryptographic audit or
  proof against compromised devices. Existing threat-model limitations still apply.

## I. Phase 04 readiness

Ready for review and planning of the Vaurivo Design System, not certified for
production distribution. Obtain logo/palette approval, define semantic light/dark
and module tokens, typography, spacing, focus/accessibility and reduced-motion
rules, then inventory reusable existing Compose components. Require English/Persian
large-text screenshots and signed-device regression gates before adopting new
visual tokens. Keep all Phase 02 compatibility contracts frozen unless a separately
approved, tested migration is introduced.

No Phase 04 design-system implementation, UI redesign, architecture refactor or
automatic next phase has begun. Stop here for review.
