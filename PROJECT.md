# FASTable Master Implementation Plan & Project Specification

## 1. Project Overview & Architectural Summary
FASTable is an Android native application (Kotlin, Material Design 3, Room, WorkManager, Google Sheets API, Firebase) for students of FAST NUCES university. This project encompasses major system-wide enhancements across backend notification scheduling, authentication, data persistence & caching, UI cleanup, view consistency, and dark mode theming.

## 2. Architecture & Data Flow
```
               ┌────────────────────────────────────────────────────────┐
               │                     User Interface                     │
               │  Home (Dashboard) │ Timetables │ Profile │ Free Rooms │
               └───────────┬────────────────────────────────┬───────────┘
                           │                                │
                           ▼                                ▼
               ┌────────────────────────┐      ┌────────────────────────┐
               │       ViewModels       │      │   Auth / Google Sign-In │
               │ Home, Custom, Batch,   │      │ Login & SignUp flows,  │
               │ FreeRooms, Faculty     │      │ Room UserProfileDao,   │
               └───────────┬────────────┘      │ DP Loader & Picasso    │
                           │                   └────────────────────────┘
                           ▼
               ┌────────────────────────────────────────────────────────┐
               │          Repository & Persistence Layer                │
               │  TimetableRepository (Offline-First Cache Provider)    │
               │  Room DB (AppDatabase v5: courses, timetable_sessions) │
               └───────────┬────────────────────────────────────────────┘
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
┌───────────────────────────┐ ┌───────────────────────────────────────────┐
│ Google Sheets & Remote API│ │ Background Workers & Notification Engine  │
│ Connectivity-gated sync,  │ │ NotificationScheduler (Double-Gated Cutoff)│
│ In-memory parsed cache    │ │ NotificationWorker (Strict 08:00 - 17:15) │
└───────────────────────────┘ └───────────────────────────────────────────┘
```

---

## 3. Feature Inventory

| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Strict 5:15 PM Notification Cutoff | Enforce absolute hard cutoff at 17:15 for notification scheduling and dispatching | M1 | R1 |
| 2 | Scheduled Class Hours Enforcement | Restrict notifications strictly to Monday–Saturday 08:00–17:15 window; zero Sunday notifications | M1 | R1 |
| 3 | Notification Double-Gating | Scheduling validation in NotificationScheduler + runtime validation in NotificationWorker/FCM | M1 | R1 |
| 4 | Offline-First Cache-First Persistence | Prevent destructive re-fetching on revisit; load courses and sessions instantly from Room | M2 | R6 |
| 5 | Custom Timetable Zero-Placeholder Pipeline | Eliminate dummy `"Placeholder"` course insertion and filter them out across DAOs/ViewModels | M2 | R5, R6 |
| 6 | Batch Timetable Room Persistence | Check Room `TimetableDao` before network download | M2 | R6 |
| 7 | Shared Spreadsheet Memory Cache | Centralize Google Sheets in-memory cache between TimetableRepository and FreeRoomsViewModel | M2 | R6 |
| 8 | Robust Network Connectivity Detection | Replace shell `ping` command in GoogleSheetsService with Android `ConnectivityManager` | M2 | R6 |
| 9 | Google Sign-In & Sign-Up Flow | Fix "Continue with Google" on Login & add Google button to SignUp; remove restrictive domain blocks | M3 | R2 |
| 10 | Profile Picture (DP) Retrieval & Rendering | Retrieve Google photoUrl, support web URLs in Picasso, and display DP in Profile, Drawer, TopBar | M3 | R2 |
| 11 | Resilient Profile State & Logout Fix | Make Profile.kt load from Room if FirebaseAuth is null; wipe Room profile on logout to prevent session leak | M3 | R2 |
| 12 | Dashboard Tip Banner Removal | Completely remove `cardTipBanner` from `activity_home.xml` and clean up `Home.kt` | M4 | R3 |
| 13 | Explanatory Text UI Cleanup | Remove redundant preview banners, toggle hints, disclaimers, and verbose helper text across screens | M4 | R3 |
| 14 | White Card & Box Styling | Set backgrounds of chips, badges, and preview cards to white (`?attr/colorSurface`) instead of orange | M4 | R4 |
| 15 | Circular FABs & Enlarged Plus Icon | Add `Corner.Full` shape to FABs and enlarge plus icon to 32dp for optimal touch target | M4 | R4 |
| 16 | Seamless Edge-to-Edge Timetable Views | Eliminate sharp white corners on Batch & Custom Timetable by aligning `tabContainer` edge-to-edge | M4 | R5 |
| 17 | Dark Orange Top App Bar Theming | Replace navy blue across all 11 top bars with deep, aesthetically pleasing orange (`#B84500` / `#8F3600`) | M5 | R7 |
| 18 | Dark Mode Contrast Harmonization | Eliminate visual clashes between navy, orange, and dark background; fix unreadable dark-mode text & badges | M5 | R7 |
| 19 | Unit Testing & Comprehensive E2E Verification | Implement automated cutoff tests and verify build (`assembleDebug`) and functional acceptance criteria | M6 | Acceptance Criteria |

