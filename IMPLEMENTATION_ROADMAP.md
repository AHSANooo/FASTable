# FASTable - Detailed Implementation Roadmap

## Project Overview
FASTable is an Android timetable management application for FAST-NUCES students that integrates Firebase Authentication, Firestore, SQLite, and Google Sheets API for seamless schedule management with offline support.

**Current Status:** Basic authentication structure in place with Sign Up, Login, and Home screens. Firebase integration started.

---

## Phase 1: Fix & Stabilize Authentication System (Week 1)

### 1.1 Fix Sign_Up.kt Syntax Errors
- **Issue:** File has corrupted code structure with mixed implementation sections
- **Tasks:**
  - [ ] Rewrite Sign_Up.kt with proper class structure
  - [ ] Fix package declaration (should be at top)
  - [ ] Fix import statements placement
  - [ ] Reorganize onCreate(), form validation, and sendVerificationEmail() methods
  - [ ] Ensure single verification email flow without duplication
- **Testing:**
  - [ ] Compile without errors
  - [ ] Test email validation with invalid format
  - [ ] Test password strength validation (min 6 characters)
  - [ ] Test password matching
  - [ ] Verify user creation in Firebase Console

### 1.2 Fix Firebase Email Verification Domain Issue
- **Error:** "UNAUTHORIZED_DOMAIN: Domain not allowlisted by project"
- **Root Cause:** Custom email template uses domain not added to Firebase approved list
- **Tasks:**
  - [ ] Go to Firebase Console → Authentication → Templates → Email Verification
  - [ ] Reset to default template
  - [ ] Verify email is sent successfully
  - [ ] Test verification link
- **Alternative:** If custom template needed:
  - [ ] Go to Firebase Console → Settings → Authorized Domains
  - [ ] Add your custom domain (if using Firebase Dynamic Links)
  - [ ] Update email template with allowed domain

### 1.3 Enhance Email Verification Flow
- **Tasks:**
  - [ ] Use Firebase ActionCodeSettings with Dynamic Links
  - [ ] Test ActionCodeSettings configuration:
    ```kotlin
    val actionCodeSettings = ActionCodeSettings.newBuilder()
        .setUrl("https://fastable.firebaseapp.com/verify")
        .setHandleCodeInApp(false)
        .setAndroidPackageName(
            "com.example.fastable",
            true,  // installIfNotAvailable
            "21"   // minimumVersion
        )
        .build()
    ```
  - [ ] Implement email verification check on Login screen
  - [ ] Add "Resend Email" option
  - [ ] Log all verification states in Logcat
- **Testing:**
  - [ ] Create test account with real email (Gmail recommended)
  - [ ] Verify email link works in browser
  - [ ] Verify user cannot login until email verified
  - [ ] Test resend email functionality

### 1.4 Enhance Login.kt
- **Tasks:**
  - [ ] Add email verification check before allowing login
  - [ ] Show appropriate error message if email not verified
  - [ ] Implement "Resend Verification Email" button
  - [ ] Add "Forgot Password" functionality
  - [ ] Improve error handling with specific Firebase error messages
  - [ ] Add loading state during authentication
- **Testing:**
  - [ ] Try login with unverified email
  - [ ] Test resend verification
  - [ ] Test forgot password flow

### 1.5 Create User Data Model in Firestore
- **Tasks:**
  - [ ] Create `UserData` data class in new file: `models/UserData.kt`
    ```kotlin
    data class UserData(
        val userId: String = "",
        val name: String = "",
        val email: String = "",
        val batch: String = "",
        val degree: String = "",
        val section: String = "",
        val profilePictureUrl: String = "",
        val createdAt: Long = System.currentTimeMillis(),
        val isEmailVerified: Boolean = false
    )
    ```
  - [ ] Create Firestore user document on successful signup
  - [ ] Store user metadata (batch, degree, section)
  - [ ] Test document creation in Firebase Console

### 1.6 Session Management
- **Tasks:**
  - [ ] Implement SharedPreferences to store user session
  - [ ] Create `SessionManager` utility class:
    - [ ] Save user login state
    - [ ] Store user ID for quick access
    - [ ] Clear session on logout
  - [ ] Implement logout functionality in Menu screen
