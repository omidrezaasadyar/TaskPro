# Vaurivo Phase 02: baseline preservation and Vault external intents

Scope: protect the existing TaskPro working tree; fix the approved external-intent
issue; document compatibility and a future branding map. No Phase 03 work is
authorized or performed. Date: 2026-10-20.

## Preservation

- Original branch: `main`; HEAD: `faadf57eb4911089181bde52fe93f2b90a710266`.
- Original state: 23 modified tracked files and 38 untracked files. All 115
  tracked/non-ignored project files were copied byte-for-byte outside the checkout.
- The local baseline includes SHA-256/size manifest, original Git status and HEAD,
  staged/unstaged binary patches, and recovery instructions. Both directory copies
  and ZIP entries were hash-verified against the original files before source edits.
- Archive SHA-256: `1602A439E1EAA95E94AE7D0FCB5937300820811EED5F3932D35989D406299A53`.
- No commit, stash, reset, discard, branch rewrite, push or migration was performed.
- Git metadata/history, ignored files, caches, runtime databases, local.properties,
  signing keys and sensitive local state were excluded. No blocked filenames or
  high-confidence credential signatures were found in the selected source files;
  this limited check is not a guarantee that arbitrary source contains no secrets.
- The baseline is local, not uploaded or added to the repository. It is not an
  off-device disaster-recovery copy or a user-data backup. Signing-key recovery is
  separate and remains the owner's responsibility.
- Recover into a new empty directory, verify hashes, inspect and compare before
  restoring individual files. Never extract over a dirty checkout or apply patches
  blindly. Preserve any newer work before restoring anything.

## Baseline configuration and pre-existing issues

The actual project uses Kotlin 2.1.0, AGP 8.7.3, Gradle 8.11.1, Compose BOM
2024.12.01, SDK 35 and Java/Kotlin target 17. CI uses JDK 17/build-tools 35.0.0.
This workstation has JDK 21.0.8/build-tools 36.0.0 and uses the pre-existing ignored
`.gradle/local-sdk.init.gradle` override. No toolchain/dependency/CI file was edited.