---

## 4. Milestones & Work Decomposition

| # | Milestone Name | Scope & Requirements | Dependencies | Status |
|---|----------------|----------------------|-------------|--------|
| M1 | Backend Notification Scheduling | Enforce 5:15 PM cutoff and class hours window in `NotificationScheduler`, `NotificationWorker`, `FCMService`, `ScheduleChangeDetector` (R1) | None | DONE |
| M2 | Caching Efficiency & Zero Placeholders | Offline-first persistence in `TimetableRepository`, `CourseDao`, `CustomTimetableViewModel`, `BatchTimetableViewModel`, `FreeRoomsViewModel`, `GoogleSheetsService` (R5, R6) | None | DONE |
| M3 | Google Authentication & Profile Picture | Fix login/signup flow, domain checks, DP loading via Picasso, Profile.kt resilience, and logout cleanup (R2) | None | DONE |
| M4 | Dashboard UI, View Styling & Corner Fixes | Remove tip banner & explanatory text; white cards/boxes; circular FABs & enlarged plus icon; edge-to-edge tabs (R3, R4, R5) | M2 (for custom timetable loading) | PLANNED |
| M5 | Dark Mode Contrast & Top Bar Theme | Replace navy blue across all 11 top bars with dark orange theme; fix dark mode contrast clashes (R7) | M4 | PLANNED |
| M6 | Comprehensive Verification & Testing | Automated test suite execution, unit test validation for cutoff, forensic audit, build checks (R8) | M1, M2, M3, M4, M5 | PLANNED |

---

## 5. Interface Contracts & Boundaries

### 5.1 NotificationScheduler ↔ NotificationWorker & FCM
- **Canonical Bounds**:
  - `MIN_CLASS_MINUTES_OF_DAY = 480` (08:00 AM)
  - `MAX_CUTOFF_MINUTES_OF_DAY = 1035` (05:15 PM)
- **Function**:
  `NotificationScheduler.isWithinAllowedNotificationWindow(calendar: Calendar = Calendar.getInstance()): Boolean`
  Returns `true` strictly if `dayOfWeek != Calendar.SUNDAY` and `currentMinutes in 480..1035`.
- **Worker Contract**:
  `NotificationWorker.doWork()` must check `isWithinAllowedNotificationWindow()` as its very first operation before any database insertion or notification posting. If `false`, return `Result.success()` immediately.

### 5.2 TimetableRepository ↔ ViewModels (Cache Contract)
- **Course Entity**:
  Dummy courses with `name == "Placeholder"` must NEVER be persisted to Room DB `courses` table.
