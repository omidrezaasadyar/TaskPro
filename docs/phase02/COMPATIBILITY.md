# Phase 02 compatibility inventory

Status: preservation inventory, not a migration. No identifier, storage schema,
cryptographic format, signing configuration, or visible application brand is changed
in Phase 02. Values below were checked against the current working tree on
2026-10-20. Paths in this document are repository-relative.

## Application and update identity

| Contract | Existing value / evidence | Rule |
| --- | --- | --- |
| applicationId | `com.taskpro.app`; `app/build.gradle.kts:13` | Keep unchanged; changing it creates a different installed application. |
| Namespace and production packages | `com.taskpro.app`; application configuration and Kotlin package declarations | No package migration in the branding phase. |
| Current version | `2.0.4`, versionCode `9`; `app/build.gradle.kts` | Phase 02 leaves both unchanged. Future installed updates need an appropriate increasing version code. |
| SDK contract | minSdk 24, targetSdk 35, compileSdk 35 | No change here; retain API coverage when planning upgrades. |
| Signing | No explicit release signing configuration; existing release output is unsigned | Identify the certificate of the actually installed app and ownership of its signing key before promising an in-place release update. No keystore/private key was copied or read for this inventory. |
| Local debug vs CI | Local Gradle uses the local debug signing identity; CI does not declare a stable signing key | Same applicationId is insufficient to establish update compatibility. Local/CI/phone certificate continuity is unverified. Do not uninstall to work around a mismatch. |
| Internal symbols | `TaskProApp`, `TaskProTheme`, `Theme.TaskPro`, Gradle root project `TaskPro` | Not public branding requirements. Retain during Phase 03 unless a separate, tested cleanup is approved. |

The branch remains `main`, at HEAD
`faadf57eb4911089181bde52fe93f2b90a710266`. The existing remote is
`https://github.com/omidrezaasadyar/TaskPro.git`. GitHub repository renaming,
release tag changes and distribution changes are separate decisions.

## Room data

Source: `app/src/main/java/com/taskpro/app/data/AppDatabase.kt`, `Task.kt`,
`Item.kt`, `RestoreJournal.kt`, and `vault/VaultStore.kt`.

- Database filename: `taskpro.db`; current Room schema version: **3**.
- `items`: `id`, `name`, `colorArgb`, `createdAt`.
- `tasks`: `id`, `itemId`, `title`, `notes`, `dueAt`, `status`, `position`,
  `createdAt`; `itemId` references `items.id` with cascading deletion and an index.
- Stored task statuses: `PENDING`, `COMPLETED`, `SNOOZED`. Preserve IDs,
  relationship IDs, epoch-millisecond timestamps and manual positions.
- `restore_journal`: `id`, `theme`, `background`, `lastBackupAt`, `oldTaskIds`.
  It completes preferences/reminder application after committed restores.
- `vault_state`: `id`, `envelope`, `revision`; active singleton ID is 1.
- `vault_chunks`: `position`, `ciphertext`; envelope storage is split into
  256-KiB text chunks. DAO reconstruction and revision-checked writes are transactional.
- Migration 1 -> 2 adds the restore journal; migration 2 -> 3 adds Vault tables.
  Both are registered. No destructive fallback is configured.
- `exportSchema` is currently false. Exported-schema fixtures and broader device
  migration coverage are proposed follow-up work, not changes made in this phase.

The existing test `BackupRepositoryTest.versionOneDatabaseMigratesWithoutLosingExistingTasks`
checks the 1 -> 3 migration chain with pre-existing task notes and group data.

## DataStore

Source: `app/src/main/java/com/taskpro/app/settings/SettingsRepository.kt`.

| Contract | Exact value |
| --- | --- |
| Store name | `settings` (platform file under `datastore/`) |
| Theme key / values | `theme_mode`: `SYSTEM`, `LIGHT`, `DARK` |
| Background key / values | `background_style`: `PLAIN`, `GRADIENT`, `AURORA` |
| Last backup key | `last_backup_at`, nullable epoch-millisecond `Long` |

Keep persisted names independent of translated display labels. Settings and Room
are separate stores; the restore journal is required for completing cross-store
restoration. A Room transaction alone does not make DataStore/alarm operations atomic.

## Vault document and encryption

Sources: `vault/VaultCrypto.kt`, `VaultModels.kt`, `VaultRepository.kt`.

| Contract | Existing value |
| --- | --- |
| Payload schema | Vault document JSON `version: 1`; UUID record, custom-field and category IDs; category parent IDs and record category IDs |
| Field enum names | `TEXT`, `SECRET`, `URL`, `EMAIL`, `NUMBER`, `DATE`, `NOTES` |
| Category icon enum names | `FOLDER`, `BANK`, `EMAIL`, `WEB`, `BUSINESS`, `SERVER`, `KEY`, `PERSON` |
| Envelope | `version: 1`, `kdf: argon2id`; existing salt, wrapping and data fields |
| KDF | Argon2id version 0x13; memory 19,456 KiB; iterations 2; parallelism 1 |
| Key/salt | Random 32-byte data key; random 16-byte password-KDF salt |
| Cipher | `AES/GCM/NoPadding`; 32-byte key; 12-byte nonce; 128-bit tag |
| Password policy | Master/backup creation requires 12-1,024 characters |
| Limits | Document 4 MiB; envelope bound 128 MiB |
| Foreground lock | Default 120 seconds; supported stored values 30, 60, 120, 300, 600 |
| Away lock | `AWAY_LOCK_MILLIS = 60000`; process death loses the in-memory key |

Authenticated data is a byte-level compatibility contract, not display branding:

```text
TaskPro:<purpose>:1:argon2id:19456:2:1
```