The working workflow invokes tests, lint and assembleDebug on main, claude/** and
manual dispatch. Committed HEAD only invokes assembleDebug. The feature/test code
and workflow changes must undergo an explicitly approved commit/publication process
before a clean remote checkout or GitHub build can include them. The current local
preservation baseline does not assert that those changes are committed or deployed.

Pre-change tests were explicitly rerun: 74 passed, zero failures/errors. Lint
completed with 60 existing warnings and zero errors (incremental analysis reused
the unchanged inputs); the debug build succeeded. SDK package.xml marshalling
warnings report the read-only SDK installation. These are pre-existing environment
messages, not failures introduced by the security change.

## Security behavior

Before the fix, RecordDetails offered ACTION_VIEW for any field whose value parsed
as an HTTP(S) URL, including a masked password, sensitive custom URL, secret-typed
custom field, or ordinary text/note field. Four regression cases failed against
that original code; the two permitted URL cases passed.

The fix carries URL-field metadata in a function-local detail-display value and
requires all of: an explicitly permitted URL field, non-sensitive classification,
and the existing validUrl check. Only the standard website field and custom fields
typed URL with secret=false expose the open action. Revealing a secret never
changes its classification. No model, schema, cryptography, clipboard logic,
masking logic, locking logic, resources, navigation or visible branding was changed.

## Intended file scope

| File | Purpose |
| --- | --- |
| `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultScreen.kt` | Minimal detail-UI URL-action whitelist; the only existing source file intentionally edited. |
| `app/src/test/java/com/taskpro/app/vault/VaultExternalIntentTest.kt` | Six new synthetic-data regression tests for allowed URLs, protected fields, intents, masking, copying and locking. |
| `docs/phase02/COMPATIBILITY.md` | Exact compatibility identifiers and proposed migration/release gates. |
| `docs/phase02/BRANDING_MAP.md` | File-by-file public/internal/compatibility classification and English/Persian migration plan; no rebrand applied. |
| `docs/phase02/brand-reference-inventory.csv` | Per-occurrence file/line/column/token classification, without actual credential values. |
| `docs/phase02/REPORT.md` | Preservation, execution evidence, unresolved risks and Phase 03 readiness. |

Local preservation/generation helpers and private validation copies are outside
the repository and are not application dependencies or shared artifacts.

## Validation results

Commands use the installed JDK/SDK environment and the existing local init script.

```powershell
.\gradlew.bat -I .gradle/local-sdk.init.gradle :app:testDebugUnitTest --rerun :app:lintDebug :app:assembleDebug --console=plain
.\gradlew.bat -I .gradle/local-sdk.init.gradle :app:testDebugUnitTest --tests com.taskpro.app.vault.VaultExternalIntentTest --console=plain
```

| Check | Result |
| --- | --- |
| Unchanged baseline | 74 executed tests passed; lint 0 errors / 60 warnings; debug build successful. |
| Pre-fix regression reproduction | 6 tests executed; 2 permitted-URL cases passed; 4 protection cases failed as expected. |
| Fixed focused tests | 6 executed tests passed; zero failures/errors. |
| Fixed full suite | 80 executed tests passed across 11 suites; all 74 original test names retained, plus the 6 new security cases. |
| Fixed lint and debug build | Both successful; lint 0 errors / 60 warnings. Warning signatures (ID, severity, message, file) match the baseline exactly, allowing source-line shifts. |
| Final diff and preserved-file hashes | Only VaultScreen.kt differs among the 115 preserved files; the other 114 are byte-identical. New files are only the security test and four Phase 02 documentation/inventory files. HEAD and staged state remain unchanged. |

The six tests check standard HTTPS opening, custom non-sensitive HTTP URL opening,
password protection before/after reveal and copy, sensitive custom URL protection,
secret-type protection even when its separate sensitive flag is false, and the
absence of URL actions on other standard/custom field types. They inspect captured
ACTION_VIEW intents in Robolectric; they do not launch or certify a real browser.

Existing executed baseline coverage includes Tasks data through backup/restore and
module switches, master/recovery operations, session timeouts, device-wrapper
cryptography, legacy/unified/selective backups, rollback/journal replay, the Room
1->3 migration chain, reminder rearming after restoration, and reminder UI layouts.
No unrelated task/reminder/schema behavior was changed to expand coverage.

The same full-suite coverage passed after the fix. No new test, lint or debug-build
failure remains. The deliberate pre-fix failures demonstrate the vulnerability;
they are not baseline failures or regressions introduced by the fix.

Existing lint warning groups: GradleDependency 33, UnusedResources 13,
AndroidGradlePluginVersion 3, ConstantLocale 3, MonochromeLauncherIcon 2,
ObsoleteSdkInt 2, and one each of OldTargetApi, PluralsCandidate, UseTomlInstead,
VectorRaster. No dependency, SDK, resource, locale or launcher change was made
to suppress those pre-existing findings.

The branding inventory covers 424 exact occurrences across 91 files: 31 user-facing,
292 internal and 101 compatibility-critical. Each file/line/column/token was checked
against the source, with no duplicate occurrence locations. All application labels,
Persian translations, backup identifiers, AAD strings, namespaces and internal
identifiers remain unchanged; the classifications are plans, not edits.

## Limitations and follow-up gates

- No physical phone or emulator validation is claimed. Real Keystore/sensor
  enforcement, notification/alarm delivery, permission denial, reboot/process death,
  screenshots and browser integration remain device/release gates.
- The exact CI JDK 17/build-tools 35 configuration is not installed locally;
  workstation success with the recorded override does not certify that environment.
- No production signing configuration or verified installed-phone certificate is
  available. No signing key was inspected, exported, copied, rotated or published
  by the agent. Gradle uses the existing local debug signing identity normally.
- Existing release output is unsigned; CI distributes a debug APK. Feature branches
  can update the same rolling debug release. Release-policy/signing changes require
  separate approval and were not implemented.
- Schema export remains disabled; broader migration/device fixtures, task-edit/drag
  integration tests, restore interruption testing and dependency security review are
  proposed additions, not fabricated executed results.
- Existing Android cloud/device backup policies remain enabled. No user database
  or personal Vault data was inspected or modified; all tests use synthetic data.
- The full dirty baseline still needs human review and explicit commit authorization.

Phase 02 implementation and automated verification are complete. Ready for human
review of a display-only Phase 03 branding scope, while compatibility-critical
identifiers remain frozen. Production distribution is not certified: signing
ownership, same-certificate installed updates, device behavior and clean CI parity
remain gates before release. No Phase 03 branding, schema, visual or architecture
work has started, and no commit/push is authorized by completing this phase.
