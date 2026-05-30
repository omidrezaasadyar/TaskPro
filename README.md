# TaskPro — تسک‌پرو

A clean, modern Android **task manager** built with Kotlin and Jetpack Compose.

Manage to-dos grouped by *items* (e.g. an Omani company, an English company,
your Iranian company, or just yourself). Each item holds a colour-coded,
re-orderable list of tasks with date/time reminders that sound on your phone.

> این یک نمونهٔ اولیهٔ حرفه‌ای است. مرحله به مرحله می‌توان آن را تغییر و گسترش داد.

---

## Features (mapped to the requirements)

1. **Entry / home screen** — lists all your items with live task counts.
2. **Menus** — a navigation drawer with **Settings**, **Add new item**,
   **Completed**, and **Snoozed** (more can be added later).
3. **Add items** — create any item you want to track tasks for
   (شرکت عمانی، شرکت انگلیسی، شرکت ایرانی، شخص خودم، …).
4. **Tasks with reminders** — each task can have a **date + time**; the phone
   plays a **sound notification** at the appointed moment (exact alarms,
   re-armed after reboot).
5. **Status + colours** — tick to **complete** (moves to Completed),
   **snooze** (moves to Snoozed). Colours: **done = green**, **pending = red**,
   **snoozed = orange**.
6. **Light / Dark theme** + **Vazirmatn** font bundled for proper Persian text
   (Settings → Theme; defaults to *follow system*).
7. **Priority reordering** — long-press a task and drag it up/down.
8. **Send a copy** — export an item's full task list as text from the home list
   or the item screen, to share via WhatsApp, etc.

### v1.1 refinements
- Every item gets its own **coloured left stripe** (auto-assigned) for quick recognition.
- Each item screen shows tappable **Completed** and **Snoozed** boxes that open
  that item's list for the chosen status.
- Home counters are stacked (number over a small label) so they fit on one line.
- **Settings → About** shows the designer/developer (Omidreza Asadyar), the
  company (EIS LLC), and the app **version** (so updates are identifiable).
- Deleting an item now asks for **confirmation** first.

---

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3, dynamic colour on Android 12+)
- **Room** for local persistence
- **DataStore** for theme preference
- **AlarmManager** for exact-time reminders (+ `BOOT_COMPLETED` re-scheduling)
- **Navigation Compose**
- Custom **long-press drag-to-reorder** for LazyColumn
- **Vazirmatn** font bundled under `app/src/main/res/font/`

Minimum SDK 24, target SDK 35.

---

## Project structure

```
app/src/main/java/com/taskpro/app/
├── TaskProApp.kt            # Application + notification channel
├── MainActivity.kt          # Compose host, theme, permissions
├── data/                    # Room entities, DAOs, repository, DI container
├── notification/            # AlarmManager scheduler + receivers
├── settings/                # DataStore theme preference
├── ui/
│   ├── theme/               # Colours, Vazirmatn typography, Material theme
│   ├── navigation/          # Routes + NavGraph
│   ├── components/          # TaskCard, drag-and-drop list, dialogs
│   └── screens/             # home, item, completed, snoozed, settings
└── util/                    # date formatting + share helper
```

---

## Building

Open in **Android Studio** (Ladybug or newer) and Run, or from the CLI:

```bash
./gradlew assembleDebug
```

The Gradle wrapper, Vazirmatn fonts, and launcher icons are all included.

### Notes
- On Android 13+ the app requests the **notifications** permission on launch.
- On Android 12+ exact alarms may need the *"Alarms & reminders"* permission in
  system settings; the app falls back to inexact alarms if it isn't granted.
- Vazirmatn is licensed under the SIL Open Font License.
