# Reusable Compose component specification

Proposed UI contracts only. Evidence IDs resolve to exact source references in
[REFERENCE_ANALYSIS.md](REFERENCE_ANALYSIS.md#verified-repository-evidence).
Use the supplied visual language and [tokens](DESIGN_TOKENS.md), not a replacement
application or design framework. The names below are proposed composable boundaries,
not new classes already present. Components receive display state and callbacks;
repositories own persistence, validation, encryption, reminders and authentication.

## Common contract

Each interactive component supports default, press, focus, hover where applicable,
selected, disabled, busy and error states. Color alone never indicates a state.
Maintain >=48-dp non-overlapping targets, wrapping text, localized labels and
content-based direction. State-bearing icons get localized action/state descriptions;
decorative icons get no duplicate announcement. Keep a coherent traversal order.
Prefer existing Material icons (including AutoMirrored directional variants),
Material interaction sources and bounded ripples. No custom icon drawing or Lottie.

All Vault descendants live under the existing session generation/secure-window
boundary. Never hand a decrypted model to an unrestricted global preview, cache,
SavedStateHandle, analytics or navigation argument. Use synthetic records in visual
tests. Recompose from immutable display models without altering stable entity IDs.

## C01 WorkspaceCard

- Purpose/anatomy: select Tasks or Vault; accent icon tile, name, optional safe
  subtitle, navigation/locked affordance. Treat the entire frame as one destination.
- Layout: 16-dp padding/radius, min height 128 dp; paired only when each card can
  accommodate labels, otherwise stacked. Small top icon tile does not enlarge headings.
- States: pressed tonal fill, focus outline, selected module marker; Vault locked,
  not-configured and unavailable subtitles contain no record metadata.
- Accessibility: button/destination semantics, single concise localized label;
  decorative arrow/lock does not create a second command.
- Boundary/reuse: extract presentation from E03 into proposed
  `ui/components/workspace/WorkspaceCard.kt`; reuse on 00/02. No repository reads,
  biometric calls or remembered-choice persistence inside it.

## C02 ProgressSummaryCard

- Purpose/anatomy: truthful scheduled-task summary; circular determinate progress,
  count sentence, scope/date caption and optional overdue/reminder badges.
- Layout: one repeated summary frame, 16-dp padding, ring 64 dp, text in flexible
  column; stack at large text. Badge line is a FlowRow, not compressed tiny chips.
- States: loading before computation, empty with no misleading 0/0 ring, populated,
  calculation failure. No random or demonstration score in production.
- Accessibility: announce one fraction and scope; hide duplicate ring semantics;
  do not animate/announce every recomposition.
- Boundary/reuse: proposed `ui/components/workspace/ProgressSummaryCard.kt`; accepts
  approved read-only summary state. No verified current backing; blocked by D03.
  Can later be reused on Tasks without inventing completion timestamps.

## C03 SectionHeader

- Purpose/anatomy: section label, optional count and one real action (New group,
  View all). Avoid all-caps lettering in Persian.
- Layout: start/end alignment with 8-dp gap, min 48-dp action height, wraps to a
  second row when needed. No section-sized floating card.
- States: action enabled/busy/disabled; loading count is absent, not zero.
- Accessibility: heading semantic; separate action label names its target.
- Boundary/reuse: proposed `ui/components/SectionHeader.kt`; reused for groups,
  favorites, upcoming and field sections; existing E09 labels can supply content.

## C04 SearchField

- Purpose/anatomy: leading search icon, query, clear command and optional scope
  selector; persistent label instead of placeholder-only meaning.
- Layout: min 56 dp, 12-dp radius, fills remaining row width. Move scope control
  below at compact/large-type widths; never cover query with icons.
- States: idle/focused/query/loading/results/none/error. Clear does not navigate.
  Vault results disappear on lock, including pending asynchronous work.
- Accessibility: editable role, localized label/scope and clear description; result
  count announced politely only after settled input, excluding secrets.
- Boundary/reuse: proposed `ui/components/SearchField.kt`; wrap E08 and E09 fields
  without combining their data sources. Federated/global search requires D04.

## C05 FilterChip

- Purpose/anatomy: short label, optional count, selected check. Appropriate for
  existing status scopes/favorites; not a decorative suggestion of missing features.
- Layout: min 48-dp hit height, min visual height 36 dp, wraps/scrolls horizontally
  without shrinking labels. Do not force all reference chips onto one line.
- States: selected/unselected/focus/press/busy; checked marker plus tonal fill.
- Accessibility: selected/checked state and role; no duplicate label on icon.
- Boundary/reuse: proposed `ui/components/FilterChip.kt`, or direct styled existing
  Material chip. Feed E09 favorite/tag/sort UI; new task filters are gated by D05.

## C06 TaskRow

- Purpose/anatomy: status/completion command, task title, optional notes, group
  identity, due/reminder line and independent overflow/status/reorder controls.
- Layout: unframed row/separator on task lists, 16-dp content padding; title takes
  available width and grows; controls move below at large type. Fixed icon targets
  cannot push text off-screen. Extended action area gets bottom-list clearance.
- States: pending/completed/snoozed, overdue display flag, drag, saving and error.
  Pending is not automatically overdue. No invented high/medium/low priority.
- Accessibility: read title/status/date/group once; custom reorder-up/down actions
  are proposed accessible equivalents to drag; keep separate edit and completion
  targets. Status commands never trigger row edit; drag release never opens editor.
- Boundary/reuse: adapt E07 TaskCard presentation in place, retain E05 click and
  drag contracts; use `TaskRepository.updateTask/setStatus/reorder`. References to
  future `TaskRow` may be a private composable, not a mandatory file/class rename.
  Preserve status-list reopen/resume/delete and task export even if not pictured.

## C07 VaultGroupCard

- Purpose/anatomy: folder/icon tile, group name, direct item count, subgroup count
  and independent menu for existing management actions.
- Layout: 16-dp padding/radius, 2 columns only when min 160-dp cards fit; otherwise
  one. Name may wrap; counts sit below and never overlap the folder icon.
- States: selected/pressed/focused, loading/error; newly saved groups appear from
  repository state immediately. Count subtitle explicitly means direct contents.
- Accessibility: group name and item/subgroup counts; menu description includes
  group name, not merely More. Expand/collapse state is exposed in tree variants.
- Boundary/reuse: extract E09 folder row into proposed
  `ui/components/vault/VaultGroupCard.kt`; reuse E10 tree/category controls. Preserve
  existing arbitrary colors/icons, parent UUIDs and direct-versus-descendant distinction.

## C08 VaultItemCard

- Purpose/anatomy: non-secret record title/category, generic category icon, favorite
  marker, modified timestamp where enabled; opens record detail.
- Layout: compact list row or flat repeated card, 12-16 dp padding; no email or
  username preview by default, no remote favicon/logo fetching.
- States: favorite/normal/selected/focused/busy; lock removes it, not just blurs it.
- Accessibility: label is title/category and selected/favorite state; copy password
  is not an unlabelled invisible shortcut on the whole card.
- Boundary/reuse: extract E09 ListItem presentation to proposed
  `ui/components/vault/VaultItemCard.kt`. Existing favorites/recently modified may
  populate 04; last-used ordering needs new data/approval (D06).

## C09 SecureFieldCard

- Purpose/anatomy: field label/icon, value or fixed mask, reveal if secret, copy,
  external-open only when explicitly safe. Notes can expand as unframed multiline
  content within their own field frame, not nested cards.
- Layout: 12-16 dp padding, min 72 dp height; value wraps or technical text scrolls
  horizontally without truncating the copy value; icon commands may use a second row.
- States: masked/revealed/copy-feedback/invalid-link/disabled/busy; reveal never
  changes the sensitive classification. Reset all reveal state on lock/session change.
- Accessibility: default secret node says field name and Hidden, never the raw
  value; show/hide/copy commands are independent. Explicit read-aloud policy for
  revealed secrets requires D13, not automatic secret announcements.
- Boundary/reuse: restyle E11 RecordDetails with a field display model carrying
  `sensitive` and `isUrl`. Preserve exactly the whitelist at VaultScreen.kt:424:
  URL type AND !sensitive AND validUrl. Use existing clipboard service, not system
  clipboard directly in each field. Reuse in standard and custom fields.

## C10 SecurityIndicator

- Purpose/anatomy: descriptive lock/safety state or local password estimate with
  icon, short text and optional 0-4 scale, not a fictional 0-100 health rating.
- Layout: compact row or field support line. Do not fill screen-04 health-card
  space with an unsupported numeric metric.
- States: locked/unlocked, estimate-pending/available/error. Label estimate as local
  guidance, never Secure, unbreached or guaranteed strong.
- Accessibility: label plus score, no color-only strength or repeated live updates;
  no secret value in description.
- Boundary/reuse: E13 PasswordStrength and E12 editor estimate; keep the existing
  estimator and truncation semantics, disclose its limits. Whole-vault scoring is
  D06 and absent from the redesign-only path.

## C11 PrimaryActionButton

- Purpose/anatomy: clear command, optional leading icon, pending indicator in a
  stable reserved slot; module accent with verified onAccent.
- Layout: min 56 dp, radius 12 dp, horizontal padding 20 dp; allow wrapping. Extended
  New task/Add Item is a command, not an icon-only plus. No text clipping in RTL.
- States: press/focus/hover/disabled/busy/error/retry. Busy prevents duplicate calls;
  the label does not jump or disappear under a spinner.
- Accessibility: real button role, busy and disabled reason, decorative leading icon.
- Boundary/reuse: proposed `ui/components/PrimaryActionButton.kt`; no save logic.
  Reuse for unlock/save/create; preserve reminder Done primary, Snooze secondary,
  Dismiss lower emphasis rather than recoloring all reminder actions identically.

## C12 BottomNavigationBar

- Purpose/anatomy: Home/Tasks/Vault with icon and label, selected indicator and a
  subtle Vault lock state. Map labels to stable routes, not string-based routing.
- Layout: 80-dp min content height plus system inset, 3 equal targets; visible in
  main workspaces, not duplicated inside modal editors/details. May become a rail
  at expanded widths after explicit layout QA.
- States: selected/focused/pressed, locked Vault; selected Tasks remains active in
  task group/detail/status/search descendants, not only at its root.
- Accessibility: tab role/selected state; traversal follows mirrored visual order
  in RTL without changing route identity. Announce Vault locked without record counts.
- Boundary/reuse: adapt E01 NavGraph Scaffold; visual wrapper proposed
  `ui/components/BottomNavigationBar.kt`. Preserve singleTop/popUpTo state contracts
  and fix descendant selection only as a reviewed UI change, not a new router.

## C13 PasswordGeneratorPanel

- Purpose/anatomy: length label plus slider/step control, lowercase/uppercase/digit/
  symbol toggles, generate/regenerate, controlled masked preview and explicit Use password.
- Layout: one in-editor tool frame, no nested cards; min 48-dp toggle targets,
  wrapping character controls and LTR technical value; reserve full-width apply command.
- States: default 24 chars, range 12-128, regenerating, no-character-class error,
  generated, applied/cancelled. Moving controls never silently overwrites an existing password.
- Accessibility: slider range/value plus increment/decrement, toggle checked states,
  preview masked in semantics; generate announces completion without the secret.
- Boundary/reuse: adapt E13 PasswordGenerator using VaultCrypto.generate unchanged.
  Inline preview/explicit application is proposed UI behavior (D09); no system-random
  replacement or persistent generator secret/preferences. Supports record creation/editing.

## C14 EmptyState

- Purpose/anatomy: small contextual existing icon, concise state, one applicable
  command; distinguish no data, empty folder and no search matches.
- Layout: unframed max 360-dp inner content, 24-dp padding; no giant illustration
  obscuring workflows. Unsupported features are omitted, not shown as fake empty data.
- States: truly empty, filtered-empty with Clear filters, not-configured Vault with
  Create master password. No count-zero before repository loading completes.
- Accessibility: heading plus action; decorative icon excluded, no repeated automatic focus.
- Boundary/reuse: proposed `ui/components/EmptyState.kt`; E04/E05/E09 existing empty
  messages can be reused. Keep both EN/FA translations and no destructive reset action.

## C15 ErrorState

- Purpose/anatomy: non-secret explanation, scoped Retry or safe alternative and
  preserved user-entered context where session permits.
- Layout: inline near field/operation; blocking panel only for existing restore
  readiness failure. Long DU-code/help strings wrap without hiding commands.
- States: field validation, incorrect password, device unavailable, cooldown,
  recoverable I/O, restore pending. A retry does not create a new vault implicitly.
- Accessibility: concise polite error announcement after action; no raw exception,
  stack trace, secret or success-style green icon.
- Boundary/reuse: proposed `ui/components/ErrorState.kt`; E14/E18 existing resource
  IDs and DU01-DU05 mappings remain authoritative, not replaced by guessed diagnoses.

## C16 LoadingState

- Purpose/anatomy: actual progress if measurable; otherwise neutral indicator and
  short localized status. Never a fictitious percentage or health score.
- Layout: stable placeholder space without credential skeletons; preserve button
  and form dimensions. Blocking startup gate uses full content area, not a fake Home.
- States: initial load, search settling, save, restore, authentication; busy prevents
  repeated submits. Cancellation follows the actual operation's guarantees.
- Accessibility: loading state, polite start/end announcement, no repeated animation
  announcements; static indicator when animations disabled.
- Boundary/reuse: proposed `ui/components/LoadingState.kt`; E02/E09/E18 state sources;
  all expensive work remains in existing coroutine/repository boundaries.

## C17 ConfirmationDialog

- Purpose/anatomy: explicit operation title, accurate consequence, affected modules/
  group name, cancel and unambiguous confirm command; restore includes metadata.
- Layout: max 560 dp, scrollable body, commands wrap/stack; no clipped long Persian
  warnings. Destructive confirm is not visually merged with cancel.
- States: ready/busy/error; dirty-edit discard differs from delete and from creating
  a new empty vault. Do not claim rollback after a committed restore journal exists.
- Accessibility: dialog title announced, focus contained/restored to invoker, Cancel
  first safe action; secure policy for every Vault/recovery/backup-password dialog.
- Boundary/reuse: reuse E09 deletion, E15 previous-vault and E18 selective replacement
  content with proposed `ui/components/ConfirmationDialog.kt`. Dirty-form confirmation
  is a UI-only proposal needing D09 approval; no automatic save on back.

## C18 SecureInputField

- Purpose/anatomy: persistent field label, value, lock icon where secret, reveal
  command and inline validation/support. Works for master/PIN/recovery/custom secret.
- Layout: min 56 dp and growing helper text, 12-dp corners; keyboard/IME type follows
  actual field semantics; multiline notes expand inside a scrollable form.
- States: empty/focused/filled/masked/revealed/error/disabled/busy. Focus alone never
  reveals; hide on session exit. Mask custom SECRET even if sensitive=false.
- Accessibility: retain password semantics when visually revealed; localized action
  descriptions and errors never contain value. Do not put entered data in test tags.
- Boundary/reuse: adapt E13 VaultInput without weakening autocorrect-off/password
  behavior or storing secrets in saveable state; keep CryptoObject outside UI field.
  Reuse E14/E15 setup/recovery/editor flows; no payment PIN/CVV/OTP templates.

## C19 Search/filter and category utility menus

The required primitives above must also compose into existing category browser,
tag/favorite/sort panel and custom-field rows, not replace them with inert icons.
Use E10/E13 `VaultChoice`, `VaultToggle`, categoryIcon and categoryPath, improving
their presentation/labels and target sizes only in the later approved pass.
Preserve searchable category hierarchy and all custom-field types; expose All
records and Uncategorized alongside folders. This utility composition adds no
model enum, category maximum, network service or plaintext search cache.

## Verification contract for every component

Proposed, not executed: semantic role/state tests, independent child-action click
tests, EN/FA golden screenshots in both themes, 320-dp/2x-font reflow, keyboard and
TalkBack traversal, no nested-card framing, stable pressed/loading dimensions,
contrast checks on final rendered colors. Secure components additionally require
lock/generation invalidation, screenshot-blocking device checks and redacted
semantics/previews. Preserve E24 security tests rather than replacing assertions
with screenshot-only approval.
