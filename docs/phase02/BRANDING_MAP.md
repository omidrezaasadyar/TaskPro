# TaskPro-to-Vaurivo branding migration map

Planning only, verified against the Phase 02 working tree on 2026-10-20. No
application resource, namespace, identifier, filename suggestion or visual brand
was changed. Classification is per occurrence, not a global replacement rule.

## Scope and interpretation

- `brand-reference-inventory.csv` identifies every matching TaskPro/Task Pro/
  Persian brand occurrence by repository path, 1-based line and column, matched
  token, category, reason and proposed action. It does not copy credential values
  or complete source lines into the report.
- Scope: the 115 baseline project files plus the new security regression test.
  Binary font/wrapper files are not text-scanned. Ignored files, Git internals,
  runtime data, private preservation artifacts, these new planning documents and
  the generated inventories themselves are deliberately excluded.
- User-facing = candidate for an approved display-only rebrand. Internal = no
  branding-driven rename is required. Compatibility-critical = retain exact bytes
  and identifiers. A file can contain all three; never replace all matches in it.
- Source line locations refer to this Phase 02 snapshot and must be regenerated
  or rechecked after subsequent edits. Package declarations are frozen, while
  imports/qualified references/class names are implementation references.

## English and Persian resource plan

| File | Existing display content | Proposed Phase 03 change / constraints |
| --- | --- | --- |
| `app/src/main/res/values/strings.xml` | TaskPro app name, legacy task-only About and backup/error wording | Use approved `Vaurivo` product name; describe existing Tasks and Vault capabilities. Preserve resource keys, placeholders, escapes and safety warnings. Reconcile legacy/plaintext vs unified/encrypted wording by actual use. |
| `app/src/main/res/values-fa/strings.xml` | Persian TaskPro app name/About and backup/error wording | Use the approved Latin `Vaurivo` brand pending a separate approved Persian transliteration. Localize surrounding text; preserve placeholders and RTL behavior. |
| `app/src/main/res/values/vault_strings.xml` | Task Manager, Secure Vault, TaskPro clipboard feedback | Use `Tasks` / `Vault` in branded navigation, or full `Vaurivo Tasks` / `Vaurivo Vault` where context needs it; brand clipboard feedback without changing ownership logic. |
| `app/src/main/res/values-fa/vault_strings.xml` | Persian module names and localized Vault text | Proposed localized module labels: `کارها` and `گاوصندوق` under the `Vaurivo` product name. Confirm terminology; keep credential/URL direction handling. |
| `app/src/main/res/values/recovery_strings.xml` | Task Pro recovery instructions and `Task Pro | v%1$s` | Use `Vaurivo | v%1$s`, retaining the runtime version placeholder and recovery/key-loss warnings. |
| `app/src/main/res/values-fa/recovery_strings.xml` | Localized recovery text and Task Pro version line | Change the visible brand consistently; retain `%1$s`, warnings, Latin version readability and large-font/RTL tests. |
| `app/src/main/res/values/unlock_strings.xml` | Existing unlock/setup labels; no TaskPro text match | No required brand-token replacement. Review only for terminology consistency; preserve setup/recovery/security distinctions. |
| `app/src/main/res/values-fa/unlock_strings.xml` | Localized unlock/setup labels | Same consistency review; no functionality or validation changes in the branding phase. |
| `app/src/main/res/values-fa/vault_dashboard_strings.xml` | Localized dashboard labels | Retain translations and placeholder behavior; verify folder/count/Add Item layouts with the final labels. |
| `app/src/main/res/values/themes.xml`, `values-night/themes.xml` | Internal `Theme.TaskPro` style name | Keep internal style identifiers; visual token changes, if later approved, do not require renaming the style. |

Other display candidates: suggested backup names in BackupSection/VaultMasterSetup,
the clipboard label in VaultClipboard, README/BACKUP/SECURE_VAULT prose, workflow
release display names and matching UI-test expectations. Keep `.json`, old-format
examples, parser IDs, history and external integration contracts intact. The root
Gradle project name and TaskProApp/TaskProTheme are not required to change.

