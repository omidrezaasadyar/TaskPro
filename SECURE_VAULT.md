# Vaurivo: Unified Workspace and Vault

## Using the Application

The application opens at the workspace selector. The bottom navigation switches
between Workspaces, Tasks, and Vault. Existing task lists, editing,
status actions, ordering, reminders, preferences, and task sharing remain available.

Create a vault with a master password of at least 12 characters and confirm it.
Prefer a long, unique passphrase; the local strength estimate is guidance, not a
guarantee. New vaults automatically generate a recovery key and require confirmation
that it has been saved outside the app. There is **no hidden recovery mechanism or email reset**. Losing both the master password and
a previously configured recovery key makes the vault unrecoverable.

### Offline Recovery (2.0.1)

Initial setup shows a generated key before dashboard access. Confirmation persists;
if setup is interrupted, unlocking resumes the key-saving step. Open Vault >
Vault security > Recovery Key Management to view or regenerate a key after re-entering
the master password. Regeneration replaces the recovery key and rotates the data key
without changing records. Save the key outside Vaurivo, separately from backups.
Copying uses sensitive clipboard handling. Create a new backup after regeneration.
A new key cannot recover older backups. Legacy recovery wrappers remain usable for
password recovery, but cannot be viewed until regenerated in this version.

On the login screen, choose Forgot Password?, enter the saved key, and enter/confirm
a new master password. Successful recovery preserves all records, rotates the data
encryption key, removes recovery access from the current vault, and leaves it locked.
Unlock with the new password, then configure a new recovery key and make a new backup.
An ordinary master-password change preserves configured recovery access.

Backup restore also offers Use a saved recovery key for the selected vault module.
The separate backup-file password remains required. Recovery and replacement are
validated before any database writes and use the existing atomic restore transaction.
Unselected Tasks data is unaffected. Historical backups remain recoverable
with their historical passwords/keys; offline copies cannot be remotely revoked.

Recovery uses a random 256-bit key from SecureRandom to AES-256-GCM-wrap the vault's
data key, with a fresh 96-bit nonce and separate `vault:recovery:key` associated data.
The optional versioned recovery envelope also stores the recovery key encrypted under
the vault data key, with a separate random nonce and `vault:recovery:view` associated
data. Raw recovery keys are never persisted. Viewing requires current master-password
authentication and an unlocked session. Incorrect keys, altered
wrappers, and wrappers from another vault fail authentication before import/reset.
Existing envelope-version-1 vaults without recovery still open normally. No database
migration or new dependency is needed. Keep recovery-enabled backups on version
2.0.1 or newer; older clients do not provide this recovery workflow.

