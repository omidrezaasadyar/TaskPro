# Vaurivo Development Guidelines

These rules apply to the entire repository. Vaurivo is the user-facing product name;
`TaskPro` remains in compatibility-sensitive internal identifiers.

## Architecture and scope

- This is an Android Kotlin application in `:app`, using Jetpack Compose, Material 3,
  Room, and Preferences DataStore. Inspect the relevant code and tests before editing.
- Keep ownership clear: `data/` owns Tasks and Room, `vault/` owns encrypted Vault
  storage and security, `backup/` owns export and restore, `settings/` owns preferences,
  `notification/` owns reminders, and `ui/` owns Compose screens, navigation, shared
  components, and theme. Reuse established repositories, state holders, and design tokens.
- Search narrowly (`rg`/`rg --files`), read repository instructions, check `git status`,
  and identify callers, persisted data, tests, and localization before changing behavior.
  Prefer targeted inspection and concise context over broad repository dumps.
- Make the smallest change that satisfies the approved task. Do not refactor, rebrand,
  format, or modify unrelated files. Do not fabricate production data or UI metrics.
- Preserve existing Tasks, Vault, backup/recovery, navigation, reminders, settings,
  localization, and user data. Work with pre-existing edits; never reset, clean, stash,
  discard, or overwrite someone else's work without explicit approval.

## Security and compatibility

- Never rename `com.taskpro.app` (application ID or namespace), `taskpro.db`, package
  identifiers, persisted keys, backup identifiers, Keystore aliases, encryption
  parameters, or signing/update identity without explicit approval and a verified
  migration plan. Keep Vaurivo as the name shown to users.
- Never use destructive Room migration fallbacks. Version schema changes, provide
  migrations from supported versions, and test them with representative existing data.
  Preserve stable IDs, relationships, backup compatibility, and reminder restoration.
- Treat Vault data as confidential, including metadata and notes. Preserve authenticated
  encryption, key derivation, AAD, recovery-key behavior, lock/session boundaries, and
  encrypted backups. Never store or log plaintext secrets, introduce a recovery bypass,
  weaken authentication, or send credentials to a remote service without explicit approval.
- Validate backup data before applying it; keep restore transactional or rollback-safe.
  A module-only restore must not overwrite the other module. Do not claim recovery or
  clipboard clearing guarantees beyond what the platform actually provides.
- Never commit credentials, signing keys, local databases, private recovery archives,
  machine-specific configuration, or generated binaries. Do not print secret contents.
  Review staged paths and high-confidence secret indicators before every commit.

## UI, localization, and accessibility

- Follow existing Compose state ownership, navigation, Material 3 patterns, and Vaurivo
  theme tokens. Keep controls functional and consistent across Tasks and Vault.
- Update English and Persian resources together for user-facing text. Test RTL layout
  while keeping mixed-direction usernames, URLs, passwords, and numbers readable.
- Support compact screens, large font scaling, system insets, accessible labels, clear
  focus/pressed states, and touch targets of at least 48 dp where applicable. Respect
  reduced-motion settings for nonessential animation.

## Validation and Git

- Add focused regression tests for changed behavior, especially persistence, Vault
  security, backup/restore, reminders, and navigation. Run relevant tests first, then
  `:app:testDebugUnitTest`, `:app:assembleDebug`,
  `:app:compileReleaseUnitTestKotlin`, and `:app:lintDebug` for application changes.
  Use the Gradle wrapper and the local Android SDK configuration available on the host.
- Preserve existing passing tests; investigate any regression. Do not introduce lint
  errors. Report executed tests and warning counts honestly, distinguishing cached,
  skipped, unavailable, and newly failing checks. Documentation-only changes do not
  require a full Android build unless they alter build or release instructions.
- Before risky multi-file, storage, or cryptographic work, verify a recoverable snapshot
  outside the repository. Exclude local secrets from general-purpose snapshots and
  verify integrity before any Git restructuring or migration.
- Develop on a task branch unless the owner directs otherwise. Inspect status and diff,
  stage only reviewed files, and verify the staged diff. Never reset, clean, force-push,
  rewrite history, merge, commit, or push without explicit authorization for that action.
- Inspect `.github/workflows/` before pushing: pushes to `main` or `claude/**` can build
  and publish a rolling debug release. Never create or trigger a release automatically.
  Request separate approval for publication and for changing workflow triggers.

## Documentation and completion

- Keep phase reports, design evidence, and technical docs accurate and portable. Do not
  commit private absolute paths or present historical report claims as fresh test results.
- Finish with a concise account of changed files, behavior, validation, compatibility
  risks, remaining limitations, and Git actions actually performed.
