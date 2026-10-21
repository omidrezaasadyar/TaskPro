# Vaurivo launcher identity: approval-ready specification

Status: specification only, 2026-10-20. No logo, colors, geometry, artwork, theme,
dependency, or splash implementation was changed in Phase 03. Product spelling is
Vaurivo in both English and Persian; modules are Tasks and Vault (کارها / گاوصندوق).

## Existing assets and entry points

Paths below are repository-relative.

| File | Current role | Future approved treatment |
| --- | --- | --- |
| `app/src/main/AndroidManifest.xml` | Label `@string/app_name`; icons `@mipmap/ic_launcher` / `@mipmap/ic_launcher_round`; internal `Theme.TaskPro` | Keep resource references, component/package identity and theme name. Label now resolves to Vaurivo. |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | 108-dp vector, white check mark | Replace only after approval of a multi-module Vaurivo mark. |
| `app/src/main/res/drawable/ic_launcher_background.xml` | 108-dp vector, diagonal #4F46E5 to #6366F1 gradient | Replace colors/artwork only after palette approval. |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Adaptive foreground/background composition | Retain names; connect approved artwork and a monochrome layer in a separately approved asset change. |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | Same adaptive composition | Same approved artwork, validated with round masking. |
| `app/src/main/res/values/colors.xml` | Contains `ic_launcher_background` color #4F46E5 | Review alongside artwork; the current vector defines its own gradient. Do not assume changing this color changes the icon. |
| `app/src/main/res/values/themes.xml` | Light platform NoActionBar theme; no explicit splash artwork | Preserve `Theme.TaskPro`; approve any future splash colors separately. |
| `app/src/main/res/values-night/themes.xml` | Dark platform NoActionBar theme | Same preservation rule. |
| `app/src/main/java/com/taskpro/app/MainActivity.kt` | Existing Compose entry point | No dedicated splash activity or SplashScreen API setup found. Do not add a second launch screen as part of the rebrand. |

The launcher contains no text glyphs to rename. There are no legacy unqualified
mipmap icon resources or monochrome icon resources in the current source tree.
Because minSdk is 24 while adaptive definitions are v26-qualified, API 24/25
launcher icon resolution is a pre-existing gap requiring a device check and an
approved fallback asset. The two existing MonochromeLauncherIcon lint warnings
are retained, not suppressed.

Android 12+ supplies the system splash and defaults to the launcher icon; this
project does not override its splash icon/background attributes. Earlier first
frames inherit platform themes. Actual OEM rendering has not been certified.
See [Android splash guidance](https://developer.android.com/develop/ui/views/launch/splash-screen).

## Deliverable contract for the designer

1. Obtain written approval of the multi-module symbol, foreground geometry,
   background palette, optical weight and monochrome silhouette. It must represent
   Vaurivo as a whole, not require a task tick or lock as the only possible meaning.
2. Supply editable source artwork plus Android vector foreground/background assets.
   Use the existing resource names. Do not bake the wordmark, a launcher mask,
   external shadow, or corner rounding into the foreground.
3. Use a 108 x 108 dp adaptive layer canvas with essential content inside the
   centered 66 x 66 dp safe zone; allow edge artwork to be masked. Supply a separate
   single-color monochrome layer for themed icons. Validate all with Android's
   [adaptive icon requirements](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).
4. Supply API 24/25 legacy `ic_launcher` and `ic_launcher_round` fallbacks in
   mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi (48/72/96/144/192 pixel outputs respectively),
   generated from the approved master and checked in Android Studio Asset Studio.
   This is proposed work, not an asset added during Phase 03.
5. Include preview sheets on light/dark wallpapers, round/squircle/rounded-square
   masks, themed icon modes, launcher, Settings, recents, share sheet, and the
   Android 12+ system splash. Ensure a recognizable silhouette at small sizes.
6. Provide a written color/contrast and source-license record. No new asset runtime
   or animation dependency is needed. Avoid adding a splash wordmark or animation
   until separately approved, and respect reduced-motion settings if motion is added.

## Acceptance and rollout gates

- Compare approved exports to source geometry and safe-zone overlays; inspect
  mask clipping, monochrome silhouette and small-size contrast.
- Resolve API 24/25 fallbacks and test launch/cold/warm start on API 24/25, 26+, 31+
  and a themed-icon-capable launcher. No such physical checks were executed here.
- Confirm label Vaurivo in EN/FA without changing package, class or resource IDs.
- Keep database, cryptography, backup formats and signing configuration untouched.
- Verify an in-place install using the owner's original signing key; never uninstall
  as a workaround for a signature mismatch. Launcher caches may delay icon updates.

Pending: approved logo and palette, fallback/monochrome exports, device previews,
and signed-update validation. Phase 03 deliberately ships the existing artwork.