Launcher artwork needs a separately approved Vaurivo asset; no logo was invented
or replaced here. The GitHub repository/remote name, artifact ID, release tags,
APK filename and signing configuration require a separate consumer/update plan.

## File-by-file occurrence map

Counts are occurrence counts, including imports and package declarations. Detailed
locations and individual classifications are in the CSV.

| File | User-facing | Internal | Compatibility-critical | Phase 03 treatment |
| --- | ---: | ---: | ---: | --- |
| `.github/workflows/build-apk.yml` | 3 | 0 | 1 | Change approved display text only; retain frozen contracts. |
| `app/build.gradle.kts` | 0 | 0 | 2 | No branding-driven change; preserve contracts and implementation references. |
| `app/proguard-rules.pro` | 0 | 1 | 0 | No change required for visible rebranding. |
| `app/src/main/AndroidManifest.xml` | 0 | 4 | 0 | No change required for visible rebranding. |
| `app/src/main/java/com/taskpro/app/backup/BackupFormat.kt` | 0 | 6 | 3 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/backup/BackupRepository.kt` | 0 | 13 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/backup/WorkspaceBackup.kt` | 0 | 1 | 4 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/data/AppContainer.kt` | 0 | 8 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/data/AppDatabase.kt` | 0 | 3 | 2 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/data/Converters.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/data/Item.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/data/ItemDao.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/data/ItemWithTaskCounts.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/data/RestoreJournal.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/data/Task.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/data/TaskDao.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/data/TaskRepository.kt` | 0 | 1 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/data/TaskWithItemName.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/MainActivity.kt` | 0 | 13 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/notification/BootReceiver.kt` | 0 | 3 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/notification/ReminderActivity.kt` | 0 | 9 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/notification/ReminderReceiver.kt` | 0 | 4 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/notification/ReminderScheduler.kt` | 0 | 0 | 2 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/settings/SettingsRepository.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/TaskProApp.kt` | 0 | 2 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/AppViewModelProvider.kt` | 0 | 9 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/components/DragDropList.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/components/DraggableItem.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/components/GradientBackground.kt` | 0 | 2 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/components/NameDialog.kt` | 0 | 1 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/components/TaskCard.kt` | 0 | 8 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/navigation/NavGraph.kt` | 0 | 11 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/navigation/Screen.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/home/HomeScreen.kt` | 0 | 12 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/home/HomeViewModel.kt` | 0 | 3 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/item/AddTaskSheet.kt` | 0 | 3 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/item/ItemDetailScreen.kt` | 0 | 13 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/item/ItemDetailViewModel.kt` | 0 | 4 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/item/ItemStatusListScreen.kt` | 0 | 6 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/item/ItemStatusListViewModel.kt` | 0 | 4 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/reminder/ReminderPopupScreen.kt` | 0 | 3 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/search/SearchScreen.kt` | 0 | 8 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/search/SearchViewModel.kt` | 0 | 2 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/settings/BackupSection.kt` | 2 | 6 | 1 | Change approved display text only; retain frozen contracts. |
| `app/src/main/java/com/taskpro/app/ui/screens/settings/SettingsScreen.kt` | 0 | 6 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/settings/SettingsViewModel.kt` | 0 | 10 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultComponents.kt` | 0 | 7 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultEditors.kt` | 0 | 2 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultLogin.kt` | 0 | 4 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultMasterSetup.kt` | 1 | 2 | 1 | Change approved display text only; retain frozen contracts. |
| `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultScreen.kt` | 0 | 3 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/vault/VaultUnlockScreen.kt` | 0 | 5 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/WorkspaceBackdrop.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/screens/WorkspaceScreen.kt` | 0 | 2 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/theme/Color.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/theme/StatusColors.kt` | 0 | 1 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/theme/Theme.kt` | 0 | 2 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/ui/theme/Type.kt` | 0 | 1 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/util/DateFormat.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/util/ShareUtil.kt` | 0 | 3 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/util/TextDirection.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/vault/VaultClipboard.kt` | 1 | 0 | 3 | Change approved display text only; retain frozen contracts. |
| `app/src/main/java/com/taskpro/app/vault/VaultCrypto.kt` | 0 | 1 | 2 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/vault/VaultDeviceUnlock.kt` | 0 | 0 | 8 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/vault/VaultModels.kt` | 0 | 1 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/vault/VaultRepository.kt` | 0 | 3 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/java/com/taskpro/app/vault/VaultStore.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/res/values-fa/recovery_strings.xml` | 1 | 0 | 0 | Review display candidates; retain internal implementation references. |
| `app/src/main/res/values-fa/strings.xml` | 2 | 0 | 0 | Review display candidates; retain internal implementation references. |
| `app/src/main/res/values-night/themes.xml` | 0 | 1 | 0 | No change required for visible rebranding. |
| `app/src/main/res/values/recovery_strings.xml` | 2 | 0 | 0 | Review display candidates; retain internal implementation references. |
| `app/src/main/res/values/strings.xml` | 7 | 0 | 0 | Review display candidates; retain internal implementation references. |
| `app/src/main/res/values/themes.xml` | 0 | 1 | 0 | No change required for visible rebranding. |
| `app/src/main/res/values/vault_strings.xml` | 1 | 0 | 0 | Review display candidates; retain internal implementation references. |
| `app/src/main/res/xml/backup_rules.xml` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/main/res/xml/data_extraction_rules.xml` | 0 | 0 | 2 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/test/java/com/taskpro/app/backup/BackupFormatTest.kt` | 0 | 6 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/test/java/com/taskpro/app/backup/BackupRepositoryTest.kt` | 0 | 10 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/test/java/com/taskpro/app/ui/screens/reminder/ReminderPopupScreenTest.kt` | 0 | 5 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/test/java/com/taskpro/app/vault/VaultCryptoTest.kt` | 0 | 0 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/test/java/com/taskpro/app/vault/VaultDeviceUnlockTest.kt` | 0 | 1 | 2 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/test/java/com/taskpro/app/vault/VaultExternalIntentTest.kt` | 0 | 6 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/test/java/com/taskpro/app/vault/VaultFirstRunTest.kt` | 0 | 9 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/test/java/com/taskpro/app/vault/VaultIntegrationTest.kt` | 0 | 4 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/test/java/com/taskpro/app/vault/VaultScreenTest.kt` | 2 | 17 | 1 | Change approved display text only; retain frozen contracts. |
| `app/src/test/java/com/taskpro/app/vault/VaultUnlockUiTest.kt` | 0 | 6 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `app/src/test/java/com/taskpro/app/vault/WorkspaceNavigationTest.kt` | 0 | 7 | 1 | No branding-driven change; preserve contracts and implementation references. |
| `BACKUP.md` | 4 | 0 | 1 | Change approved display text only; retain frozen contracts. |
| `README.md` | 3 | 2 | 0 | Review display candidates; retain internal implementation references. |
| `SECURE_VAULT.md` | 2 | 0 | 2 | Change approved display text only; retain frozen contracts. |
| `settings.gradle.kts` | 0 | 1 | 0 | No change required for visible rebranding. |

## Acceptance checks for a later approved rebrand

Verify all approved English/Persian display surfaces, format placeholders, screen-reader labels, mixed-direction strings, large fonts, compact layouts and dark/light themes. Open pre-rebrand task data and encrypted vault/backups unchanged; verify a same-certificate in-place update. Update only display-sensitive test expectations; keep cryptographic and legacy-format fixtures unchanged. These rebrand checks are proposed, not executed in Phase 02.
