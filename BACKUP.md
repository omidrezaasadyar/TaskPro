# Legacy Task Backup Format (Version 1)

Vaurivo creates encrypted, module-selectable version-2 backups by default.
See [Vault and Unified Backup](SECURE_VAULT.md) for current usage, passwords,
security, and migration details. The notes below describe the original version-1
task-only format, which remains readable by the new restore flow.

Settings contains Create Backup, Restore Backup, Last Backup, and a Share Backup
action after a successful export. Android's document picker chooses the destination
or source, including supported cloud providers. Keep the JSON file in Documents,
Downloads, cloud storage, or on another device, outside Vaurivo's private storage.
The file can be restored after reinstalling Vaurivo or installing it on a new phone.
It is not encrypted and contains task notes; share it only with trusted recipients.

Restore validates the entire file, then asks for confirmation before replacing all
current groups, tasks, and preferences. Selecting a file or cancelling confirmation
does not import anything. Export the current data first to retain a rollback copy.

## Included Data

- Groups: IDs, names, color accents, creation timestamps.
- Tasks: IDs, parent group IDs, titles, notes, due/reminder timestamps, completion
  or snoozed status, manual priority order, creation timestamps.
- Preferences: theme, background, and last successful backup timestamp.

Vaurivo currently has no recurring-task model or separate priority labels. Manual
task ordering is its priority system. Android permissions, notification channel
settings, and device time zone are OS settings and cannot be transferred by this
file. Allow notifications on the destination device. Future pending and snoozed
reminders are rescheduled; completed tasks and past reminders do not ring again.

## Format and Safety

The UTF-8 JSON envelope identifies `com.taskpro.backup`, format `version: 1`, an
exact JSON payload string `dataJson`, and its SHA-256 checksum `sha256`. The payload
includes creation time, producing app version, items, tasks, and settings. This is
logical data, not a copy of a live SQLite or WAL file. SHA-256 detects accidental
damage; it does not authenticate the sender or encrypt the contents.

The reader limits input to 20 MiB and 100,000 records per entity type. It rejects
unknown format versions, invalid types/enums/timestamps, missing required fields,
duplicate IDs, broken group references, and checksum mismatches before any write.
Application versions can differ when they support the same backup format. Future
format changes must retain a version-1 decoder or provide an explicit migration;
unknown formats must never be partially imported.

Room replaces groups and tasks in one transaction, preserving IDs and order. A
recovery journal is committed in that same transaction. Preferences and Android
alarms cannot participate in SQLite transactions, so the journal finishes these
operations idempotently before being removed. Startup blocks the main UI until
recovery completes; boot recovery also runs before rescheduling reminders. A failed
database transaction leaves the old database intact. A failure after commit retains
the journal and presents Retry. No destructive database migration is used.

## Verification

Run `gradlew.bat testDebugUnitTest assembleDebug lintDebug` with JDK 17 and Android
SDK 35. Tests cover all-field/Unicode round trips, empty backups, compatible app
versions, unsupported formats, corruption, invalid records, transactional rollback,
interrupted-restore recovery, reminders, file round trips, and database migration.

On a device, additionally verify the document picker and share sheet with local
and cloud providers, cancel/back, rotation during selection, insufficient space,
and a save/uninstall/reinstall/restore cycle using the externally saved file.