- **Testing:**
  - [ ] Verify session persists after app restart
  - [ ] Verify session clears on logout

---

## Phase 2: User Interface & Profile Management (Week 2)

### 2.1 Fix & Enhance SplashScreen.kt
- **Tasks:**
  - [ ] Implement 2-3 second splash screen delay
  - [ ] Check if user is already logged in
  - [ ] Redirect to Home if logged in, else to Login
  - [ ] Show app logo and name on splash
- **Testing:**
  - [ ] Fresh install → goes to Login
  - [ ] Logged-in user → goes to Home immediately

### 2.2 Develop Profile Screen (Profile.kt & activity_profile.xml)
- **Tasks:**
  - [ ] Create layout with:
    - [ ] Profile picture (circular ImageView)
    - [ ] Edit Picture button (camera/gallery)
    - [ ] Name, Email, Batch, Degree, Section (all editable)
    - [ ] Save Changes button
    - [ ] Logout button
  - [ ] Implement image upload to Firebase Storage
  - [ ] Implement profile update to Firestore
  - [ ] Load user data from Firestore on profile open
- **Subtasks for Image Upload:**
  - [ ] Request CAMERA and READ_EXTERNAL_STORAGE permissions
  - [ ] Implement image picker (camera + gallery options)
  - [ ] Compress image before upload
  - [ ] Upload to Firebase Storage: `/users/{userId}/profile.jpg`
  - [ ] Update Firestore with image URL
  - [ ] Display image with circular transformation
- **Testing:**
  - [ ] Load profile and verify all data displays correctly
  - [ ] Edit each field and save
  - [ ] Upload profile picture and verify in Storage
  - [ ] Restart app and verify changes persist

### 2.3 Improve Menu Screen (Menu.kt & activity_menu.xml)
- **Tasks:**
  - [ ] Create navigation drawer/menu with options:
    - [ ] Home
    - [ ] Timetable
    - [ ] Add Custom Timetable
    - [ ] Profile
    - [ ] Notifications
    - [ ] About Us
    - [ ] Logout
  - [ ] Implement proper navigation between screens
  - [ ] Show current user name/email in menu header
- **Testing:**
  - [ ] Navigate through all menu items
  - [ ] Verify back navigation works correctly

### 2.4 Develop About Us Screen (AboutUs.kt & activity_about_us.xml)
- **Tasks:**
  - [ ] Create professional about screen with:
    - [ ] App description
    - [ ] Version number
    - [ ] Developer info
    - [ ] GitHub link
    - [ ] Support email
- **Testing:**
  - [ ] Verify all links work

---

## Phase 3: Timetable Data Management - Setup (Week 3)

### 3.1 Create Database Layer - SQLite Setup
- **Tasks:**
  - [ ] Add SQLite dependencies to `build.gradle.kts`:
    ```kotlin
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    ```
  - [ ] Create database entities in `database/entities/`:
    - [ ] `CourseEntity.kt` - represents a course
    - [ ] `TimetableEntity.kt` - represents a timetable slot
    - [ ] `SyncMetadataEntity.kt` - tracks last sync time
  - [ ] Create Room Database class: `database/FASTableDatabase.kt`
  - [ ] Create DAO interfaces:
    - [ ] `CourseDao.kt`
    - [ ] `TimetableDao.kt`
  - [ ] Implement database migration strategy
- **Entity Structures:**
  - **CourseEntity:**
    - courseId (PrimaryKey)
    - courseCode
    - courseName
    - instructor
    - credits
  - **TimetableEntity:**
    - slotId (PrimaryKey)
    - courseId (ForeignKey)
    - dayOfWeek
    - startTime
    - endTime
    - location
    - isCustom
  - **SyncMetadataEntity:**
    - syncId (PrimaryKey)
    - lastSyncTime
    - syncStatus

