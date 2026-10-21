# Phase 05D-3 Vault Editor Visual Review

Source: `docs/phase04a/references/02-vault-details-editor.png`, Screen 06 (rightmost).
All actual images are native Android Compose renderings captured by Robolectric with synthetic Vault data. They are not emulator or physical-device captures. No real credentials appear; secret inputs and generator candidates remain masked.

| Capture | Purpose |
| --- | --- |
| `add-dark-en.png` | New item, dark English |
| `add-dark-fa.png` | New item, dark Persian RTL |
| `add-large-fa.png` | 320 dp width, 1.6x Persian font scale |
| `add-light-en.png` | New item, light English |
| `edit-dark-en.png` | Existing item with prefilled synthetic values |
| `generator-dark-en.png` | Expanded inline password generator |
| `editor-reference-comparison.png` | Reference Screen 06 beside actual Add Item |
| `generator-reference-comparison.png` | Reference Screen 06 beside actual generator |

The implemented form preserves the reference's dark palette, clear title, group selection, secure password input, generator, and persistent green Save action. It deliberately differs in two ways: it expands to a full-height scrollable editor to retain all supported fields and keep Save reachable on compact screens, and it omits the pictured record-type tabs because the current Vault model has no record-type discriminator. The generator's candidate remains masked and requires an explicit Use Password action; the reference's visible sample password is not reproduced.