The recovery key is as sensitive as a master password: anyone possessing it and a
matching vault file can recover the contents. There is no server, email delivery,
remote reset, or security-question fallback. This remains unaudited security-sensitive
code; independent review and physical-device validation are required before release.
Design guidance: [OWASP offline password recovery](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html#offline-methods).

### Vault Organization

The dashboard shows live group/item totals, colored folders, and an Add Item action.
Open a folder to see immediate subgroups and its records; Back moves to its parent.
Search spans groups and permitted record fields across the vault. Filters and sorting
are behind the filter icon. Category navigation retains rename, move, reorder, and
delete controls. New categories cycle through an editable accent palette.
Tasks and Settings share the default workspace backdrop; explicitly selected
Plain and Aurora backgrounds remain supported. Native Compose motion follows the
system animation-duration setting; no Lottie dependency is required.

The category panel supports nested categories, names, icons, color swatches and
custom RGB colors. The category menu provides edit, move up/down, and deletion.
Editing the parent moves a category. Deleting a category preserves its records in
Uncategorized. Optional starter categories are available when the tree is empty.
Use the folder button on phones; wider screens show the sidebar persistently.

Records support a title, category, URL, username, email, password, phone, account
reference, notes, tags, favorites, and creation/modification timestamps. The editor
adds typed custom fields and sensitivity controls. Edit a record's category to move
it. Details provide duplicate, delete with confirmation, copy, reveal, and safe
HTTP(S) website opening. No payment PIN, CVV, or one-time code is requested.

Search and tag filtering run only in unlocked memory. Passwords and sensitive custom
fields are excluded from search. Sort by title, modified time, creation time, or
category. Secret values are masked initially. The password generator uses
SecureRandom, supports lengths 12-128 and selected character classes, and ensures
each selected class appears. zxcvbn4j estimates strength locally with bundled
dictionaries, without contacting a service.

Ctrl+F focuses search, Ctrl+N creates a record, Ctrl+L locks, and Ctrl+Shift+H returns
to Workspaces while using the vault workspace. Touch and keyboard activity reset the
inactivity timer while Vault is visible. Security settings allow 30, 60, 120, 300,
or 600 seconds for foreground inactivity. Leaving Vault or backgrounding the app
starts the separate one-minute `VaultRepository.AWAY_LOCK_MILLIS` deadline. Returning
checks the deadline before preserving a session. Manual lock and process death lock immediately. A
master-password change requires the current password and locks after success.

## Encryption and Threat Model

The complete vault document is encrypted, including category names, record titles,
tags, notes, custom fields, timestamps, and inactivity preferences. Room holds a
`vault_state` header and `vault_chunks` ciphertext rows, bounded to 256 KiB each
to avoid Android CursorWindow limits, with an optimistic revision number;
there are no plaintext vault tables, search indexes, or cached files.

- A random 256-bit data key encrypts the document using platform AES/GCM/NoPadding.
- AES-GCM uses random 96-bit nonces and 128-bit authentication tags. Every content
  rewrite and password-key wrapping uses a newly generated nonce.
- Bouncy Castle Argon2id v1.3 derives a 256-bit wrapping key from the master password
  and a random 128-bit salt: 19,456 KiB memory, 2 iterations, parallelism 1. These
  parameters meet OWASP's published minimum Argon2id profile. Parameters are fixed
  and validated before derivation to reject hostile memory/iteration settings.
- AES-GCM wraps the data key under the derived key. Domain-separated associated
  data binds vault versus backup use, key wrapping versus content, and the format
  and KDF profile. Neither the master password nor an unwrapped key is persisted.
- Password changes rewrap the data key with a new salt and wrapping nonce and
  reencrypt content with a fresh nonce, using a conditional atomic row update.
- Locking zeros owned key byte arrays, drops decrypted document references, and
  disposes editor/detail state. Authentication completed after a lock cannot reopen
  the session. JVM/Compose strings, crypto-provider internals, and garbage-collected
  copies cannot be guaranteed to be wiped from physical memory.

Vault windows and secret dialogs use FLAG_SECURE. Password input disables ordinary
text suggestions; clipboard data is marked sensitive. Clipboard cleanup checks both
an ownership token and the content hash before attempting to clear it after 30
seconds or on lock. Other clipboard content is left untouched. Android restrictions,
clipboard managers, process termination, and already-read copies prevent guarantees.

There is no new network permission, remote sync, analytics, or credential service.
Explicit website opening and user-selected document providers are platform actions.
The app's pre-existing Android OS backup/device-transfer policy still includes the
database; vault content in such copies is ciphertext. Tasks data remains in
its original plaintext Room tables. OS permission and notification-channel settings
are not transferable through manual backups.

This protects stored files and portable backups, not a compromised/rooted device,
a debugger attached to a debug APK, a malicious keyboard/accessibility service,
or someone using an already-unlocked device. Use a non-debuggable release build for
real credentials. This implementation has automated tests but has not received an
independent cryptographic/security audit. Offline guessing resistance depends on a
strong master/backup password. Historical ciphertext rollback cannot be detected
without an external trusted freshness anchor. Old backups retain their old master
password after a password change.

References: [OWASP password storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html),
[OWASP cryptographic storage](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html),
[Bouncy Castle](https://www.bouncycastle.org/download/bouncy-castle-java/),
[zxcvbn4j](https://github.com/nulab/zxcvbn4j).

## Storage and Upgrade Safeguards

Room schema 1 contained `items` and `tasks`. Schema 2 added `restore_journal` for
task preferences/reminder recovery. Schema 3 only adds `vault_state` and `vault_chunks`; migrations
1->2->3 and 2->3 do not delete, rename, or rewrite any user task or preference.
No destructive migration fallback is enabled. IDs and task ordering remain intact.
Vault record and category IDs are independent UUIDs inside the encrypted document.
The document validates IDs, parent references, cycles, custom field types, and URLs.

Writes serialize through the vault repository and use a conditional revision update.
Backup snapshots read both modules in one Room transaction. Restore replaces selected
modules in one transaction and uses the existing durable journal for DataStore and
Android reminder side effects. An import write failure rolls back both modules.
After a committed restore, startup recovery finishes journaled side effects before
the main UI opens. Vault-only restores do not change tasks or app theme preferences;
task-only restores do not change the vault row or its password.

## Manual Backup and Restore

The public product name is now Vaurivo. Suggested exports are named
`Vaurivo-backup-<date>.json`; preserving a previous vault suggests
`Vaurivo-previous-vault-<time>.json`. Filenames do not determine the format.
Existing TaskPro-named files remain importable without renaming, conversion,
or re-encryption. All protocol markers and cryptographic associated data remain
unchanged; this is a display-only rebrand, not a storage migration.

1. Open Settings > Backup & Restore and choose Full workspace, Tasks only,
   or Vault only. A vault-only export requires an existing vault.
2. Choose Create Backup, select an external destination, and enter and confirm a
   strong backup password. Share Backup is available after a successful write.
3. Keep the file outside app-private storage, such as Documents, Downloads, another
   device, or your chosen document provider. Retain both the backup password and,
   for vault data, the vault master password separately from the file.
4. On the destination installation, choose Restore Backup, select the file, and enter
   its backup password. Leave it blank only for a legacy version-1 task backup.
5. Review date, app version, and available modules. Choose the modules to replace.
   Enter the backed-up vault's master password when selecting Vault, or choose
   Use a saved recovery key and supply that backup's recovery key and a new password.
6. Confirm replacement. Unselected modules stay unchanged. Merging is deliberately
   unsupported and the UI says so. Back up current data before replacing it.

New files use the `com.taskpro.workspace` version-2 envelope. An independent backup
password encrypts/authenticates the complete manifest and payload; the embedded
vault remains encrypted under its own master password as well. A backup can be
created while the vault is locked. An empty/unconfigured vault is omitted rather
than represented as an instruction to delete a destination vault. Future unsupported
formats fail before writes. Legacy `com.taskpro.backup` version-1 task files are
accepted through their original decoder and checksum checks; their checksum is not
cryptographic authentication and the restore preview identifies them as legacy.

The current implementation limits the plaintext vault document to 4 MiB, the
encrypted envelope to 128 MiB, and the portable file to 130 MiB. It loads bounded documents in memory rather than streaming
an unlimited archive. Oversized exports/imports fail clearly before database writes;
available device memory and storage also impose limits. Interrupted external writes
may leave an unusable partial file, which validation rejects; create a new backup.
Recoverable trash, autofill integration, cloud sync, attachments, and
cross-vault merge are not implemented. Permanent record deletion requires confirmation.

## Verification

Run `gradlew.bat testDebugUnitTest lintDebug assembleDebug` with the project SDK.
CI runs these checks before publishing an APK. Tests use synthetic credentials only.
Coverage includes legacy compatibility, all-field round trips, task migration,
AES-GCM authentication, wrong passwords, fresh nonces, bounded KDF parameters,
secret-free search, category cycles, password changes, inactivity locking,
clipboard ownership, selective/full restoration, rollback on simulated database
failure, existing interrupted-restore recovery, and record/category UI workflows.
Rendered test previews live under `app/build/reports/workspace`.

Local verification on 2026-10-17 for 2.0.1: all 50 tests passed; `assembleDebug`,
`assembleRelease`, and `lintDebug` succeeded (lint warnings remain). The local
machine used JDK 21 and an ignored Gradle init script selecting installed Android
build-tools 36; CI retains JDK 17 and build-tools 35. Both compile against SDK 35.
Navigation tests preserve task titles/notes across workspace switches and verify
vault locking. Keyboard create/cancel/lock, category rename, password reveal/hide,
all custom field types, large-vault chunk storage, and exports of all three backup
scopes are covered. A full exported file also restores into a fresh database.
Recovery tests cover setup reauthentication, key replacement, password changes,
wrong/missing/tampered keys, key consumption, data-key rotation, backup recovery and
transaction rollback. UI tests exercise setup/reset and render light/dark and compact
RTL layouts with enlarged text. Lint reports 0 errors and 57 warnings.
The release APK is unsigned until the application's release signing key is supplied;
the installable debug APK is only for synthetic-data testing.

Physical-device checks still needed before distribution include document/cloud
providers, background/app-switch timing, keyboard/autofill behavior, clipboard
restrictions on modern Android, notification/alarm permissions, accessibility,
and actual uninstall/reinstall restoration. An independent security review is
recommended before storing high-value production credentials.

Dashboard/recovery refinement validation (2026-10-17): 55 tests pass, including
automatic setup confirmation, interrupted setup, authenticated key viewing, legacy
recovery compatibility, view-ciphertext tampering, regeneration data-key rotation,
folder navigation/search, immediate category updates, and compact RTL large-text
rendering. Debug/release builds and lint succeed. No database schema or dependency
changes were introduced by these refinements. Release signing and physical-device
security/accessibility checks remain required before distribution.

## Device Unlock

The locked Vault screen uses the supplied dark/mint design inside the existing
destination. Home and Tasks remain accessible, including while the in-destination
password/PIN panel is open. No protected record count is exposed while locked.
The font uses the existing bundled/default sans fallback; other screen designs
are unchanged. Initial vault creation still requires a 12-character master password
and recovery-key confirmation.

From the locked screen, open Unlock settings, authenticate with the master password,
then save a 4-6 digit PIN and/or enable biometrics. Prefer six digits. Biometric setup
itself requires a successful strong-biometric CryptoObject operation. After setup,
entering the locked destination automatically prompts once; cancellation does not
loop. Tap the fingerprint target to retry. Without configured biometrics, the locked
landing screen remains visible until the user chooses PIN or master password; entry
panels no longer automatically cover it. On supported devices, the fingerprint target
offers authenticated setup if it is not configured yet. Forgot Password retains the
existing recovery flow. Biometric enrollment changes may invalidate device access;
use the master password and reconfigure it.

To view the lock screen, open an existing vault and select Lock vault. A fresh vault
must complete master-password/recovery-key setup first. Returning to an unlocked
session within the existing timeout still opens its dashboard. Version 2.0.2
(version code 7) identifies the corrected local build; it is not automatically
published to GitHub by a local Gradle build. Install updates over the existing app
with the same signing key; do not uninstall to work around a signature mismatch.

Version 2.0.3 (version code 8) defers biometric AES-GCM AAD submission until the
authenticated CryptoObject callback. The AAD bytes and existing device wrappers
remain compatible. PIN setup verifies the Keystore encrypt/decrypt round trip before
replacing the profile, and device-file commits are checked by reading them back.
The setup form explicitly requests the existing master password.

Device-unlock failures display stable, non-secret codes: DU01 for master-password
verification, DU02 for PIN-envelope creation, DU03 for Keystore operations, DU04 for
device-file storage, and DU05 for a changed vault during setup. No exception messages,
stack traces, passwords, PINs, or keys are displayed or logged. These codes identify
the failed stage rather than asserting a device-specific root cause. Master-password
unlock remains independent of the optional device profile.

Version 2.0.4 (version code 9) makes master-password creation explicit. On an empty
installation, opening Vault shows New master password and Confirm password,
followed by mandatory recovery-key confirmation. Locked-vault landing and unlock
settings also offer Create master password.

If a stored encrypted vault exists, this action is a new empty vault, not a password
reset or decryption bypass. The user confirms this and selects a destination with
Android's document picker. The previous encrypted envelope is exported in the normal
version-2 vault-only backup, whose outer backup password is the newly chosen master
password. The old envelope still requires its original master password or recovery
key. Backup output is reopened and its size/SHA-256 checked before replacement. The
repository then performs the existing transactional, revision-checked vault update;
tasks and settings are untouched. Cancel, output failure, or a changed revision does
not replace the vault. No schema or encryption-format migration is required. If the
file picker is interrupted by recreation/process death, re-enter the new password;
pending setup credentials are not persisted.

PIN access uses the existing Argon2id/AES-GCM implementation plus an outer Android
Keystore AES-GCM layer. Five failed attempts impose a persisted 30-second cooldown.
This is a convenience PIN, not a substitute for a strong master password. Biometric
access uses an auth-per-use Keystore key and BIOMETRIC_STRONG; successful callbacks
alone cannot decrypt the vault. AndroidX Biometric 1.1.0 is the only new direct
dependency; its transitive Fragment library is constrained to 1.8.9 for compatibility
with the existing Activity Result permission API.

Device wrappers contain no plaintext password/PIN or vault key and are saved with
AtomicFile in noBackupFilesDir. They are not exported in portable or Android backups.
Credential bindings invalidate device unlock after master-password changes, recovery,
or replacement with a differently keyed vault. Reinstalling/moving phones requires
the master password or portable recovery key, then new device-unlock setup. Existing
Room schema, vault envelope, and portable backup format are unchanged.

Vault-only FLAG_SECURE remains enabled; Home/Tasks clear it. Biometric operations and
entered credentials are cleared when the host stops, and decrypted session state is
discarded on lock. JVM strings cannot be reliably wiped, and rooted/compromised
processes are outside the protection provided by this design. Keystore hardware
backing depends on the device; no universal hardware-security claim is made.

Validation includes real AES-GCM with an injected test key provider for device-wrapper
tests (the JVM does not provide a real Android Keystore/sensor). Physical-device checks
are still required: biometric enrollment/cancellation/lockout, secure-window screenshot
blocking, rotation, process death, PIN restart cooldown, and background return after
one minute. Platform guidance: https://developer.android.com/identity/sign-in/biometric-auth

Local unlock-feature verification: all 74 tests passed, including first-use master
creation/recovery confirmation, existing-vault creation with verified external backup,
file-picker cancellation, backup failure and stale-revision preservation, failed setup preserving
existing PIN/vault data, Keystore and storage failure reporting, deferred biometric AAD,
legacy biometric-wrapper compatibility, existing-master-password validation, locked landing-page
visibility and entry-panel dismissal, PIN setup/keypad
unlock, master-password unlock, persistent cooldown, stale biometric callbacks,
credential invalidation, task navigation, existing recovery/backup tests, and RTL
large-text rendering. `assembleDebug`, `assembleRelease`, and `lintDebug` succeeded.
No physical Android device was connected; sensor/Keystore enforcement tests remain
on-device release gates. The generated release APK remains unsigned.