### 3.2 Create Firebase Firestore Structure
- **Tasks:**
  - [ ] Design Firestore collection structure:
    ```
    /timetables
      /{batchId}
        /{degreeId}
          /{sectionId}
            /courses -> array of course objects
            /metadata
              lastUpdated: timestamp
              version: number
    ```
  - [ ] Create Firestore data classes in `models/`:
    - [ ] `FirestoreCourse.kt`
    - [ ] `FirestoreTimetable.kt`
    - [ ] `FirestoreSync.kt`
  - [ ] Set Firestore Security Rules:
    ```
    rules_version = '3';
    service cloud.firestore {
      match /databases/{database}/documents {
        match /timetables/{batchId}/{degreeId}/{sectionId}/{document=**} {
          allow read: if request.auth != null;
          allow write: if false; // Only admin writes
        }
        match /users/{userId}/{document=**} {
          allow read, write: if request.auth.uid == userId;
        }
      }
    }
    ```

### 3.3 Create Data Sync Manager
- **Tasks:**
  - [ ] Create `services/SyncManager.kt`:
    - [ ] Check connectivity before sync
    - [ ] Fetch timetable from Firestore
    - [ ] Save to SQLite cache
    - [ ] Handle sync conflicts
    - [ ] Update sync metadata
  - [ ] Implement WorkManager for periodic sync:
    ```kotlin
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    ```
  - [ ] Schedule daily timetable sync (e.g., 2 AM)
- **Sync Logic:**
  - [ ] Compare local last sync time with Firestore
  - [ ] If newer version in Firestore, download and cache
  - [ ] If sync fails, use cached version
  - [ ] Log all sync attempts

### 3.4 Integrate Google Sheets API
- **Tasks:**
  - [ ] Add Google Sheets dependencies:
    ```kotlin
    implementation("com.google.android.gms:play-services-sheets:v4-rev20210622-1.30.10")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")
    ```
  - [ ] Create Google Sheets API service class: `services/GoogleSheetsService.kt`
  - [ ] Implement sheet parsing logic:
    - [ ] Authenticate with API key
    - [ ] Fetch data from configured Google Sheet
    - [ ] Parse data according to structure:
      - [ ] Row 1: Headers
      - [ ] Column A: Course Code
      - [ ] Column B: Course Name
      - [ ] Column C: Instructor
      - [ ] Column D: Day
      - [ ] Column E: Start Time
      - [ ] Column F: End Time
      - [ ] Column G: Location
  - [ ] Handle color-based parsing (if applicable):
    - [ ] Different colors represent different batches/sections
  - [ ] Upload parsed data to Firestore
- **Tasks for Batch Upload to Firestore:**
  - [ ] Create admin tool/script to:
    - [ ] Read Google Sheet
    - [ ] Parse data
    - [ ] Group by batch/degree/section
    - [ ] Upload to Firestore collection structure

---

## Phase 4: Home Screen & Timetable Display (Week 4)

### 4.1 Develop Home Screen Layout (activity_home.xml)
- **Tasks:**
  - [ ] Create layout with:
    - [ ] Material Toolbar with menu icon + notification icon
    - [ ] Welcome message with user name
    - [ ] Current day timetable in card format
    - [ ] Upcoming 3 days timetable
    - [ ] Quick action buttons (Add Custom, View All)
    - [ ] Sync status indicator
  - [ ] Use RecyclerView for timetable slots
  - [ ] Implement CardView for each course slot

### 4.2 Create Timetable Display Components
- **Tasks:**
  - [ ] Create `adapters/TimetableAdapter.kt`:
    - [ ] Display course slots with times
    - [ ] Show course code, name, location
    - [ ] Color-code by subject type
    - [ ] Show sync status
  - [ ] Create `models/CourseSlot.kt` data class
  - [ ] Implement click listener for course details
- **Testing:**
  - [ ] Display test data in RecyclerView
  - [ ] Verify proper layout rendering

### 4.3 Enhance Home.kt Implementation
- **Tasks:**
  - [ ] Load user batch/degree/section from Firestore
  - [ ] Fetch today's timetable from SQLite
  - [ ] Display on Home screen
  - [ ] Implement swipe refresh for manual sync
  - [ ] Add "View All Courses" navigation
  - [ ] Show offline/online sync status
- **Offline Handling:**
  - [ ] Check connectivity on app start
  - [ ] Load from SQLite cache if offline
  - [ ] Show "Offline Mode" indicator
  - [ ] Sync in background when online
- **Testing:**
  - [ ] Display home screen with test timetable
  - [ ] Disable network and verify offline display
  - [ ] Enable network and verify sync