- **Cache-First Loading**:
  `CustomTimetableViewModel.loadOrSyncData(forceRefresh: Boolean = false)`: Queries Room `courseDao.getAllCoursesOnce()` first. If non-empty, sets `_isLoading.value = false` and skips network download. Only downloads if DB is empty or user requests manual refresh.
- **Shared In-Memory Cache**:
  `TimetableRepository.getSpreadsheet()` is the single authority for cached Google Sheets models; `FreeRoomsViewModel` delegates to it.

### 5.3 Authentication & Profile DP Contract
- **Google DP Storage**:
  `UserProfile.profileImageUrl` retains original Google `https://` URL or local cached path.
- **Image Loader Protocol**:
  If `profileImageUrl.startsWith("http")`: load directly via `Picasso.get().load(url).placeholder(...).error(...)`.
  If local file: verify `File(url).exists()` before loading.
- **Session Lifecycle**:
  `UserProfileDao.deleteAllProfiles()` must be invoked upon user logout to prevent unauthorized auto-login from `SplashScreen`.

---

## 6. Code Layout & Exclusive File Ownership

```
FASTable/
├── app/src/main/
│   ├── java/com/example/fastable/
│   │   ├── utils/
│   │   │   └── NotificationScheduler.kt             [M1 Exclusive]
│   │   ├── services/
│   │   │   ├── NotificationWorker.kt                [M1 Exclusive]
│   │   │   ├── FCMService.kt                        [M1 Exclusive]
│   │   │   └── ScheduleChangeDetector.kt            [M1 Exclusive]
│   │   ├── data/
│   │   │   ├── local/CourseDao.kt                   [M2 Exclusive]
│   │   │   ├── remote/GoogleSheetsService.kt        [M2 Exclusive]
│   │   │   └── repository/TimetableRepository.kt    [M2 Exclusive]
│   │   ├── viewmodel/
│   │   │   ├── CustomTimetableViewModel.kt          [M2 Exclusive]
│   │   │   ├── BatchTimetableViewModel.kt           [M2 Exclusive]
│   │   │   └── FreeRoomsViewModel.kt                [M2 Exclusive]
│   │   ├── Login.kt                                 [M3 Exclusive]
│   │   ├── SignUp.kt                                [M3 Exclusive]
│   │   ├── Profile.kt                               [M3 Exclusive]
│   │   ├── Add_Timetable.kt                         [M2 / M4]
│   │   ├── CustomTimetable.kt                       [M4]
│   │   ├── Home.kt                                  [M3 logout / M4 tip banner / M5 theme]
│   │   └── adapters/
│   │       ├── DashboardSessionAdapter.kt           [M5 Exclusive]
│   │       └── NotificationAdapter.kt               [M5 Exclusive]
│   └── res/
│       ├── layout/
│       │   ├── activity_home.xml                    [M4 Exclusive]
│       │   ├── activity_custom_timetable.xml        [M4 Exclusive]
│       │   ├── activity_add_timetable_new.xml       [M4 Exclusive]
│       │   ├── item_selected_chip.xml               [M4 Exclusive]
│       │   ├── dialog_edit_profile.xml              [M4 Exclusive]
│       │   ├── activity_login.xml                   [M3 Exclusive]
│       │   ├── activity_sign_up.xml                 [M3 Exclusive]
│       │   ├── activity_profile.xml                 [M3 / M5]
│       │   ├── activity_notification.xml            [M5 Exclusive]
│       │   ├── activity_faculty_offices.xml         [M5 Exclusive]
│       │   ├── activity_free_rooms.xml              [M5 Exclusive]
│       │   ├── activity_campus_map.xml              [M5 Exclusive]
│       │   ├── activity_about_us.xml                [M5 Exclusive]
│       │   └── activity_user_manual.xml             [M5 Exclusive]
│       ├── drawable/
│       │   └── section_badge_bg.xml                 [M4 Exclusive]
│       └── values/
│           ├── colors.xml                           [M5 Exclusive]
│           ├── themes.xml                           [M4 / M5]
│           └── values-night/themes.xml              [M5 Exclusive]
└── app/src/test/java/com/example/fastable/
    └── NotificationCutoffTest.kt                    [M1 / M6 Exclusive]
```

