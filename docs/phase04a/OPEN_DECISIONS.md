# Open decisions and approval record

Phase 04A ends at design review. A recommendation is not approval; absence of an
answer preserves current behavior. No new preference, record type, metric, query
provider, auth mode or schema is implemented by this document.

## Settled inputs and non-negotiable constraints

- Public product name Vaurivo; English modules Tasks and Vault, Persian کارها and
  گاوصندوق. Keep Latin product spelling and existing app identity.
- Both supplied images are the primary visual direction. The user explicitly
  approved copying them into docs/phase04a/references during this turn; verified
  copies are present. No further image-copy approval is pending.
- Premium Dark Minimal with restrained lavender Tasks and mint Vault, a corresponding
  light theme, preserved Vazirmatn and existing localization. Exact tokens below
  remain proposals pending visual approval, not a new app or framework.
- Existing data/keys/backup/signing/notifications/routes are compatibility contracts,
  not branding targets. No migrations, destructive restores or source edits now.
- No hidden recovery bypass, credential transmission, locked Vault metadata, secret
  plaintext index or unsafe external URL actions. These are safety constraints,
  not optional visual preferences to trade away for screenshot fidelity.

## Decision register

| ID | Decision needed / recommendation | If deferred | Affected stage / approval class |
| --- | --- | --- | --- |
| D01 | Launch and Home policy. Recommend 00 once on first launch, Home 02 on returns, explicit chooser access anytime, optional remembered Tasks/Vault. Approve how first-use/module keys default, survive backup, and interact with reinstall/process restoration. Remembered Vault still authenticates. Keep existing route tokens: workspace for workspace/Home presentation, home for Tasks groups. | Keep current selector/start route and omit Remember choice rather than show an inert checkbox. No DataStore additions. | C; new behavior/persistence approval, plus navigation design approval. |
| D02 | Approve exact dark/light palette, 16-dp repeated cards/12-dp fields/24-dp sheets, 24-sp titles and 12-sp minimum captions in DESIGN_TOKENS. Recommend brighter readable secondary text and real interactive outlines instead of the mock's faint lines. | No theme/resource change. Tokens remain a documented candidate. | A/B; visual design approval. |
| D03 | Approve Home real aggregate queries and progress meaning. Recommend Scheduled today based on current local-day dueAt, current completed status fraction; eligible future reminders and stable upcoming order. No completedAt history exists. | Omit progress/upcoming widgets; do not fill with sample or inferred counts. Existing per-group status counts remain available in Tasks. | C; new functional read-model approval and deterministic boundary tests. |
| D04 | Global search policy. Recommend task title/notes search on Home and a Vault-search entry inside authenticated Vault, with metadata-free Home. Federated combined results would need a secure results surface, lock invalidation and explicit scope rules. | Separate existing searches; no combined-search label, persistent index, decrypted preview or hidden unlock. | C/G; new functional/security approval if combined results requested. |
| D05 | Tasks date strip/all-groups feed/filters. Recommend preserve Groups/manual view; approve dueAt/local-day semantics, All dates/Undated and status/With notes/Has reminder filters before adding. Do not reorder a filtered projection back into storage. | Existing groups/pending/status/search workflows restyled; no nonfunctional calendar/filter tools. | D; new functional/query approval. |
| D06 | Vault health metrics and usage tracking. Recommend omit 0-100 health/reused/old/breach score. Use real Groups/Items totals, Favorites or Recently modified. Approve any last-used tracking or whole-vault audit engine separately with definitions/privacy tests. | No health card or Recently used heading; no modifiedAt manipulation to invent usage. | E; new functional/security/data approval. |
| D07a | Recurrence and explicit task priority levels. No existing model. Recommend separate product/storage/reminder/backup migration specification, not this redesign. | Manual position remains priority; no Weekly badge. | Separate future feature, not stage D styling. |
| D07b | Subtasks/completion history. No task child/progress or completedAt model. Requires definitions for completion, sorting, backup and migration. | No 2/5 progress or Done HH:mm. | Separate future feature/schema approval. |
| D07c | Task tags or independent notes module. Current task notes are a field; Vault tags already exist. | Preserve notes/Vault tags; do not create separate note records or task tag UI. | Separate future feature/model approval. |
| D07d | Task-to-Vault links. Requires opaque stable relation, missing-link behavior, locked routing and no record-title leakage. | No linked-bank/Vault link shortcuts. | Separate future feature/security approval. |
| D07e | TOTP. Requires audited algorithm/library decision, encrypted seed/import/export lifecycle, clock/clipboard/accessibility/threat-model tests, backup compatibility. | Omit code/timer; custom numeric/text field is not a TOTP generator. | Separate future feature/security approval; no dependency added now. |
| D08 | Record templates versus types. Recommend generic Item first; optional Login/Email/Note layout presets only if they preserve all current fields and persist no new discriminator. Card needs a separate banking/payment data decision; never PIN/CVV/OTP collection. | Generic existing record editor with all standard/custom fields; no fake type selector. | E; presets functional/interaction review; persisted types need separate model approval. |
| D09 | New UI behaviors. Recommend dirty-form Discard/Keep editing; explicit generator Use password; restore reachable group-rename action; optional favorite placement in detail. Approve these separately from existing save/lock semantics. Lock/timeout always clears secret draft immediately. | Existing behavior remains, no pretend discard safety or generator preview/apply controls. Preserve existing favorite list and rename limitation. | B/D/E; UI-only behavior approval with new interaction tests. |
| D10 | Locale/calendar policy. Recommend existing stored epoch timestamps and locale UI formatting. Confirm Gregorian versus optional Jalali presentation before promising a Persian calendar; preserve password/PIN bytes and time merge behavior. | Existing locale/resources/date logic; no new calendar dependency/preference or date migration. | A/D/F; language review; calendar change is separate functional approval. |
| D11 | Permanent Vaurivo logo, geometry and brand palette approval. The mock symbol/gradient is not an approved icon. Follow Phase 03 launcher specification; address legacy/monochrome exports as separately scoped asset work. | Existing launcher artwork unchanged. Screen 00 uses existing symbolic icon without claiming it is the permanent mark. | Separate launcher asset approval, not a gate to documentation completion. |
| D12 | Background preferences versus flat reference canvas. Recommend restrained neutral base for operational screens while retaining existing PLAIN/GRADIENT/AURORA options; design each selected option explicitly. Do not remap stored enum meanings to hide a removed option. | Existing visuals/preferences unchanged; no default reset. | A/F; visual policy approval. |
| D13 | Accessible deliberate reading of revealed secrets. Recommend never auto-announce passwords/keys; provide an explicitly user-triggered accessible mechanism only after a security/accessibility review, preserving password semantics and privacy. Avoid excluding blind users from credential access. | Current password semantics/masking remain; no automatic secret contentDescription or unreviewed read-aloud action. | B/E/G; security/accessibility behavior approval and device/TalkBack testing. |