---

## Phase 5: Course Search & Custom Timetable (Week 5)

### 5.1 Develop Course List Screen
- **Tasks:**
  - [ ] Create `CourseListActivity.kt`
  - [ ] Create layout `activity_course_list.xml` with:
    - [ ] SearchView at top
    - [ ] RecyclerView for courses
    - [ ] Filter options (subject, instructor)
  - [ ] Load all courses from SQLite
  - [ ] Implement search by:
    - [ ] Course code
    - [ ] Course name
    - [ ] Instructor name
  - [ ] Implement filter:
    - [ ] By batch/degree/section
    - [ ] By instructor
  - [ ] Display course details in expandable cards
- **Testing:**
  - [ ] Search for existing courses
  - [ ] Verify results update in real-time
  - [ ] Test filters

### 5.2 Develop Custom Timetable Feature (Add_Timetable.kt & CustomTimetable.kt)
- **Tasks:**
  - [ ] Create `AddCustomTimetableActivity.kt` with form:
    - [ ] Course name
    - [ ] Day of week (spinner)
    - [ ] Start time (time picker)
    - [ ] End time (time picker)
    - [ ] Location
    - [ ] Notes (optional)
    - [ ] Add button
  - [ ] Create `CustomTimetableDao` in Room
  - [ ] Add table: `custom_timetable` with:
    - [ ] id (PrimaryKey)
    - [ ] userId (ForeignKey)
    - [ ] courseId
    - [ ] startTime
    - [ ] endTime
    - [ ] location
    - [ ] notes
    - [ ] createdAt
  - [ ] Save custom timetable to SQLite
  - [ ] Display custom courses mixed with regular courses
  - [ ] Implement delete custom course
  - [ ] Implement edit custom course
- **Validation:**
  - [ ] Validate end time > start time
  - [ ] Prevent duplicate custom courses same day/time
  - [ ] Check time format (24-hour)
- **Testing:**
  - [ ] Add custom course
  - [ ] Verify it appears in timetable
  - [ ] Edit and delete custom course

### 5.3 Create Course Details Dialog/Screen
- **Tasks:**
  - [ ] Show when clicking a course:
    - [ ] Course code & name
    - [ ] Instructor
    - [ ] Day & time
    - [ ] Location
    - [ ] Credits
    - [ ] Add to custom (if not already)
    - [ ] Close button
  - [ ] Allow marking as favorite (optional)

---

## Phase 6: Notifications & Push Notifications (Week 6)

### 6.1 Setup Firebase Cloud Messaging (FCM)
- **Tasks:**
  - [ ] Add FCM dependencies:
    ```kotlin
    implementation("com.google.firebase:firebase-messaging-ktx:23.2.1")
    ```
  - [ ] Create `services/MyFirebaseMessagingService.kt`:
    - [ ] Extend FirebaseMessagingService
    - [ ] Override onMessageReceived()
    - [ ] Override onNewToken()
  - [ ] Store FCM token in Firestore under user profile
  - [ ] Add to AndroidManifest.xml:
    ```xml
    <service android:name=".services.MyFirebaseMessagingService"
        android:exported="false">
        <intent-filter>
            <action android:name="com.google.firebase.MESSAGING_EVENT" />
        </intent-filter>
    </service>
    ```
  - [ ] Add INTERNET and POST_NOTIFICATIONS permissions

### 6.2 Create Notification Display System
- **Tasks:**
  - [ ] Create `NotificationManager` utility:
    - [ ] Show local notifications
    - [ ] Create notification channels
    - [ ] Handle notification tapping
  - [ ] Create notification database table:
    - [ ] notificationId
    - [ ] title
    - [ ] message
    - [ ] type (timetable_change, cancellation, new_course)
    - [ ] timestamp
    - [ ] isRead
    - [ ] relatedCourseId
  - [ ] Create `NotificationEntity.kt` and DAO

### 6.3 Develop Notification Screen (Notification.kt & activity_notification.xml)
- **Tasks:**
  - [ ] Create layout with:
    - [ ] List of notifications (newest first)
    - [ ] Mark as read/unread
    - [ ] Delete notification option
    - [ ] Filter by type
    - [ ] Empty state message
  - [ ] Load notifications from SQLite
  - [ ] Implement swipe to delete
  - [ ] Click notification → show full details
  - [ ] Implement mark all as read
