# Reference-to-repository gap matrix

Design analysis only. Source IDs E01-E25 resolve to exact paths/entry points in
[REFERENCE_ANALYSIS.md](REFERENCE_ANALYSIS.md#verified-repository-evidence).
Line numbers are from the current dirty working tree. None of the proposed gaps
were fixed by this documentation phase.

Classification legend (each row has exactly one):

- EP = Existing capability to preserve.
- ER = Existing capability to redesign.
- UI = New UI-only capability; still needs design/interaction approval, not a storage change.
- FN = New functional capability requiring separate approval; do not smuggle it into styling.

P0 = security/data-preservation or implementation-blocking decision; P1 = core
redesign scope after approval; P2 = optional enhancement; Deferred = excluded from
the redesign-only implementation. Priority is not authorization. D numbers link
to [OPEN_DECISIONS.md](OPEN_DECISIONS.md).

| Ref | Reference feature | Verified existing implementation | Existing source location | Gap/discrepancy | Proposed design solution | Security/data impact | Priority | Classification |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 00 | Brand + version | Vaurivo resources and runtime version labels | E03 WorkspaceScreen.kt:33; E25; E15 VaultLogin.kt:80 | Mock says TaskPro/1.0.0 and uses unapproved symbol | Vaurivo, runtime version, existing artwork until D11 | No identifier/signing change | P1 | ER |
| 00 | Module selection | Real Tasks/Vault callbacks | E03 WorkspaceScreen.kt:43 | Reference side-by-side; current stacked rows | Responsive C01 cards; retain callbacks | No auth bypass on Vault selection | P1 | ER |
| 00 | First-run / remember choice | No persisted launch/module choice | E01 NavGraph.kt:56; E20 SettingsRepository.kt:31 | Every current launch starts selector | D01 first-run/return policy, missing-key/backup semantics before persistence | New preference/backup decision; never store credentials | P0 decision | FN |
| 00 | Decorative rings/pattern | Existing gradient/pattern entrance | E03 WorkspaceScreen.kt:36; E22 WorkspaceBackdrop.kt:19 | Reference more restrained; no approved permanent mark | Small optional static/short entrance within 00 only | No new asset/runtime dependency | P2 | ER |
| 01 | Locked layout and fallback methods | Dedicated unlock screen | E14 VaultUnlockScreen.kt:159, :197 | Hardcoded dark even in light app; small labels | Shared tokens, centered lock target, wrapping/stacked fallback buttons | Preserve secure window/session | P1 | ER |
| 01 | Biometric unlock | Strong CryptoObject flow, one initial prompt, cancellation | E14 VaultUnlockScreen.kt:99, :142 | Decorative target could imply sensor even unconfigured | Truthful available/setup/unavailable states, OS prompt unchanged | Keep generation/binding/key requirements | P0 | EP |
| 01 | PIN fallback/cooldown | Optional local PIN, persistent failed-attempt cooldown | E14 VaultUnlockScreen.kt:218; E16 VaultDeviceUnlock.kt:116 | Use PIN currently also leads to setup when absent | Set up PIN vs Use PIN, full error/cooldown labels | Same ASCII policy/Keystore/DU codes; no new fallback | P0 | ER |
| 01 | Master/setup/recovery | Explicit create master, recovery confirmation and offline reset | E15 VaultMasterSetup.kt:23; VaultLogin.kt:89, :122 | Reference lacks fresh setup/recovery safety | Keep complete flow in secure sheets/pages, distinguish create-new from recover | Never replace existing encrypted Vault without verified backup | P0 | EP |
| 01 | Locked item count | Current locked UI has no decrypted count | E09 VaultScreen.kt:99; E14 VaultUnlockScreen.kt:173 | Reference exposes 38 protected items | Omit counts/names on every locked surface | Prevent metadata leak, do not decrypt for layout | P0 | EP |
| 01 | Auto-lock wording | 120-s default foreground, 60-s away | E16 VaultRepository.kt:15, :55; E13 VaultModels.kt:37 | Mock says 1 minute without distinction | Accurate foreground vs away explanation | No timeout/crypto-policy change | P0 | ER |
| 02 | Date/greeting header | No dashboard greeting | E03 WorkspaceScreen.kt:33 | New presentation from device clock | Localized date/time greeting, no name/location assumptions | No extra permission or persistence | P1 | UI |
| 02 | Home dashboard destination | workspace=selector; home=Tasks groups | E01 NavGraph.kt:56, :64; E04 HomeScreen.kt:88 | Mock Home is not implemented HomeScreen | D01 evolve workspace presentation, retain Tasks group route | No lost group routes or accidental data reset | P0 decision | FN |
| 02 | Cross-module search | Task search plus separate unlocked Vault search | E08 SearchViewModel.kt:24; E09 VaultScreen.kt:165 | No combined provider/index or safe results surface | Task search baseline; D04 approve federated secure flow separately | No locked results, persistent plaintext index or hidden unlock | P0 decision | FN |
| 02 | Today's progress | Existing statuses/dueAt, no aggregate dashboard or completedAt | E06 Task.kt; E04 HomeViewModel.kt:16 | Mock progress scope/denominator undefined | D03 Scheduled today based on current dueAt/local-day/status | New read projection, no guessed completion history | P1 approval | FN |
| 02 | Upcoming/overdue | Dates/status/reminder queries exist; no Home feed | E06 TaskDao.kt:20, :49; TaskRepository.kt | New aggregate/order/empty-state semantics | D03 deterministic real projections, future query tests | Do not reschedule or rewrite dates just to display | P1 approval | FN |
| 02 | Vault module preview | Session state exists; Home currently no Vault records | E01 NavGraph.kt:35; E16 VaultState:11 | Mock has item/group counts while privacy undefined | Generic Open/Locked/Create state; no Home record metadata | Preserve away deadline and Tasks screenshot policy | P0 | EP |
| 02 | Task module counts | Per-group live pending/completed/snoozed counts | E04 HomeViewModel.kt:18; E06 TaskRepository.kt:19 | No independent notes-record count | Truthful existing task/group counts; D03 for extra projections | Never count task notes as independent note entities | P1 | ER |
| 02/03/04 | Bottom navigation | workspace/home/vault tabs and state switching | E01 NavGraph.kt:37, :44 | Labels differ from Home concept; descendants not consistently selected | C12 labels Home/Tasks/Vault, route-to-tab ancestry after D01 | Preserve routes and session lifecycle; new label resources only | P1 | ER |
| 03 | Task group discovery | Groups/create/counts/share/delete exist | E04 HomeScreen.kt:88, :192 | Reference jumps into aggregate task feed | Keep accessible Groups area/root; no flattening that hides ownership | IDs/FK relationships intact | P0 | EP |
| 03 | Group rename | Repository/ViewModel/dialog exist; no visible trigger found | E04 HomeScreen.kt:99, :234; HomeViewModel.kt:29 | Dead-end UI, not verified usable rename | Reviewed menu entry calling existing rename callback | No schema change; confirm design D09 | P2 | UI |
| 03 | Date selector / date filtering | Editor date/time; no main date strip/feed | E05 AddTaskSheet.kt:132; E06 Task.kt | Scope and undated behavior absent | D05 define local dueAt filter + All dates/Undated | New query semantics; keep manual order untouched | P1 approval | FN |
| 03 | All / Notes / Reminders chips | Three statuses; notes and dueAt fields only | E06 Task.kt; E05 ItemDetailScreen.kt:167 | Not existing task types or filters | D05 status/With notes/Has reminder projections, no model enum | Do not migrate notes into records or discard undated tasks | P1 approval | FN |
| 03 | Compact completion/task row | Existing status actions and card | E07 TaskCard.kt:50; E05 ItemDetailScreen.kt:203 | Reference compact checkbox, current action row | C06 independent completion/edit controls with same callbacks | Avoid duplicate toggle/edit/delete events | P1 | ER |
| 03 | Task edit | Pending card click reuses AddTaskSheet and updateTask | E05 ItemDetailScreen.kt:257; ItemDetailViewModel.kt:48 | Reference does not show edit workflow | Preserve prefill/same-ID update/cancel; no toggle on body tap | No duplicate task or changed status/position | P0 | EP |
| 03 | Completed/Snoozed workflows | Reopen/resume/complete/delete in status lists | E05 ItemStatusListScreen.kt:92 | Not pictured beyond one completed row | Keep status lists and commands discoverable | Editing them is not currently wired; separate UI extension | P0 | EP |
| 03 | Drag/manual priority | Long-press position reorder in pending group | E07 DragDropList.kt:102; E05 ItemDetailScreen.kt:99 | Date sorting could overwrite manual order | Keep manual group view and accessible reorder commands | Never persist a filtered/sorted projection as full order | P0 | EP |
| 03 | Weekly recurrence / priority labels | No recurrence or explicit priority enum | E06 Task.kt | New persisted behavior not present | D07a separate specification; only manual position now | Schema/backup/reminder migration risk | Deferred | FN |
| 03 | Subtasks, tags, completion time | Task has no children, tags or completedAt | E06 Task.kt | Mock 2/5, tag and Done HH:mm unsupported | D07b/D07c; omit unsupported progress/time badges | Requires new model/history semantics and compatibility work | Deferred | FN |
| 03 | Vault links | No task record-link field/navigation | E06 Task.kt; E01 Screen.kt:4 | Mock linked-account shortcut unimplemented | D07d explicit secure link model later | No locked title leak or route-based auth bypass | Deferred | FN |
| 03 | New task action and notes/reminders | Shared add/edit sheet, notes, dueAt, alarms | E05 AddTaskSheet.kt:52; E23 ReminderScheduler.kt | Mock hides group selection and notes editing | Labelled action, existing group context, responsive editor | Retain timestamp/scheduler/status contracts | P1 | ER |
| 04 | Folder cards/nesting/colors | Actual nested folders, direct counts and tree | E09 VaultScreen.kt:154, :254; E10 CategoryEditor:104 | Current rows rather than reference grid | Responsive C07 grid + visible parent/up/category browser | IDs/colors/order unchanged; show direct/descendant meaning | P1 | ER |
| 04 | Category CRUD/reorder/starters | Rename/move/icons/RGB/delete/collapse/starters | E10 VaultScreen.kt:346, :385 | Most absent from reference | Folder menu and category browser retain all actions | Delete category keeps records uncategorized | P0 | EP |
| 04 | Total groups/items | Live doc.categories/doc.records sizes | E09 VaultScreen.kt:220 | Reference uses unsupported health-card space | Compact real totals, no faux security score | Visible only inside authenticated Vault | P1 | ER |
| 04 | Search/favorites/tags/sort | In-memory public-field matching and four sort modes | E09 VaultScreen.kt:165, :233; E13 VaultModels.kt:29 | Reference omits tags and several sort modes | Top C04; labelled filter/sort menu | No password/standard-note/secret-custom indexing | P0 | EP |
| 04 | Security health 82, reused/old/breach metrics | Only per-input local zxcvbn estimate exists | E12 VaultEditors.kt:43; E13 PasswordStrength:142 | No health engine/password-change history/breach data | D06 separate model/security proposal; omit card | No fake score or remote secret transmission | Deferred | FN |
| 04 | Recently used | modifiedAt exists; access history does not | E13 VaultModels.kt:22; E09 VaultScreen.kt:169 | last-used is not modifiedAt | Favorites or Recently modified from existing state; D06 for access tracking | No synthetic usage timestamps or covert tracking | P2 | FN |
| 04 | Add Item / Lock | Actual labelled add and manual lock | E09 VaultScreen.kt:92, :250 | Mock floating action overlaps row | C11 extended action with reserved bottom space; always visible Lock | No unsaved/session bypass | P1 | ER |
| 05 | Field-card detail | Populated standard/custom values and timestamps | E11 VaultScreen.kt:399 | Current plain columns/dividers; reference omits fields | C09 field frames, retain separate email/username/phone/reference/notes/tags | No data loss from visual simplification | P1 | ER |
| 05 | Mask/reveal/copy | Per-field mask, typed sensitivity and owned clipboard | E11 VaultScreen.kt:415; E17 VaultClipboard.kt:21 | Reference footer overpromises clearing | Best-effort privacy text; independent actions, no secret announcements | Preserve token/content match and secure-window behavior | P0 | EP |
| 05 | Website opening | Explicit non-sensitive URL whitelist | E11 VaultScreen.kt:424; E24 VaultExternalIntentTest.kt:52 | Redesign/autolink could regress fix | Keep predicate exactly, no auto-link secret/note text | Prevent external intent credential leakage | P0 | EP |
| 05 | Favorite in detail toolbar | Favorite edit in list exists, not detail header | E09 VaultScreen.kt:281; E11 RecordDetails:403 | New placement of current operation | UI-only callback placement, no new record property | Preserve ID/modifiedAt operation semantics | P2 | UI |
| 05 | Edit/duplicate/delete | Current details callbacks and confirmation | E11 VaultScreen.kt:406; E09 VaultScreen.kt:316, :322 | Mock overflow hides duplicate/delete | Clear menu with safe confirmation; existing same-ID edit | Duplicate intentionally new ID; never on ordinary save | P0 | EP |
| 05 | 2FA code/timer | No TOTP type, seed handling or generator | E13 VaultModels.kt:7, :89; E12 RecordEditor:36 | Reference suggests live OTP | D07e separate design/security approval; omit region | New secret lifecycle/backup/export/clock threat model | Deferred | FN |
| 06 | Sheet-style editor | Secure full-screen generic RecordEditor dialog | E12 VaultEditors.kt:36, :47 | Reference sheet only shows subset of fields | Expanding secure modal, full field/custom access, IME-safe Save | Keep FLAG_SECURE, no draft persistence | P1 | ER |
| 06 | Login/Card/Email/Note selector | Generic VaultRecord; only custom fields have types | E13 VaultModels.kt:7, :22 | No record-type discriminator | Generic form first; D08 optional non-destructive presets; Card deferred | No incompatible enum, dropped hidden fields or payment secrets | P2 approval | FN |
| 06 | Password generator | SecureRandom-backed existing length/classes tool | E13 VaultComponents.kt:152; VaultCrypto.generate | Reference inline candidate and lower default differ | C13 reuse 12-128/default24; reviewed explicit Use password | Do not silently replace existing credential | P1 | ER |
| 06 | Save/validation/category/tags/custom fields | Actual document validator and same-ID repository edit | E12 VaultEditors.kt:50, :62, :80; E09 VaultScreen.kt:316 | Reference does not show all existing capabilities | Progressive disclosure, full-field preservation and scoped error labels | Same encrypted transactional write and IDs | P0 | EP |
| 06 | Dirty-form discard protection | Dismiss currently discards with no prompt | E12 VaultEditors.kt:47; E05 AddTaskSheet.kt:67 | Back/swipe/tab can lose draft | D09 transient confirmation; session lock still clears immediately | No autosave or plaintext saved-state persistence | P1 | UI |
| All | Light theme / Persian / large type | App themes + Vazirmatn + localized strings; unlock hardcoded dark | E14; E21; E25 | Only dark refs; small fixed/low contrast tools | Shared semantic tokens, reflow, natural EN/FA and larger targets | Keep text bytes/crypto inputs exact | P0/P1 | ER |
| All | Settings/About/background | Working preferences/About screen | E18 SettingsScreen.kt:57; E20 | Not pictured; reference flat canvas vs stored backgrounds | Keep discoverable Settings and D12 visual coexistence policy | Persisted keys/enums/last backup unchanged | P0 | EP |
| All | Unified backup/legacy recovery | Full/tasks/vault encrypted export; selective replace; v1 reader | E18 BackupSection.kt:50, :115; E19 BackupRepository.kt:40, :79 | Not pictured; no safe merge exists | Keep all scopes, preview, confirm, external save/share and retry journal | Never overwrite unselected module or change protocol/AAD | P0 | EP |
| All | Recovery key management/master change | Unlocked security settings, reauthentication, rotation | E09 VaultScreen.kt:336; E15; E16 setupRecovery:225 | Reference lacks entry points | Vault security menu, not dashboard recovery buttons | No recovery bypass; key/password remains unrecoverable if both lost | P0 | EP |
| All | Reminders/task sharing | Actual popup/permissions/rearm and text export | E23; E04 HomeScreen.kt:196; E05 ItemDetailScreen.kt:128 | Missing from references | Preserve actions/entry points and primary/secondary emphasis | Notification/action IDs and scheduling unchanged | P0 | EP |

## Existing capabilities absent or incomplete in the references

Implementation acceptance must explicitly retain the following, not merely match
the screenshots: group create/delete/share and accessible counts; pending task
editing, drag ordering, completed/snoozed/reopen/resume paths; task notes and title/
note search; date/time selection/clearing and full-screen reminder actions; locale
text direction; all theme/background/About options; every category hierarchy/
color/icon/reorder/starter function; standard/custom fields, tags, favorites, all
sort modes, duplicate and confirmed delete; fresh setup and interrupted recovery-key
confirmation; master password/recovery/empty-new-vault preservation; on-device unlock
setup/cooldown/errors; selective/legacy backups and durable interrupted-restore recovery.

No currently unreachable rename handler, proposed global feed, metrics engine,
record template or TOTP generator should be labelled as an existing working feature.
These distinctions must remain visible in later implementation tickets and QA.