---

## 7. Detailed Step-by-Step Implementation Plans

### 7.1 Requirement R1: Backend Notification Scheduling
1. In `NotificationScheduler.kt`:
   - Define `MIN_CLASS_MINUTES_OF_DAY = 480` (8:00 AM) and `MAX_CUTOFF_MINUTES_OF_DAY = 1035` (5:15 PM).
   - Implement `isWithinAllowedNotificationWindow(calendar)` checking that current day is not Sunday and minute-of-day is between 480 and 1035.
   - In `getSessionTimeInMillis`: reject session if day is Sunday or if class start time is past 17:15.
   - In `scheduleNotificationForSession`: check target trigger time (`sessionTime - 15m`). If trigger time minute-of-day > 1035 or < 480, drop scheduling.
2. In `NotificationWorker.kt`:
   - At the entry of `doWork()`, verify `NotificationScheduler.isWithinAllowedNotificationWindow()`. If false, log suppression and return `Result.success()`.
   - Ensure `saveNotificationToDatabase` occurs ONLY AFTER the cutoff check passes.
   - Update `isQuietHours()` to delegate directly to `!NotificationScheduler.isWithinAllowedNotificationWindow()`.
3. In `FCMService.kt` & `ScheduleChangeDetector.kt`:
   - Suppress push notification display and database saving if current time is outside the allowed notification window.

### 7.2 Requirement R2: Google Authentication & Profile Picture
1. In `activity_login.xml`: Update button text to `"Continue with Google"`.
2. In `Login.kt`:
   - Remove hard domain check in `handleGoogleSignInResult` so all valid Google accounts succeed.
   - In `saveUserProfileAndProceed`, download and cache photo URL locally and initialize Room `UserProfile`.
3. In `activity_sign_up.xml` & `SignUp.kt`:
   - Add `"Continue with Google"` MaterialButton matching the login screen.
   - Wire `GoogleSignInClient` and launcher to enable 1-tap Google sign-up.
4. In `Profile.kt`:
   - In `loadUserProfile()`, fallback to Room `userProfileDao.getAnyUserProfile()` if `FirebaseAuth.currentUser` is null.
   - In `displayUserProfile()`, support HTTP URLs:
     `if (profile.profileImageUrl.startsWith("http")) Picasso.get().load(url)...`
   - In `syncProfileFromFirebase()`, preserve HTTP photo URLs without treating them as Base64 strings.
   - In edit dialog save actions, use Room profile UID when Firebase user is null.
5. In `Home.kt`:
   - In `btnLogout`, execute `userProfileDao.deleteAllProfiles()` so logout completely invalidates the session and prevents `SplashScreen` auto-login loops.
   - Display profile DP in drawer header and bind click listener.

### 7.3 Requirement R3, R4, R5: UI Cleanup, Styling & Corners
1. Dashboard UI Cleanup (R3):
   - In `activity_home.xml`: Delete `cardTipBanner` (lines 37–80). Re-anchor `tabContainer` to `topAppBar`.
   - In `Home.kt`: Remove `setupTipBanner()` method and call.
   - In `activity_add_timetable_new.xml`: Remove preview banner card (lines 275–293) and toggle hint.
   - In `dialog_edit_profile.xml`: Remove explanatory disclaimer note.
   - In `activity_custom_timetable.xml`: Remove redundant `"Select Batch & Section"` TextView header.