- **Testing:**
  - [ ] Display test notifications
  - [ ] Verify delete functionality
  - [ ] Test mark as read

### 6.4 Implement Timetable Update Detection
- **Tasks:**
  - [ ] Create comparison logic in SyncManager:
    - [ ] Compare local timetable with Firestore
    - [ ] Detect changes, cancellations, additions
  - [ ] Trigger notifications when:
    - [ ] Course is rescheduled
    - [ ] Course is canceled
    - [ ] New course added for user's batch/section
  - [ ] Send local notification with details
- **Testing:**
  - [ ] Manually update timetable in Firestore
  - [ ] Trigger sync and verify notification appears

---

## Phase 7: Offline Support & Data Caching (Week 7)

### 7.1 Implement SQLite Caching Strategy
- **Tasks:**
  - [ ] Add last sync timestamp to all tables
  - [ ] Implement cache expiration (7 days)
  - [ ] Create `CacheManager.kt`:
    - [ ] Check if cache is valid
    - [ ] Refresh cache if needed
    - [ ] Clear expired cache
  - [ ] Implement transaction-based operations

### 7.2 Connectivity Management
- **Tasks:**
  - [ ] Add connectivity utilities:
    ```kotlin
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    ```
  - [ ] Create `NetworkManager.kt`:
    - [ ] Check internet connectivity
    - [ ] Listen to connectivity changes
    - [ ] Auto-sync when online
  - [ ] Add INTERNET and ACCESS_NETWORK_STATE permissions
  - [ ] Show connectivity status in UI

### 7.3 Implement Offline UI Indicators
- **Tasks:**
  - [ ] Add "Offline" banner at top when no internet
  - [ ] Show cached data with "Last updated: {time}"
  - [ ] Disable data modification in offline mode
  - [ ] Show sync progress indicator when syncing
- **Testing:**
  - [ ] Disconnect network → verify offline mode
  - [ ] Reconnect → verify sync happens

### 7.4 Handle Data Conflicts
- **Tasks:**
  - [ ] Document conflict resolution strategy:
    - [ ] Last-write-wins for timetable updates
    - [ ] User actions preserved (custom courses)
  - [ ] Implement logging for conflict resolution
  - [ ] Manual conflict resolution UI (if needed)

---

## Phase 8: Push Notification Server Setup (Week 8)

### 8.1 Create Firebase Cloud Function (Node.js)
- **Tasks:**
  - [ ] Setup Firebase Cloud Functions:
    ```bash
    npm install -g firebase-tools
    firebase init functions
    ```
  - [ ] Create function to detect timetable changes:
    ```javascript
    exports.onTimetableUpdate = functions.firestore
        .document('timetables/{batchId}/{degreeId}/{sectionId}')
        .onUpdate(async (change, context) => {
            // Detect changes
            // Get all user tokens with this batch/section
            // Send push notification to all users
        });
    ```
  - [ ] Implement change detection logic
  - [ ] Query user FCM tokens from Firestore
  - [ ] Send bulk notifications using Firebase Admin SDK

### 8.2 Test Push Notifications
- **Tasks:**
  - [ ] Deploy Cloud Function
  - [ ] Manually update Firestore timetable
  - [ ] Verify notifications received on app
  - [ ] Test notification tap → open app

---

## Phase 9: Testing & Quality Assurance (Week 9)

### 9.1 Unit Testing
- **Tasks:**
  - [ ] Add test dependencies:
    ```kotlin
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
    testImplementation("androidx.test:core:1.5.0")
    ```
  - [ ] Create tests for:
    - [ ] Data parsing from Google Sheets
    - [ ] Cache validation logic
    - [ ] Sync conflict resolution
    - [ ] Form validation

### 9.2 UI Testing
- **Tasks:**
  - [ ] Add Espresso dependencies:
    ```kotlin
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    ```
  - [ ] Create UI tests for:
    - [ ] Login flow
    - [ ] Sign up flow
    - [ ] Timetable display
    - [ ] Search functionality
    - [ ] Navigation

