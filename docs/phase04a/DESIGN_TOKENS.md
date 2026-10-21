# Vaurivo design tokens: proposed, not installed

Primary direction: Premium Dark Minimal / Modern Private Productivity Hub, grounded
in all seven [supplied references](REFERENCE_ANALYSIS.md#reference-provenance).
Dark values are visually calibrated proposals, not an extracted original design
file. Light is an accessible semantic counterpart. Logo/palette approval remains
open (D02/D11); changing application colors is not authorized in Phase 04A.

## Color contracts

Opaque sRGB tokens unless stated. Use semantic names, not a literal green on every
screen. Keep task statuses, module identity and arbitrary stored group colors separate.

| Semantic token | Dark | Light | Use |
| --- | --- | --- | --- |
| canvas | `#0C1110` | `#F4F7F5` | Full-screen background and system-bar blend. |
| surface / card | `#131B19` | `#FFFFFF` | Repeated cards, fields, navigation. |
| surfaceRaised | `#1B2522` | `#EAF0EC` | Menus, modal interior, pressed neutral surfaces. |
| borderSubtle | `#26322E` | `#D2DCD6` | Decorative dividers only; not the sole control outline. |
| outlineInteractive | `#71827A` | `#718279` | Checkbox/input/outlined-button boundaries needed for recognition. |
| textPrimary | `#E7EEEC` | `#17231D` | Headings, body, active icons. |
| textSecondary | `#A5B3AC` | `#4D6256` | Supporting labels, helper/error context. |
| textTertiary | `#8C9D94` | `#596D61` | Timestamps and captions, never tiny/faint substitute text. |
| disabledContainer | `#1A211E` | `#E4EBE6` | Actually unavailable controls only. |
| disabledContent | `#708079` | `#77857C` | Disabled state; always paired with disabled semantics/reason. |
| tasksAccent | `#ADB8FF` | `#4654A6` | Tasks main command, selection and progress. |
| tasksContainer | `#24283B` | `#E8ECFF` | Tasks selected item; text uses primary/accent, not white by default. |
| vaultAccent | `#5EDFB2` | `#087556` | Vault main command, lock/unlock and focus. |
| vaultContainer | `#19362C` | `#DDF3E9` | Vault selection and icon tiles. |
| onAccent | `#0C1110` | `#FFFFFF` | Text/icons on solid module buttons; never white on dark-theme mint/lavender. |
| success | `#5EDFB2` | `#147346` | Completed/success indicator plus text/icon, not security certification. |
| warning | `#F0C06B` | `#8A5700` | Reminder/snoozed/caution with label. |
| error | `#FF8A7A` | `#B33C32` | Error/overdue/destructive action; not every pending task. |
| focusTasks | `#C7CEFF` | `#4654A6` | 2-dp outline with 2-dp offset. |
| focusVault | `#8CEBC9` | `#087556` | Matching Vault focus ring. |
| scrim | black 56% | black 32% | Non-secret dimming only; do not blur-copy decrypted backgrounds. |

Selected controls: module container + accent outline + selected icon/semantics.
Hover: surfaceRaised and outline; never the only discoverability mechanism.
Pressed: tonal change plus ordinary bounded ripple, using textPrimary unchanged.
Do not apply a blanket opacity to active content or completed task text. Completed
rows retain textSecondary contrast with check icon and strikethrough. Destructive
buttons use error text/outline on a neutral surface; filled error buttons need a
separately verified onError pair before adoption.

## Contrast calculations and limits

Calculated in this phase from the proposed opaque values using sRGB relative
luminance, linearization threshold 0.04045 and `(Llighter + .05)/(Ldarker + .05)`.
Ratios below are rounded for reporting; acceptance uses unrounded results.

| Pair | Dark ratio | Light ratio |
| --- | ---: | ---: |
| textPrimary / surface | 14.88 | 16.22 |
| textSecondary / surface | 8.05 | 6.57 |
| textTertiary / surface | 6.15 | 5.55 |
| tasksAccent / surface | 9.24 | 6.82 |
| vaultAccent / surface | 10.57 | 5.69 |
| warning / surface | 10.39 | 6.10 |
| error / surface | 7.65 | 5.82 |
| outlineInteractive / surface | 4.32 | 4.06 |
| outlineInteractive / surfaceRaised | 3.88 | 3.51 |
| onAccent / tasksAccent button | 10.04 | 6.82 |
| onAccent / vaultAccent button | 11.49 | 5.69 |

Target at least 4.5:1 for all active text (including captions), and 3:1 for required
control boundaries/state graphics. This deliberately avoids relying on the large-text
exception. WCAG is used as design guidance, not a claim of certified Android
accessibility: [text contrast](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html),
[non-text contrast](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html).
Decorative borderSubtle and disabledContent are not valid substitutes for active
outlines/text. Recompute contrast for selected containers, ripples, alpha blends,
custom colors, and actual screenshots; the table does not certify every composition.

Group colors remain stored as supplied. Use them in small icon swatches/leading
markers with a neutral, readable name; adjust only the rendered icon/background
pair for contrast, never rewrite persisted RGB values. Suggested new-group palette
can map to blue, mint, coral, amber and lavender, but existing choices win.

Task presentation proposal for D02 review: Pending uses a neutral/module marker
and explicit Pending label, Completed uses success/check/strikethrough, and Snoozed
uses warning/clock plus its label. Overdue is a derived time condition, not a fourth
persisted status. This differs from the current red-for-all-pending palette; approve
that visual distinction before replacing it. Stored PENDING/COMPLETED/SNOOZED and
their actions never change, and completed content must not be faded into illegibility.

## Typography

Keep bundled Vazirmatn from E21, weights 400/500/700. It already supports Persian
and Latin. Do not add a font dependency or assume the screenshot's exact font.
Use no negative tracking and no viewport-proportional font sizing; letterSpacing 0 sp.

| Role | Size / line height sp | Weight | Limit / application |
| --- | --- | --- | --- |
| intro display | 28 / 36 | 700 | 00 only; wraps without fixed line count. |
| screen title | 24 / 32 | 700 | Tasks/Vault/greeting; compact toolbar may grow. |
| record/form title | 20 / 28 | 700 | Detail/editor; never hero-sized. |
| section heading | 14 / 20 | 500 | Groups/Upcoming; no all-caps requirement in Persian. |
| body / input | 16 / 24 | 400 | Fields and primary content. |
| task/item title | 16 / 24 | 500 | 2+ lines allowed; completion does not shrink it. |
| support | 14 / 22 | 400 | Group paths, descriptions, hints. |
| caption / navigation | 12 / 18 | 500 | Minimum normal text size; increase if device QA requires. |
| action label | 14 / 20 | 500 | Minimum 48-dp target; multi-line allowed. |
| numeric counter | 20 / 28 | 700 | Plain integer/progress; no misleading 0 during loading. |
| secret / technical | 16 / 24 | 400 | Platform monospace for password/key when deliberately revealed; maintain glyph coverage. |

Persian uses the same semantic sizes, with room for marks and its measured font
metrics. Use start/end padding and content-based paragraph direction for prose;
render URLs/emails/Latin identifiers and runtime `Vaurivo | v...` as LTR runs within
the surrounding RTL screen. Never inject directional formatting into stored or
copied credentials. Keep password strings exact, without digit localization or trimming.
UI counts/dates may use locale formatting; PIN storage remains ASCII 4-6 digits.
Calendar changes require D10; do not equate Persian locale with an implemented Jalali picker.

Validate at font scales 1.0, 1.3, 1.5 and 2.0 in EN/FA. Fixed-format icon targets
stay stable but text-bearing containers grow; collapse two-column layouts when
needed. Do not use maxLines=1 on destructive warnings, fallback methods or buttons.

## Space, shape and responsive layout

| Token / region | Proposed contract |
| --- | --- |
| spacing scale | 4, 8, 12, 16, 20, 24, 32, 40, 48 dp; 2 dp only for optical alignment/borders. |
| screen margins | 16 dp at compact widths; 24 dp above 600 dp; content centered rather than stretched on very wide windows. |
| repeated card padding | 16 dp; 12 dp for dense list fields; 12 dp gap, never nested cards. |
| radii | repeated selection/folder cards 16 dp to match reference; fields/buttons 12 dp; icon tile 12 dp; utility tools 8 dp; sheet top 24 dp; circular progress only where genuinely circular. |
| strokes / elevation | border 1 dp, focus 2 dp; cards 0 dp elevation, overlay max 4 dp visual elevation; dark mode uses borders, not heavy shadow. |
| touch targets | >=48 x 48 dp with 8 dp separation for adjacent icon commands; glyph 20-24 dp. No overlapping enlarged invisible targets. |
| header | min 64 dp excluding status bar; grows for font scale/title. |
| bottom navigation | min 80 dp content plus system navigation inset; always labels; 3 equal tabs, grows if text needs it. |
| primary action | min 56 dp height; extended Add Item/New task at bottom-end 16 dp from content edge; reserve action height + 24 dp bottom list padding above tabs. |
| group grid | 2 columns only with >=160 dp per card after margins/gap and fitting text; otherwise 1 column. Minimum height 120 dp, no fixed text-height clamp. |
| compact <600 dp | Single content pane; full-height secure detail; editor expands for IME; use accessible modal category browser. |
| medium 600-839 dp | Centered content max 720 dp; optional paired workspace cards; do not force three panes. |
| expanded >=840 dp | Start category rail 240-260 dp; list minimum 280 dp; detail >=320 dp. Only show all three if available width fits; otherwise detail replaces list. |
| sheets / dialogs | Sheet max width 640 dp; normal initial height up to 90% available safe height, full-height when IME/large type demands. Dialog max 560 dp, scrollable body and visible commands. |

Use actual window constraints, not a device-name check. Handle split-screen,
landscape, fold hinges as unavailable area, display cutouts, gesture/3-button
navigation and hardware keyboards. Root owns system-bar insets once; each modal
owns its own safe/IME padding. Avoid double-padding nested Scaffolds. Long lists
have stable keys and preserve scroll during same-session updates.

## State, motion and Compose mapping

Propose `VaurivoColors`, spacing/shape/motion values as UI-only providers in the
existing `ui/theme` package, while retaining TaskProTheme's internal name and
ThemeMode/BackgroundStyle persisted enums. Map surface/onSurface/outline/error to
MaterialTheme; module accent is explicit per workspace, never a global mutation.
No new framework is needed. Component names are specified in [COMPONENT_SPEC.md](COMPONENT_SPEC.md).

State tokens: default, pressed, focused, hovered, selected, disabled, loading,
error and success. Motion duration/easing and reduced-motion rules are specified
in [NAVIGATION_UX.md](NAVIGATION_UX.md#motion-specification). Unreadable disabled
buttons must have an adjacent explanation; avoid indistinguishable loading/empty states.

Current divergences to resolve only after approval: fixed generic Material colors,
gradient backdrop, hardcoded dark unlock palette, inconsistent local typography
and 40-dp category tools. None are modified here (E09/E14/E21/E22; D02/D12).