Existing purposes include `vault:key`, `vault:data`, `backup:key`, `backup:data`,
`device-pin:key`, `device-pin:data`, `vault:recovery:key`, and
`vault:recovery:view`. Retain the exact UTF-8 strings, separators, case, purposes,
nonce/tag rules and current version decoding. A cosmetic global replacement would
make existing ciphertext fail authentication.

Initial setup generates a random 32-byte recovery key and requires saved-key
confirmation before dashboard access. The master password is not stored in
plaintext. Recovery viewing/regeneration requires authentication. Regeneration
rotates the data key; password recovery also rotates it and consumes the current
recovery wrapper. Historic backups retain their historic recovery access. After
password recovery, configure a replacement recovery key and create a new backup.
Never record actual master passwords, PINs, recovery keys or decrypted records in
logs, fixtures, diagnostics or reports; tests use synthetic values only.

## Device unlock and clipboard

Sources: `vault/VaultDeviceUnlock.kt`, `VaultClipboard.kt`.

| Contract | Exact value / behavior |
| --- | --- |
| Device file | `noBackupFilesDir/vault-device-unlock.json`; AtomicFile; not portable |
| PIN Keystore alias | `taskpro.vault.pin.v1` |
| Biometric Keystore alias | `taskpro.vault.biometric.v1` |
| PIN-derived password prefix | `TaskPro PIN:` (including the space and colon) |
| PIN outer AAD | `TaskPro:device:pin:<binding>` |
| Biometric AAD | `TaskPro:device:bio:<binding>`; submitted after authentication |
| Binding | Base64, no-wrap SHA-256 of concatenated envelope salt and wrapped-key strings |
| Device algorithms | Keystore AES-256-GCM; biometric key requires per-use strong-biometric authentication |
| PIN policy | 4-6 ASCII digits; fifth failed attempt starts a persisted 30-second cooldown |
| Clipboard ownership key | `taskpro.clip` |
| Clipboard handling | Sensitive marker; clear after 30 seconds only if token and content still match; best effort |

The clipboard display label `TaskPro` is a branding candidate, but `taskpro.clip`
is an ownership identifier and must remain unchanged. Master-password changes,
recovery and differently keyed restores invalidate device profiles by binding.
Reinstallation/device transfer requires the master password or recovery key and
new device-unlock setup. No Keystore key is exported in routine backups.

## Backup formats and platform backup

Sources: `backup/BackupFormat.kt`, `WorkspaceBackup.kt`, `BackupRepository.kt`,
`res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`.

| Contract | Existing value |
| --- | --- |
| Legacy task backup | `format: com.taskpro.backup`, `version: 1`; `dataJson` and `sha256`; 20-MiB input limit |
| Unified backup | `format: com.taskpro.workspace`, `version: 2`; encrypted `envelope`; decoded payload version 2; 130-MiB input limit |
| Scopes | `FULL`, `TASKS`, `VAULT`; records/configuration for selected modules |
| Selection behavior | Restore replaces selected modules; does not merge or erase unselected modules |
| Restore safety | Validation/decryption before import; Room transaction; durable task preference/reminder journal |
| Suggested filenames | `TaskPro-backup-<date>.json` and `TaskPro-previous-vault-<time>.json`; cosmetic candidates, not format identifiers |
| OS backup | Application backup enabled; database `taskpro.db` and `datastore/` included in cloud/device-transfer rules |

Legacy files are plaintext plus an unkeyed corruption checksum. They must not be
described as authenticated/encrypted. Version-2 files use authenticated encryption,
and a backup-file password is distinct from a Vault master password. Preserve both
readers during rebranding. Do not rename format markers to `com.vaurivo.*`.

New-empty-vault creation preserves an existing encrypted envelope using the current
version-2 format, verifies external output, and performs a revision-checked update.
This starts an empty Vault; it is not password recovery. Its saved old data still
needs the old password or recovery key. Task Manager data is not replaced.

## Notifications, routes and external integration

Sources: `notification/ReminderScheduler.kt`, `TaskProApp.kt`, navigation files,
`.github/workflows/build-apk.yml`.

- Channel ID: `task_reminders`; reminder action: `com.taskpro.app.ACTION_REMINDER`.
- Extras: `extra_task_id`, `extra_task_title`; request/notification IDs currently
  derive from `taskId.toInt()`. Do not change PendingIntent identity during a rebrand.
- Routes: `workspace`, `home`, `vault`, `search`, `settings`, `item/{itemId}`,
  `item/{itemId}/status/{status}`. Visible navigation labels may change; retain routes.
- CI artifact name: `taskpro-debug-apk`; release tags: `latest-debug`, `build-error`;
  APK filename: `app-debug.apk`. Coordinate changes with download/automation consumers;
  changing display names does not require changing these identifiers.
- Manifest component names, backup rules and permission behavior are unchanged.

## Phase 03 compatibility gates (proposed, not executed here)

1. Confirm installed signing certificate and original-key ownership; prove a same-key
   in-place update without uninstalling or changing applicationId.
2. Open existing task data, master/recovery-enabled Vaults and old device profiles.
3. Read legacy and unified backups, restore each scope, and test wrong-password,
   tampering, cancellation, rollback and interrupted restore paths.
4. Exercise migrations on representative schema-1/2/3 installations with real Room
   metadata, reminders, preferences and large/chunked Vault fixtures.
5. Test notification delivery, permission denial, reboot restoration, biometric
   cancellation/enrollment changes, clipboard and secure-window behavior on devices.

The automated tests executed during Phase 02 are listed separately in REPORT.md.
This inventory is not evidence that the proposed device/release gates passed.