### 9.3 Manual Testing Checklist
- **Authentication:**
  - [ ] Sign up with valid/invalid credentials
  - [ ] Email verification flow
  - [ ] Login with verified/unverified email
  - [ ] Logout
  - [ ] Password reset

- **Timetable Display:**
  - [ ] Load timetable for different batches/sections
  - [ ] Display today, tomorrow, next 3 days correctly
  - [ ] Color coding works
  - [ ] Swipe refresh works

- **Offline Mode:**
  - [ ] Disconnect network
  - [ ] App still shows cached timetable
  - [ ] Offline banner visible
  - [ ] Reconnect and auto-sync

- **Custom Timetable:**
  - [ ] Add custom course
  - [ ] Edit custom course
  - [ ] Delete custom course
  - [ ] Time validation works

- **Search:**
  - [ ] Search by course code
  - [ ] Search by course name
  - [ ] Search by instructor
  - [ ] Filters work correctly

- **Notifications:**
  - [ ] Receive notification when timetable updates
  - [ ] Click notification → relevant course shown
  - [ ] Delete notification
  - [ ] Mark as read

- **Profile:**
  - [ ] View profile data
  - [ ] Edit profile
  - [ ] Upload profile picture
  - [ ] Logout from profile

---

## Phase 10: Optimization & Release Preparation (Week 10)

### 10.1 Performance Optimization
- **Tasks:**
  - [ ] Implement image caching with Glide:
    ```kotlin
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    ```
  - [ ] Database query optimization:
    - [ ] Add indexes to frequently queried columns
    - [ ] Implement pagination for large lists
  - [ ] Memory leak detection and fixes
  - [ ] Firebase query optimization
  - [ ] Use ProGuard/R8 for code shrinking

### 10.2 Security Hardening
- **Tasks:**
  - [ ] Implement certificate pinning for API calls
  - [ ] Secure SharedPreferences with EncryptedSharedPreferences:
    ```kotlin
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    ```
  - [ ] Validate all user inputs
  - [ ] Never store sensitive data in logs
  - [ ] Review Firestore security rules
  - [ ] Test with OWASP Mobile Top 10

### 10.3 Code Quality
- **Tasks:**
  - [ ] Run static analysis (Lint)
  - [ ] Fix all warnings
  - [ ] Code formatting consistency
  - [ ] Documentation comments for public APIs
  - [ ] Remove debug code

### 10.4 Build Optimization
- **Tasks:**
  - [ ] Enable minification in release build
  - [ ] Test release APK on devices
  - [ ] Reduce app size:
    - [ ] Remove unused resources
    - [ ] Use vector drawables
    - [ ] Optimize images
  - [ ] Test crash handling with Firebase Crashlytics:
    ```kotlin
    implementation("com.google.firebase:firebase-crashlytics-ktx:18.6.0")
    ```

### 10.5 Prepare for Release
- **Tasks:**
  - [ ] Create signed release APK
  - [ ] Test on multiple devices (API 24+)
  - [ ] Test on different screen sizes
  - [ ] Test on different orientations
  - [ ] Prepare app store listing
  - [ ] Create privacy policy
  - [ ] Create terms of service
  - [ ] Prepare screenshots for Play Store

---

## Phase 11: Post-Release & Maintenance

### 11.1 Beta Testing
- **Tasks:**
  - [ ] Distribute beta APK to 20-50 testers
  - [ ] Gather feedback through Google Form
  - [ ] Fix critical bugs
  - [ ] Optimize based on feedback

### 11.2 Play Store Release
- **Tasks:**
  - [ ] Create Google Play Developer account
  - [ ] Create app store listing with:
    - [ ] App title & description
    - [ ] Screenshots
    - [ ] Privacy policy link
    - [ ] Feature graphics
  - [ ] Set pricing and distribution
  - [ ] Upload signed APK
  - [ ] Request review

### 11.3 Monitoring & Analytics
- **Tasks:**
  - [ ] Setup Firebase Analytics
  - [ ] Track key events:
    - [ ] User signup
    - [ ] Login
    - [ ] Timetable views
    - [ ] Search queries
    - [ ] Custom timetable creation
  - [ ] Setup Firebase Performance Monitoring
  - [ ] Create dashboard for metrics
  - [ ] Monthly review and optimization