2. Card, Box & Button Styling (R4):
   - In `item_selected_chip.xml`: Change card background to `?attr/colorSurface` (white) and text color to `?attr/colorOnSurface`.
   - In `section_badge_bg.xml`: Set `<solid android:color="@color/white" />`.
   - In `item_timetable_session.xml`: Remove orange background tint from `tvSection`.
   - In `activity_home.xml`:
     - On `fabAddTimetable`: add `app:shapeAppearanceOverlay="@style/ShapeAppearance.Material3.Corner.Full"` and `app:maxImageSize="32dp"`.
     - On `fabRefresh`: add `app:shapeAppearanceOverlay="@style/ShapeAppearance.Material3.Corner.Full"` and change background tint from navy to orange.
3. Timetable Views & Corners (R5):
   - In `activity_custom_timetable.xml` & `activity_add_timetable_new.xml`:
     - Update `tabContainer`: remove `marginHorizontal="20dp"`, set width to `match_parent`, set `background="?attr/colorSurface"`, padding horizontal `16dp`, padding vertical `8dp`.
     - Matches dashboard layout edge-to-edge, eliminating sharp white corners.

### 7.4 Requirement R5 (Placeholders) & R6: Caching Efficiency
1. In `TimetableRepository.kt`:
   - In `syncBatchesOnly()`: Do NOT insert dummy `Course(name = "Placeholder", ...)` rows into `courseDao`.
   - In `syncData()`: Filter out any `"Placeholder"` items when checking `existingCourses`.
2. In `CourseDao.kt`: Add `WHERE name != 'Placeholder'` to `getAllCourses()` queries.
3. In `CustomTimetableViewModel.kt`:
   - In `loadInitialData()`: Filter out placeholder items.
   - Implement `loadOrSyncData(forceRefresh: Boolean = false)`: If courses exist in Room cache, load them immediately and skip network sync unless user explicitly requests refresh.
4. In `Add_Timetable.kt`:
   - Replace unconditional `viewModel.syncData(forceRefresh = true)` with `viewModel.loadOrSyncData(forceRefresh = false)`.
   - Keep course RecyclerView hidden and show loading indicator until real courses are confirmed loaded.
5. In `BatchTimetableViewModel.kt`:
   - In `loadTimetable()`: Read from Room `TimetableDao` first. If cached sessions exist, emit immediately.
6. In `FreeRoomsViewModel.kt`:
   - Delegate `getSpreadsheet()` to `TimetableRepository` to share the in-memory cache.
7. In `GoogleSheetsService.kt`:
   - Replace shell `ping` with Android `ConnectivityManager`.

### 7.5 Requirement R7: Dark Mode Contrast & Top Bar Theme
1. Define top bar theme colors in `colors.xml`:
   - Light top bar orange: `#B84500` (WCAG AA > 5.1:1 against white text).
   - Dark top bar orange: `#8F3600` (Deep warm amber-orange).
2. In `themes.xml` & `values-night/themes.xml`:
   - Add theme attribute `colorTopBar`. Set `android:statusBarColor` to match.
   - In `values-night/themes.xml`: Replace discordant light sky blue (`#60A5FA`) and navy (`@color/secondary_navy`) with harmonious warm amber and slate surface tones.
3. Replace navy blue (`@color/secondary_navy`, `#0D2A5C`, `#0A2E5D`) across all 11 top bars with `?attr/colorTopBar` (or `@color/top_bar_orange`).
4. Fix dark mode contrast across screens:
   - In `Home.kt:153,166`: Replace `secondary_navy` for unselected tab text with theme attribute `colorOnSurfaceVariant`.
   - In `DashboardSessionAdapter.kt:80`: Use contrast-compliant badge color in dark mode.
   - In `activity_profile.xml`: Replace hardcoded white background with `?android:attr/colorBackground`, and navy texts with semantic attributes.
   - In `activity_notification.xml` & `NotificationAdapter.kt`: Replace hardcoded `#F5F7FA` and white card backgrounds with theme background and surface attributes.
   - In `item_free_room.xml`, `item_slot_free_rooms.xml`, `item_map_location.xml`: Remove hardcoded white containers and apply semantic theme colors.
