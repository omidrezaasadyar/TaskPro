# Screen-by-screen specification

Design proposal only. Screen numbers refer to the supplied images, not existing
route names. E01-E25 map to [repository evidence](REFERENCE_ANALYSIS.md#verified-repository-evidence).
All common geometry, typography, contrast, accessibility and motion contracts in
[DESIGN_TOKENS.md](DESIGN_TOKENS.md) and [NAVIGATION_UX.md](NAVIGATION_UX.md) apply.
Counts/names in mockups are illustrative; production never hardcodes them.

## 00 - Launch / Choose section

Reference: rightmost phone in image 1. Preserve the brand header, small runtime
version, centered existing security symbol, concise introduction and paired Tasks/
Vault cards. Use Vaurivo, not TaskPro. A permanent new logo is not approved. Keep
the symbol area restrained and collapsible on short screens; selection controls
must remain reachable by scrolling. Compact widths/large type stack the two cards.

**Current:** E03 supplies a selector on every cold start at route workspace, with
bottom navigation from E01. No first-launch flag or module preference exists (E20).

**Proposed routing, pending D01:** first launch shows 00 once; normal returning
launch opens Home 02; Home/Settings always expose Choose workspace to reopen 00.
The user may explicitly remember Tasks or Vault from the chooser. Remembering
Vault means opening the locked/setup destination, never automatic credential
storage, biometric enrollment or bypass. Tasks opens its existing group list.
If the user selects neither preference, the default return destination is Home.
Version is always runtime BuildConfig, not a manually entered mock version.

This behavior needs approved persistence semantics and backup/default policy;
therefore do not add even an onboarding preference during a styling-only pass.
Until D01 is approved, preserve current routing and omit a nonfunctional Remember
my choice control. The repeated-launch friction is an acknowledged current gap,
not silently fixed in this phase.

Back from explicitly reopened chooser returns to its invoker. Back from root
launch follows Android exit behavior without opening a second duplicate Home.
Future modules add genuine destinations to the same selection structure; no empty
fake modules or new storage enum are required for this two-module release.

Acceptance: brand visible, both destinations reachable in EN/FA and large type;
no mandatory repeated introduction under the approved returning-user policy;
no hidden Vault data. All selection actions use the real navigation callbacks.

## 01 - Vault Unlock

Reference: leftmost phone in image 1. Title/settings first; centered lock tile,
locked title and accurate timeout explanation; fingerprint target when usable;
PIN/master commands above persistent tabs. Preserve the reference's quiet layout,
not its protected-record count. Stack fallback buttons when their full labels do
not fit. Use the corresponding light tokens when the application is light; current
unlock hardcodes dark colors (E14), so light support is a proposed visual correction.

**Entry/state contract:**

| State | UI and safe action |
| --- | --- |
| Loading vault existence | Neutral loading state; do not flash setup or dashboard/counts. |
| No configured vault | Explicit Create master password form, confirm at least 12 characters, existing local estimate and recovery notice. Never ask for Existing master password first. |
| Newly created / pending saved key | Existing mandatory recovery key confirmation; no dashboard access before saved acknowledgment. Recover this step after interrupted setup (E15/E16). |
| Locked + configured strong biometric | Existing Android BiometricPrompt CryptoObject once on entry; target retries after cancellation. App rings are decorative feedback, not a simulated sensor or progress percentage. |
| Hardware present but not configured | Show Enable biometric unlock, routing to authenticated setup, not Touch sensor as though enrolled in the app. |
| Unavailable/no enrollment/invalidated key | Clear method-unavailable explanation and master password; PIN only when configured. Do not silently enable device credentials as a new fallback. |
| PIN configured | Numeric keypad and actual current cooldown; wrong PIN uses existing persistent five-attempt/30-second policy. Master/recovery remain reachable. |
| PIN not configured | Label Set up PIN rather than pretend a PIN already exists; authenticate existing master to configure. |
| Master mode | Masked password field, show/hide, Unlock, Forgot password, runtime version; failed attempt has generic non-secret message. |
| Recover | Existing saved key plus new/confirm master form; no email reset/backdoor. Successful recovery preserves records, consumes current key access and requires new recovery setup/backup. |
| Stored vault + create new master action | Separate dangerous-action explanation, verified external encrypted previous-vault backup, then new empty vault. This is not recovery and must not be primary. Preserve cancel/failure/stale-revision safety (E15). |

PIN/biometric setup must keep DU01-DU05 stage-specific errors without exceptions or
credentials in messages. Cancel/back closes panel or OS prompt; no looped prompt,
no deletion, no silent save. Home/Tasks remain accessible outside the in-destination
unlock panel. Recovery dialogs own secure focus and return to authentication.

No item count, group name, username, favorite, strength score or recently-used
content before authentication. Subtitle states 1 minute away separately from the
configured foreground inactivity period (30/60/120/300/600 seconds; default 120).
Do not change these values to match the image. Manual lock and timeout remove
decrypted UI and local editor state; pending authentication cannot reopen a stale
session. Process death requires a new authenticated unlock, not saved screen state.

Acceptance: every method has a truthful available/setup/unavailable state; full
fallback labels fit; no metadata leaks; unchanged master/recovery/device tests
plus device authentication/cancellation tests remain mandatory later.

## 02 - Home / Workspaces

Reference: second phone in image 1. Use date/greeting and Settings header, search,
one compact progress summary, unframed Workspaces section with two cards, Upcoming
section and Home/Tasks/Vault navigation. Date/greeting are local UI-only content;
use device time, no location/account permissions. Greetings cannot assume a user name.

**Critical source mismatch:** E04 HomeScreen is the existing Tasks group list and
must remain accessible under Tasks. Proposed Home dashboard belongs to the
workspace presentation/route, not an overwrite that strands item/{itemId} flows.

Search recommendation for the first redesign pass: label Search tasks and use
E08 title/notes results. A Vault search command opens Vault and authenticates as
needed, then uses its in-memory search. Do not advertise combined results before
D04 is approved and implemented. Home remains free of Vault record metadata even
when an unlocked session temporarily survives a tab switch. Show generic Locked /
Open Vault state, never the mock's counts or email previews. Home interaction must
not keep a hidden Vault session alive past the existing away deadline.

**Progress proposal D03:** define a local-day set of tasks whose existing dueAt
falls within [start of local day, start of next local day), across approved task
scope. Numerator is those currently COMPLETED, denominator all in that set.
Label Scheduled today, not Completed today: no completion timestamp exists.
Undated tasks remain accessible under Tasks; they are not silently scheduled today.
Snoozed tasks belong in the denominator if their current dueAt is today. Overdue
is non-completed with dueAt before now; next reminder includes eligible future
pending/snoozed reminders. Midnight/time-zone/status changes recompute summaries
without rewriting timestamps. Overdue and upcoming projections are not new task types.

No verified current dashboard query/state implements that aggregate. Until D03 is
approved, omit the summary and upcoming region, retain actual workspace navigation,
and do not render pretend counts. If approved, the Upcoming rows show real task
title/group/time, open the owning group/task flow, and retain existing status
commands; tie-break equal times by stable ID. Show a specific All caught up state
only after successful loading. Data failure is Retry, not zero progress.

Tasks card counts may use real group status counts with precise scope labels;
Vault card remains generic under the recommended Home privacy contract. Additional
modules can extend a wrapping card list; do not add tabs beyond genuine modules.

Acceptance: existing task groups remain one tap away, no locked/decrypted Vault
previews, deterministic approved progress math and proper loading/error/empty
states. Combined search/aggregates are separately gated, not implied by styling.

## 03 - Tasks

Reference: third phone in image 1. Compact title/tool row; approved date/filter
tools; sectioned task rows; extended New task; persistent selected Tasks tab.
Use lavender only for module emphasis. Existing task states keep status labels,
check/reopen/snooze icons and accessible success/warning/error differentiation.

**Safe redesign baseline:** Tasks first opens existing groups with live status
counts, create group, share and confirmed delete. Inside a group, show its name,
pending task list in manual position order, Completed and Snoozed entry points,
share and New task. E05-E07 remain authoritative. Keep group identity visible
when task titles duplicate. Existing group rename has no reachable UI action;
restoring a real rename entry point is a separately reviewed UI-only gap, not a
claim that it currently works.

Tapping the pending task text/body opens the shared AddTaskSheet prefilled for
that ID. Completion, snooze and delete are distinct targets and do not propagate
to edit. Long-press reorder retains ID/position and does not fire edit on release.
Completed/snoozed status lists keep existing reopen/resume/complete/delete actions;
editing those lists would be an optional UI extension, not existing behavior.

New task from a group uses that group; from any future aggregate feed it must
require explicit group selection, not create an orphan or choose a hidden default.
Editing uses existing updateTask with title/notes/dueAt and same ID, no duplicate
insertion, no automatic status/position change. Cancel/back never writes. Task
editor layout should become scroll-safe and reflow date/time controls, preserving
current date/time merge behavior and reminder scheduling, not silently repairing
time-zone semantics under styling.

**Date strip and chips are proposals D05:** an all-groups date feed, All/Tasks/
Notes/Reminders and grouping must have approved query semantics first. Recommend
status filters mapped to current statuses, a With notes filter on notes content
(not a separate note type), and reminder filter on dueAt. A date filter represents
local dueAt interval, not creation date. Keep an All dates/Undated escape; never
remove no-reminder tasks. Reorder stays available in a single-group pending manual
view; aggregate/date-sorted views offer Open group to reorder instead of writing
a projected order back to the database.

No weekly recurrence, independent priority levels, task tags, subtasks/progress,
cross-module Vault links or Done HH:mm exists. Omit those badges until separately
approved (D07). Snooze in task lists changes status/cancels reminder via setStatus;
the reminder popup's Snooze 10 min changes dueAt and rearms it. Do not unify these
different behaviors cosmetically. Reminders retain Done primary/Snooze secondary/
Dismiss tertiary and their current permissions/full-screen flow (E23).

Acceptance: group/search/share/status/edit/drag/reminder workflows remain fully
discoverable; Notes/recurrence/link features are never simulated; title/notes
direction handled independently; no New task action under the bottom bar.

## 04 - Vault dashboard and folders

Reference: left phone in image 2. Vault title + sort/lock tools, prominent search,
compact real total counts, colored folder cards, useful records section and Add
Item above bottom navigation. Replace the unsupported health-card region with
actual groups/items totals or remove it; never substitute a fabricated score.

**Current data:** E09/E10/E13 provide live UUID categories/subcategories, colors,
icons, names, positions and records; actual search, favorite/tag filters and sorting
by name/modified/created/category. Show Favorites or Recently modified, not Recently
used: no last-access timestamp is stored. Do not mutate modifiedAt just to infer usage.
No breach check, reused-password count or password-age analysis exists (D06).

Root shows top-level folder cards and All records / Uncategorized navigation.
Folder tap shows its direct records and immediate subfolders. Breadcrumb/up goes
to parent; Android Back does the same once child detail/editor is dismissed.
Folder-card count = direct records; subgroup count = immediate children. A separate
tree count may include descendants only if labelled as such, preserving current
distinction. Global Vault search can match paths/allowed metadata across folders;
show scope and full result path so search does not imply folder data disappeared.

Category management stays reachable from header New group and each folder menu:
rename, parent assignment with cycle prevention, custom icon/color, move up/down,
confirmed delete. Deleting a category/subtree keeps records in Uncategorized, not
delete all credentials. Optional starters remain optional. Long names reflow; chosen
colors never control the neutral group-name text. Saved groups appear from current
state without reloading; save failure leaves editor actionable.

Add Item uses the currently selected category. The visible label must be Add Item,
not only +. Search/filter result counts must distinguish filtered counts from the
global total. Empty Vault/folder/no search result/load failure are separate states.
Expanded windows retain the tree and safe detail split when widths actually fit.

Manual lock is always reachable and immediately invalidates results/details/editor;
query/sort/folder/reveal state stays session-local. Vault security contains recovery
management, master change and inactivity preferences, not the main dashboard.
Global Settings remains separate and currently locks Vault before opening (E09).

Acceptance: every group is reachable, counts truthful and live, all existing
category/record operations preserved, no hidden health/last-used assumptions,
and Add Item does not overlap the last record.

## 05 - Vault item detail

Reference: middle phone in image 2. Back at start; favorite/edit/menu at end; title
and category identity; separate readable field cards; supportive timestamps and
accurate clipboard/privacy note. Use local category/generic icons, not remote
favicon requests or unapproved service logos.

Retain separate username and email values (do not combine and lose one). Render
all populated standard URL/username/email/password/phone/reference/notes fields,
typed custom fields, tags and created/modified times. Empty optional fields can
be omitted from read-only detail but remain editable. Notes wrap and preserve
newlines. Technical values are copied exactly, irrespective of visual bidi layout.

Password and any custom field with sensitive=true OR type=SECRET are masked by
default. Each field has its own reveal and copy actions; opening another record,
locking, leaving detail or generation change resets reveal. Sensitivity does not
change after reveal. Preserve the Phase 02 predicate: only standard website or
non-secret custom URL with a valid HTTP(S) host and no user-info gets Open website.
A URL-shaped password/text/note never gets external open. Do not auto-open on row
tap, add auto-linking to Text, or route a revealed secret through ACTION_VIEW.

Favorite at detail header can call the existing favorite edit operation, keeping
record UUID; current favorite UI is in the list, so this is proposed action placement.
Overflow retains Duplicate and confirmed permanent Delete. Duplicate allocates a
new ID intentionally; normal Edit reuses the old ID. Parent folder and custom fields
remain intact. Menu actions and copy targets do not accidentally trigger row edit.

The reference's strength meter is optional presentation of the existing local
editor estimator, not stored quality certification; avoid a new detail estimator
unless reviewed for local computation and accurate wording. TOTP/2FA countdown is
not implemented, and arbitrary custom text is not a generated OTP. Omit the entire
code/timer region unless D07e receives a separate functional/security specification.

Privacy footer: recommend Copy is marked sensitive; clipboard clearing is attempted
after 30 seconds if unchanged. Screen capture protection is enabled. Do not claim
clipboard always clears or screenshots can never be captured on compromised
devices. FLAG_SECURE continues on detail/dialog/window; no real records in previews,
test artifacts, notifications or logs. TalkBack never announces secret values by
default, even when a copy/reveal button is focused (D13 governs explicit reading).

Acceptance: all field types survive round-trip edit, masking is per field,
external-action security tests remain unchanged and passing, back returns to
the same category/search position without retaining revealed text.

## 06 - Add/edit Vault item and generator

Reference: right phone in image 2. Use the bottom-sheet-style title/Cancel,
field groups, category selector, generator tool and full-width Save command.
On short screens, IME or large type, expand to full-height secure editor rather
than truncating a fixed screenshot-height sheet. At expanded widths use a centered
max-640-dp editor. The modal owns safe/IME padding and scrolling; Save stays reachable.

**Model reconciliation:** VaultRecord is generic; FieldType belongs to custom
fields, not record types. Keep a generic Item form in the safe implementation.
Login/Email/Note may later be optional field-layout presets with no new persisted
discriminator, no dropped hidden values and a Show all fields option. Card needs
separate D08 product/security approval; never request PIN, CVV or OTP. Do not show
four selectable types as if they already exist.

**Form contract:** required title; category path/Uncategorized; URL, username,
email, password, phone, reference, multiline notes; favorite; comma/Persian-comma
tags; all custom fields with stable ID, label, value, type and sensitive toggle.
Creation/modification times are read-only. Group selection retains UUID across
renames/moves. Validate using the actual document validator before repository save:
title/category validity, custom names/IDs, typed number/date/email/URL and standard
URL rules. Do not invent stricter standard-email/phone validation under visual work.
Inline field errors supplement, not replace, the existing final validation gate.

Use the existing CSPRNG and 12-128 length range, default 24, selected character
classes and at-least-one-class validation. Proposed inline generator keeps its
generated candidate local and masked by default; Generate/Regenerate does not
overwrite an existing password until Use password. Changing length/toggles is not
a silent password replacement. Cancel generator keeps previous form password.
The reference's visible generated secret is not an automatic exposure requirement.
Retain truthful local zxcvbn 0-4 guidance without remote strength services (D09).

Save preserves existing UUID/createdAt, updates modifiedAt and only replaces that
record after successful encrypted repository edit. Guard against double submit;
error keeps the editor open; no duplicate record on edit. Cancel/back makes no
write. Proposed dirty-form confirmation (D09): explicit Discard changes / Keep
editing for modified forms, including swipe/tab/back. Clean forms close directly.
User-initiated lock, timeout or session invalidation clears secret draft immediately
without waiting for discard confirmation; never reopen it after a new unlock.

Do not place passwords/recovery keys/custom secrets or decrypted draft models in
rememberSaveable, SavedStateHandle, intent extras or process-death bundles. Rotation
can lose a secure draft under the current lifecycle model; any in-memory rotation
retention requires a scoped security review, not plaintext disk persistence. Explain
unsaved loss without exposing draft content. No real-credential visual previews.

Acceptance: every existing standard/custom field remains reachable with keyboard
and TalkBack; same-ID save and cancel are verified; small-screen IME can reach all
fields/Save; generator never applies secretly; sensitivity survives every template
or layout switch.