### 11.4 Bug Fixes & Updates
- **Tasks:**
  - [ ] Monitor crash reports via Firebase Crashlytics
  - [ ] Fix bugs within 48 hours
  - [ ] Release patches monthly
  - [ ] Add new features based on feedback
  - [ ] Update documentation

---

## Dependency Management Summary

### Current Dependencies (from libs.versions.toml)
```toml
# Core
androidx-core-ktx = "1.17.0"
androidx-appcompat = "1.7.1"
material = "1.13.0"
androidx-activity = "1.11.0"
androidx-constraintlayout = "2.2.1"

# Firebase
firebase-auth = "24.0.1"
com.google.firebase:firebase-bom = "33.1.0"

# Auth
androidx-credentials = "1.5.0"
androidx-credentials-play-services-auth = "1.5.0"
googleid = "1.1.1"

# Testing
junit = "4.13.2"
androidx-junit = "1.3.0"
androidx-espresso-core = "3.7.0"
```

### Dependencies to Add
```kotlin
// Room Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Firebase Extensions
implementation("com.google.firebase:firebase-firestore-ktx:24.9.1")
implementation("com.google.firebase:firebase-storage-ktx:20.2.1")
implementation("com.google.firebase:firebase-messaging-ktx:23.2.1")
implementation("com.google.firebase:firebase-crashlytics-ktx:18.6.0")
implementation("com.google.firebase:firebase-analytics-ktx:21.5.0")

// Network & HTTP
implementation("com.squareup.okhttp3:okhttp:4.11.0")
implementation("com.google.code.gson:gson:2.10.1")

// Image Loading
implementation("com.github.bumptech.glide:glide:4.16.0")
kapt("com.github.bumptech.glide:compiler:4.16.0")

// Work Manager
implementation("androidx.work:work-runtime-ktx:2.8.1")

// Security
implementation("androidx.security:security-crypto:1.1.0-alpha06")

// RecyclerView & UI
implementation("androidx.recyclerview:recyclerview:1.3.2")
implementation("com.google.android.material:material:1.13.0")

// Location (for future use)
implementation("com.google.android.gms:play-services-location:21.0.1")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Testing
testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
testImplementation("androidx.test:core:1.5.0")
androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
```

---

## File Structure After Completion

```
app/src/main/java/com/example/fastable/
├── activities/
│   ├── SplashScreen.kt
│   ├── Login.kt
│   ├── Sign_Up.kt
│   ├── Home.kt
│   ├── Menu.kt
│   ├── Profile.kt
│   ├── CourseList.kt
│   ├── Add_Timetable.kt
│   ├── CustomTimetable.kt
│   ├── Notification.kt
│   └── AboutUs.kt
├── adapters/
│   ├── TimetableAdapter.kt
│   ├── CourseAdapter.kt
│   ├── NotificationAdapter.kt
│   └── CustomTimetableAdapter.kt
├── models/
│   ├── UserData.kt
│   ├── CourseSlot.kt
│   ├── FirestoreCourse.kt
│   ├── FirestoreTimetable.kt
│   └── Notification.kt
├── database/
│   ├── FASTableDatabase.kt
│   ├── entities/
│   │   ├── CourseEntity.kt
│   │   ├── TimetableEntity.kt
│   │   ├── CustomTimetableEntity.kt
│   │   ├── NotificationEntity.kt
│   │   └── SyncMetadataEntity.kt
│   └── dao/
│       ├── CourseDao.kt
│       ├── TimetableDao.kt
│       ├── CustomTimetableDao.kt
│       ├── NotificationDao.kt
│       └── SyncMetadataDao.kt
├── services/
│   ├── SyncManager.kt
│   ├── GoogleSheetsService.kt
│   ├── NetworkManager.kt
│   ├── CacheManager.kt
│   ├── NotificationManager.kt
│   └── MyFirebaseMessagingService.kt
├── utils/
│   ├── SessionManager.kt
│   ├── Constants.kt
│   ├── Extensions.kt
│   ├── TimeFormatter.kt
│   └── ValidationUtils.kt
├── repositories/
│   ├── TimetableRepository.kt
│   ├── UserRepository.kt
│   └── NotificationRepository.kt
├── viewmodels/ (Optional for MVVM)
│   ├── TimetableViewModel.kt
│   ├── UserViewModel.kt
│   └── NotificationViewModel.kt
└── App.kt
```