## Approval granularity and recommended order

Approve the overall reference-based visual direction and D02/D12 first, then authorize
A (tokens) and B (shared components). Review D01-D05 as product/navigation work
before the full 02/03 aggregate layouts. E can redesign current secure folders,
details/auth/editor without D06/D07 additions; keep generic records unless D08 is
explicitly approved. D09 behaviors and D13 accessibility need named test cases.
Finish F and G only after the authorized presentation stages are stable.

Do not treat approval of new colors as approval for recurrence, TOTP, data migrations,
remembered-module persistence, cloud services or an entirely new app. Each FN row
in FEATURE_GAP_MATRIX must remain deferred or receive specific authorization.

## Residual implementation and release gates

- No live design prototype was built in Phase 04A. Layout, TalkBack, motion, insets,
  actual biometric/Keystore enforcement and field-copy behavior need later execution.
- Earlier reported 88 passing tests do not certify new code that has not been written.
  Plan full suite/lint/build plus device and screenshot comparison after implementation.
- Existing dirty/untracked source must be reviewed/preserved before future edits.
  Clean CI/toolchain parity and original installed signing-certificate continuity
  remain unverified release gates; never uninstall/reset data to bypass an update error.
- Existing API24/25 launcher fallback and monochrome asset gaps remain outside this
  documentation-only phase. No arbitrary replacement icon has been installed.
- Security-health/strength language must never imply breach protection, audited
  security or guaranteed clipboard clearing. No network permission/service is proposed.

## Review response template

The owner can approve or amend D01-D13 individually, explicitly defer FN items,
and name the next implementation stage. A useful minimal authorization would
specify: approved token/presentation decisions; current-capability-only scope;
which new UI behaviors are included; and which functional additions stay deferred.
This is a review aid, not an instruction to create a commit or start work now.

Stop after Phase 04A. No UI coding, commit, push, publication, preference write or
new implementation stage is authorized until the user explicitly approves it.
