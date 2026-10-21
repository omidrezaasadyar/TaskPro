# Phase 05D-4 Authentication Visual Review

Reference: `docs/phase04a/references/01-launch-unlock-home-tasks.png`, Screen 01 (leftmost).

These are native Android Compose renderings captured with Robolectric using empty or synthetic Vault data. The first-run image renders the production `VaultLogin` composable directly; the other images use `VaultScreen`. No password, PIN, recovery key, or stored item appears in any capture. This is not physical-device or emulator biometric validation.

| File | State |
| --- | --- |
| `locked-dark-en.png` | Locked Vault, Dark English, no configured device unlock |
| `locked-light-fa.png` | Locked Vault, Light Persian RTL |
| `locked-large-fa.png` | Locked Vault, 320 dp and 1.6x Persian font scale |
| `master-dark-en.png` | Master-password entry sheet |
| `recovery-dark-en.png` | Recovery entry with no secret entered |
| `first-run-dark-en.png` | Empty first-run master-password setup form |
| `device-setup-light-en.png` | Existing device-unlock setup sheet, no secret entered |
| `unlock-reference-comparison.png` | Screen 01 beside the actual Dark English locked state |

The implementation retains the reference's quiet lock hierarchy, green security accent, and accessible fallback action. The reference's protected-item count is intentionally absent before authentication. Fingerprint and PIN actions appear only when genuinely available/configured; the Robolectric capture cannot demonstrate a real enrolled biometric prompt. The actual capture shows only the Vault module, not the application's outer bottom navigation. Light theme and Persian use the same layout with theme-specific colors and RTL alignment. Manual lock and timeout currently share the generic locked presentation because the existing repository state does not distinguish the lock reason.