---

## Key Configuration Files

### 1. Update build.gradle.kts
- Add all dependencies listed above
- Add Kotlin kapt plugin configuration
- Enable viewBinding

### 2. Update AndroidManifest.xml
- Add INTERNET permission
- Add READ_EXTERNAL_STORAGE, CAMERA permissions
- Add POST_NOTIFICATIONS permission
- Register FCM service
- Configure activities

### 3. Firebase Console Setup
- Enable Firebase Authentication (Email/Password)
- Enable Firestore Database
- Enable Firebase Storage
- Enable Firebase Cloud Messaging
- Setup Cloud Functions
- Configure email templates
- Setup security rules

### 4. Google Cloud Setup
- Create API key for Google Sheets
- Configure OAuth (if needed)
- Setup Google Sheets file with timetable

---

## Success Metrics

### Phase 1-2
- [ ] Users can sign up with email verification
- [ ] Users can login with email verification check
- [ ] Profile data can be viewed and edited
- [ ] Profile picture upload works

### Phase 3-4
- [ ] Timetable displays correctly from Firestore
- [ ] Data syncs between Firebase and SQLite
- [ ] Home screen shows current day timetable
- [ ] Offline mode works with cached data

### Phase 5-6
- [ ] Course search works accurately
- [ ] Custom timetable can be created/edited/deleted
- [ ] Push notifications received on timetable changes
- [ ] Notification screen displays all notifications

### Phase 7-9
- [ ] App works seamlessly offline
- [ ] All manual tests pass
- [ ] No crashes in extended use
- [ ] Performance acceptable on low-end devices (API 24)

### Phase 10-11
- [ ] App published on Play Store
- [ ] <2% crash rate
- [ ] Average rating >4.0 stars
- [ ] <1000 reviews in first month (realistic for university app)

---

## Quick Reference: What to Do First

### Week 1 Priority
1. **Fix Sign_Up.kt syntax errors** - CRITICAL
2. **Fix Firebase email domain issue** - CRITICAL
3. **Test email verification flow**
4. **Create UserData model and Firestore integration**
5. **Implement session management**

### Week 2 Priority
1. **Enhance Login with email verification check**
2. **Develop Profile screen**
3. **Improve Menu navigation**

### Week 3 Priority
1. **Setup SQLite with Room**
2. **Create Firestore structure**
3. **Implement SyncManager**

### Key Milestones
- **End of Week 2:** Complete authentication flow
- **End of Week 4:** Working timetable display (online & offline)
- **End of Week 6:** Push notifications working
- **End of Week 9:** Ready for beta testing
- **End of Week 10:** Ready for Play Store release

---

## Notes for Developer

### Important Reminders
1. **Never commit Firebase credentials** - Use .gitignore
2. **Test on both online and offline modes** frequently
3. **Always handle null values** in Firestore data
4. **Implement proper error logging** for debugging
5. **Get user feedback early** - don't wait until Week 9
6. **Document API changes** for team collaboration

### Testing Strategy
- Unit test as you code
- Manual testing after each phase
- Beta testing with 20-50 real users before Play Store
- Monitor crash reports after release

### Performance Guidelines
- Timetable loading: <2 seconds
- Search results: <1 second
- Image upload: <5 seconds
- Sync operation: <10 seconds (in background)
- App startup: <3 seconds

---

## References & Documentation

- [Firebase Documentation](https://firebase.google.com/docs)
- [Android Architecture Components](https://developer.android.com/topic/architecture)
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [Firebase Cloud Functions](https://firebase.google.com/docs/functions)
- [Google Sheets API](https://developers.google.com/sheets/api)
- [Material Design](https://material.io/design)
- [Android Security Best Practices](https://developer.android.com/training/best-practices/security)

---

**Roadmap Last Updated:** December 1, 2025  
**Version:** 1.0  
**Next Review:** After Phase 2 completion

