# Navigation, accessibility and interaction contracts

Proposed behavior for review; no navigation code changes in Phase 04A.
See [evidence E01-E25](REFERENCE_ANALYSIS.md#verified-repository-evidence),
[screen specifications](SCREEN_SPECIFICATIONS.md) and [approval gates](OPEN_DECISIONS.md).

## Existing destinations versus proposed labels

| Existing destination | Current responsibility | Proposed presentation / boundary |
| --- | --- | --- |
| workspace | WorkspaceScreen chooser, root start | Home dashboard 02 after approved stage C; 00 as first-run/explicit chooser per D01. Do not rename persisted/external route tokens. |
| home | HomeScreen task groups | Tasks tab root. Internal name is not a rebranding defect. |
| item/{itemId} | Group pending tasks/add/edit/share/status links | Tasks group detail; existing Long group ID remains the route argument. |
| item/{itemId}/status/{status} | Completed/Snoozed lists | Tasks descendants; preserve enum values and reopen/resume/delete. |
| search | Existing task title/notes search | Tasks search or Home task-search entry; not global Vault search. |
| vault | Locked/setup/unlocked Vault with local states | Vault tab root, retaining session gate; groups/details/editors are internal protected states, not public deep links. |
| settings | Theme/background/About + backup workflow | Shared Settings and Backup & Restore; no hidden Vault authentication bypass. |
| ReminderActivity | Separate lock-screen reminder | Independent existing reminder UX; not a Vault destination. |

Routing labels Home/Tasks/Vault translate to خانه / کارها / گاوصندوق (proposed
Home translation; current workspace label is فضاهای کاری). Product remains Vaurivo.
Do not rename com.taskpro.app, application classes, DB, enum, AAD, key aliases,
backup formats, notification identity or signing configuration for label consistency.

## Proposed route/flow model

```text
App launch -> existing restore readiness gate
  -> pending recovery: preparing / retry (no bypass to Home)
  -> approved launch policy D01:
       first use -> Choose section 00 -> Tasks or Vault
       return -> Home 02 (or explicitly remembered module)
Home 02 <-> Tasks root (existing home) <-> Vault root (existing vault)
Home/Settings -> explicit Choose section 00 -> selected module
Tasks -> group -> pending task -> add/edit task sheet -> same group
Tasks -> group -> Completed / Snoozed -> existing status actions
Tasks/Home task search -> result -> owning group
Vault -> existence/session gate
  -> not configured -> master setup -> save recovery key confirmation -> dashboard
  -> locked -> biometric / PIN / master -> pending recovery step or dashboard
  -> dashboard -> folder -> subfolder -> item detail -> edit/duplicate/delete
  -> Vault security -> timeout / master change / authenticated recovery management
  -> locked Unlock settings -> existing-master auth -> device PIN/biometric setup
  -> Forgot password -> saved recovery key + new master -> locked -> unlock/new key
Settings -> Backup & Restore -> scope/password/document picker/export/share
Settings -> Restore -> decrypt/validate -> metadata/module choice -> confirm replace
  -> existing DB transaction + durable journal -> ready / retry recovery
```

This is an information-architecture outline, not a recommendation for a new router,
module framework or persistent route enum. Preserve NavGraph and existing callbacks.
The Home dashboard and launch policy are approval-gated additions, not already wired.
Future modules plug into real workspace destinations; no blank placeholders or
unused tabs. Never derive an authentication decision from selected tab or saved route.

## Back stack and tab rules

1. Existing singleTop/saveState/restoreState switching is the starting contract.
   Keep one root per tab; restoring a tab must not duplicate item or authentication screens.
2. Highlight Tasks for group/status/task-search descendants. Highlight Vault for
   all protected child states. Settings is shared, so retain the origin for Back.
3. Back priority: IME consumes its normal dismissal first; then child dialog/menu/
   generator; dirty-editor confirmation if approved; detail; folder parent; tab root;
   Android root exit policy. A modal cannot trap Back indefinitely except existing
   non-dismissible mandatory recovery/save-in-progress safety gates.
4. Switching tabs with a dirty record prompts only while a valid unlocked session
   exists (D09). Manual lock/timeout wins and clears it immediately. Tasks must
   remain reachable; authentication panels never become a global app lock.
5. Reopening a valid same-session list restores scroll/selection. Query, category,
   selected record and reveal state are scoped to the current Vault generation.
   The current code uses local remember, so any expanded same-session restoration
   is future UI work with explicit tests, not an existing guarantee.
6. From explicitly opened chooser return to invoker; first-run root chooser does
   not fabricate a previous Home entry. Remembered Vault opens setup/locked state.
7. No secret-bearing Vault editor/detail screen is registered as an exported deep
   link. Current manifest has the launcher and reminder components, not Vault links.
   Any future link may identify an opaque destination only, must authenticate and
   re-resolve it in current authorized state, and needs separate approval.

## State restoration and session boundaries

| State | Same process / configuration | Process death / restore |
| --- | --- | --- |
| Tasks groups/status data | Read from Room; stable IDs, manual order and notes preserved. | Reload from existing database/migrations; do not create demo/default replacement records. |
| Non-secret scroll/tab state | Navigation-owned state is appropriate where already supported. | Re-resolve IDs, handle missing records; never assume a saved selected route proves authorization. |
| Task unsaved form | Existing remember-based draft may be lost; any improved retention is UI proposal. | No auto-save or duplicate insertion. Explicitly restart edit from current record. |
| Vault document/key | Existing app-scoped repository only while authorized. | Keys not persisted; locked/setup gate, with no saved decrypted screen replay. |
| Secret input/reveal/editor | No saveable/bundle persistence; dispose on generation change/stop as applicable. Rotation policy must not weaken locking. | Clear; never restore plaintext input from Android state. |
| Biometric callback | Existing CryptoObject operation, lifecycle check and generation/binding validation. | Discard operation; never count an old success callback as an unlock. |
| Backup/restore journal | Busy blocks duplicate operations; follow actual commit state. | Complete existing durable preference/reminder journal before exposing normal UI; do not claim import cancelled after commit. |
| Remember-choice/onboarding | Not currently stored; D01 required. | If approved later, define safe default/missing-key/backup behavior first; never include credentials. |

Foreground Vault inactivity is stored inside encrypted document, default 120 s with
supported values 30/60/120/300/600. Leaving Vault or backgrounding starts the separate
60-second away policy; re-entry checks its elapsed deadline before preserving the
session. Home/Tasks interaction must not mark Vault visible or extend that deadline.
Manual lock and process death lock immediately. Existing JVM strings cannot be
guaranteed wiped; retain honest platform limitations rather than promising perfect erasure.

The root FLAG_SECURE lifecycle must remain active for decrypted content and secure
dialogs, including future sheet windows. On disposal, clear owned clipboard only
when still owned/unchanged and return Tasks to its current non-secure window state.
Recommended Home never hosts decrypted metadata. Combined search would require
a separate secure-window/data-flow review before implementation (D04).

## Supporting flows that cannot disappear

- Global Settings: SYSTEM/LIGHT/DARK, PLAIN/GRADIENT/AURORA, About author/company
  and runtime version. Keep stored values; D12 decides visual coexistence.
- Vault security: inactivity selector, current-authenticated master change, recovery
  view/rotation. Recovery management stays out of dashboard actions.
- Device unlock: on-device PIN/biometric profile setup from existing master;
  not included in portable backups, invalidated by credential binding changes.
- Backup: FULL/TASKS/VAULT scopes, external Android picker, backup password,
  last successful date, share and clear errors. Can export encrypted Vault while locked.
- Restore: available-module preview, separate backup/Vault passwords or supported
  recovery key, explicit replace-selected warning; no merge. Unselected modules
  never erased. Keep legacy-v1 checksum-only disclosure and v2 encryption distinction.
- Existing-vault master creation: verified external preservation backup, then
  explicitly new empty Vault; old password/key still required for old data.
- Task sharing/export, group deletion warnings and full-screen reminders remain
  accessible even though the reference does not picture them.

## Accessibility and localization acceptance

This is a compliance-oriented design target, not a claim of WCAG/TalkBack compliance
from static images. Android recommends 48-dp minimum touch targets and appropriate
labels/roles; use existing semantic APIs without accidentally merging child commands.
[Android accessible defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults),
[Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics).

| Concern | Proposed mandatory rule and future verification |
| --- | --- |
| TalkBack structure | Screen/section headings; logical title -> content -> action traversal; tab selected state; checkbox/favorite checked state; dialogs announce title and restore focus. Test with real TalkBack, not only semantic snapshots. |
| Independent controls | Row edit, completion, copy, reveal, menu and drag alternatives are separate accessible actions. Avoid merged descriptions containing sensitive values. |
| Secure fields | Masked by default; announce label/Hidden and commands, not value/length. Visual reveal does not automatically speak. Explicit read-aloud policy requires D13 and testing with blind users. Retain password semantics on secret inputs. |
| Feedback | Errors and copy success use short polite announcements without copied content. Do not announce every timer/progress tick. Color plus icon/text/state, never color alone. |
| Touch/focus | 48-dp targets, no overlap, 2-dp visible focus. Hardware Tab/Shift+Tab/Enter/Space/Escape follow visible order; keyboard commands retain focus instead of landing behind sheet. |
| Font/reflow | EN/FA at 1.0/1.3/1.5/2.0, widths 320/360/400/600/840/1000 dp and landscape short heights. Text containers grow; full commands visible; no font shrinking to fit. |
| Contrast | Use tested active text/control pairs in token document. Recheck selected states, custom accents and screenshot composites; completed text still readable. |
| RTL | Logical start/end, mirrored back/forward arrows, localized tab order; don't mirror lock/key/search/logos. Timeline order follows locale with explicit date labels, not assumed left-to-right weekdays. |
| Mixed direction | Titles/notes each use content direction; usernames/emails/URLs/passwords/keys remain exact LTR technical runs where applicable. Copy underlying exact value, not a directionally decorated display string. |
| Localization | Every new resource EN + FA, correct indexed placeholders/plurals and no hardcoded date/greeting/module text. Vaurivo remains Latin; use natural Persian labels, no fake transliteration. |
| Calendar/numbers | Locale-aware UI counts and device time; existing timestamp persistence unchanged. Jalali/calendar choice is D10, not implied by Persian resources. Never localize password/PIN bytes before crypto. |
| Insets/keyboard | Correct status/cutout/gesture/3-button/IME padding exactly once. Focused field and Save scroll into view; navigation cannot cover last item. Validate system font/display size combined. |
| Loading/offline | No content-placeholder secrets; truthful busy/empty/error states. The existing app remains offline; no remote logos, strength services or analytics. |

## Motion specification

Durations below are proposed at normal system animation scale. Motion is feedback,
not proof of authentication or security. Use Compose primitives already installed;
no animation library. Render content/actions without waiting for decorative entrance.

| Interaction | Normal proposal | Reduced motion / animations disabled |
| --- | --- | --- |
| Workspace entrance / 00 selection | 180-220 ms opacity with <=8 dp translation, standard ease-out; press tonal/ripple 100-120 ms. | Instant layout/state; no translation/scale. |
| Tab/navigation change | 180-220 ms restrained fade, no large horizontal slide or simulated parallax. | Instant or <=80 ms opacity only if user setting permits; scale 0 is instant. |
| Task completion | 120-180 ms check/tonal change; defer row movement until action committed, no celebration. | Instant check/status; stable focus after data update. |
| Folder opening | 160-180 ms content fade; selected path updates immediately; stable row/grid bounds. | Instant content. |
| Detail selection | 160 ms fade in allocated pane; no secret value morphing across cards. | Instant masked detail. |
| Sheet/dialog | 200-240 ms standard decelerating translation and scrim; no bounce; IME remains platform-managed. | Instant sheet and focus. |
| Password reveal | Instant value swap; no character-by-character reveal, animation or automatic reading. | Same instant explicit action. |
| Biometric target | Optional 1800-2400 ms low-opacity ring only while prompt is actionable; stop when background/busy/unavailable. | Static target; no pulsing, shaking or vibration-only error. |
| Error/press | Color/focus change and optional platform haptic respecting system preference; no mandatory shake. | Text/icon error and visible focus, no movement/haptics required. |
| Loading/progress | Indeterminate only for actual pending operation; determinate fraction only from approved data; no fake interpolation to success. | Static pending status and accessible completion announcement. |

Use non-spring ease-out for entry and standard fast-out/slow-in for state changes;
linear time only for a genuine progress/timer visualization. Respect system animator
duration scale and accessibility motion preferences; explicitly test scale 0 rather
than assuming every custom animation honors it. Do not add a persisted preference
without review. Existing Ctrl+F search, Ctrl+N record, Ctrl+L lock and Ctrl+Shift+H
workspace shortcuts stay scoped to Vault and must not submit forms accidentally.
