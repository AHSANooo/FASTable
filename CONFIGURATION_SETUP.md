
Document: courses (array field)
[
  {
    id: "CS-101",
    name: "Introduction to Programming",
    instructor: "Dr. Ahmed",
    credits: 3,
    color: "#FF5722"
  },
  ...
]

Subcollection: schedule
{
  docId: "CS-101-MWF"
  {
    courseId: "CS-101",
    dayOfWeek: "Monday",
    startTime: "09:00",
    endTime: "10:30",
    location: "Building-A, Room-101"
  },
  ...
}
```

**Collection: `/users/{userId}/`**

```
{
  name: "John Doe",
  email: "john@fastnu.edu.pk",
  batch: "2023",
  degree: "CS",
  section: "A",
  profilePicture: "gs://fastable-xxxxx.appspot.com/users/userId/profile.jpg",
  fcmToken: "token_xxxxxxxxxxxx",
  createdAt: timestamp,
  isEmailVerified: true,
  preferences: {
    notifications: true,
    theme: "light",
    language: "en"
  }
}
```

**Collection: `/customTimetables/{userId}/courses/`**

```
{
  id: "custom-1",
  name: "Study Group - DSA",
  day: "Tuesday",
  startTime: "14:00",
  endTime: "16:00",
  location: "Library",
  notes: "Group study session",
  createdAt: timestamp
}
```

---

## 💾 SQLite Database Schema

### Room Entity Definitions

**CourseEntity.kt**
```kotlin
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val instructor: String,
    val credits: Int,
    val color: String,
    val batchId: String,
    val lastUpdated: Long
)
```

**TimetableEntity.kt**
```kotlin
@Entity(
    tableName = "timetable",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TimetableEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val batchId: String,
    val lastUpdated: Long
)
```

**CustomTimetableEntity.kt**
```kotlin
@Entity(tableName = "custom_timetable")
data class CustomTimetableEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)
```

**SyncMetadataEntity.kt**
```kotlin
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val id: String = "metadata",
    val lastSyncTime: Long,
    val syncStatus: String, // "success", "failed", "in_progress"
    val lastError: String?
)
```

---

## 🌐 Google Sheets Setup

### Create Google Sheet

1. Go to [Google Sheets](https://sheets.google.com)
2. Create new spreadsheet: "FASTable-Timetable"
3. Share with service account (for API access)

### Sheet Format

**Headers (Row 1):**
```
Course Code | Course Name | Instructor | Day | Start Time | End Time | Location | Credits | Batch | Degree | Section
```

**Sample Data:**
```
CS-101 | Intro to Programming | Dr. Ahmed | Monday | 09:00 | 10:30 | A-101 | 3 | 2023 | CS | A
CS-102 | Data Structures | Dr. Fatima | Wednesday | 11:00 | 12:30 | A-102 | 3 | 2023 | CS | A
```

### Get API Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create new project: "FASTable"
3. Enable APIs:
   - Google Sheets API
   - Google Drive API
4. Create service account
5. Download JSON key file
6. Store securely (add to .gitignore)

---

## 🔄 Constants Configuration

**Create: `utils/Constants.kt`**

```kotlin
object Constants {
    // Firebase
    const val FIREBASE_PROJECT_ID = "fastable-xxxxx"
    
    // Firestore Collections
    const val USERS_COLLECTION = "users"
    const val TIMETABLES_COLLECTION = "timetables"
    const val CUSTOM_TIMETABLES_COLLECTION = "customTimetables"
    
    // Database
    const val DATABASE_NAME = "fastable_db"
    const val DATABASE_VERSION = 1
    
    // Preferences
    const val PREF_NAME = "fastable_prefs"
    const val PREF_USER_ID = "user_id"
    const val PREF_USER_EMAIL = "user_email"
    const val PREF_IS_LOGGED_IN = "is_logged_in"
    const val PREF_LAST_SYNC = "last_sync"
    
    // API
    const val GOOGLE_SHEETS_API_KEY = "YOUR_API_KEY"
    const val GOOGLE_SHEET_ID = "YOUR_SHEET_ID"
    
    // Time
    const val SYNC_INTERVAL_HOURS = 24L
    const val CACHE_EXPIRY_DAYS = 7
    
    // UI
    const val ANIMATION_DURATION_MS = 300
    const val TOAST_DURATION_SHORT = Toast.LENGTH_SHORT
    const val TOAST_DURATION_LONG = Toast.LENGTH_LONG
}
```

---

## ✅ Pre-Development Checklist

- [ ] Firebase project created
- [ ] google-services.json downloaded and placed in app/
- [ ] All Firebase services enabled
- [ ] Email template set to default
- [ ] Firestore collection structure planned
- [ ] SQLite schema finalized
- [ ] Google Sheets created and shared
- [ ] API credentials downloaded
- [ ] Security rules reviewed
- [ ] build.gradle.kts updated with all dependencies
- [ ] AndroidManifest.xml updated with permissions
- [ ] Constants.kt created with configuration
- [ ] Project synced with Gradle
- [ ] No build errors present

---

## 🚀 First Build Test

```bash
# 1. Open project in Android Studio
# 2. File → Sync Now (wait for gradle sync)
# 3. Build → Clean Project
# 4. Build → Rebuild Project
# 5. Run → Run 'app' (select emulator/device)
```

**Expected Result:**
- ✅ Gradle sync completes
- ✅ Project builds without errors
- ✅ App installs on emulator/device
- ✅ SplashScreen appears for 2-3 seconds
- ✅ Login screen appears

---

## 📱 Testing Devices

### Recommended
- **Minimum:** Android 7.0 (API 24) - as per project requirement
- **Target:** Android 12-13 (API 31-33) - current market
- **Test on:** Both emulator and physical device

### Emulator Setup
```
1. Android Studio → Device Manager
2. Create Device:
   - Model: Pixel 6 (API 33)
   - Name: Pixel6_API33
   - RAM: 2GB minimum
3. Launch emulator before running tests
```

---

## 🐛 Common Setup Issues & Solutions

| Issue | Solution |
|-------|----------|
| "google-services.json not found" | Verify file location: `app/google-services.json` |
| Gradle sync fails | Clear cache: File → Invalidate Caches → Restart |
| Dependencies not resolving | Check internet connection, run `gradlew clean` |
| SDK not installed | Android Studio → SDK Manager → Install API 24+ |
| Emulator won't start | Check VT-x enabled in BIOS, restart Android Studio |
| Firebase connection fails | Verify `google-services.json` content, check credentials |

---

## 📊 Configuration Verification

After setup, verify in code:

```kotlin
// In any Activity onCreate()
Log.d("CONFIG", "Package: ${packageName}")
Log.d("CONFIG", "API Level: ${Build.VERSION.SDK_INT}")
Log.d("CONFIG", "Firebase Initialized: ${FirebaseAuth.getInstance()}")
Log.d("CONFIG", "Firestore Available: ${Firebase.firestore}")
```

Expected output:
```
CONFIG: Package: com.example.fastable
CONFIG: API Level: 33
CONFIG: Firebase Initialized: com.google.firebase.auth.FirebaseAuth@xxxxx
CONFIG: Firestore Available: com.google.firebase.firestore.FirebaseFirestore@xxxxx
```

---

**Configuration Last Updated:** December 1, 2025  
**Status:** Ready for Development  
**Next:** Start with QUICK_START_GUIDE.md
# FASTable - Configuration & Setup Guide

## 🔧 Initial Project Setup

### Step 1: Project Structure Verification

Verify your project has the following structure:

```
FASTable/
├── app/
│   ├── build.gradle.kts           (Configured ✅)
│   ├── google-services.json       (From Firebase)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/fastable/
│       │   └── res/
│       └── test/
├── gradle/
│   └── libs.versions.toml         (Dependency versions)
├── settings.gradle.kts
├── build.gradle.kts
├── IMPLEMENTATION_ROADMAP.md      (New 📄)
├── QUICK_START_GUIDE.md           (New 📄)
├── TIMELINE_AND_DEPENDENCIES.md   (New 📄)
└── CONFIGURATION_SETUP.md         (New 📄)
```

### Step 2: Firebase Project Setup

**Create New Firebase Project:**

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Add project" or select existing project
3. Project name: "FASTable"
4. Enable Google Analytics (optional)

**Enable Services:**

```
Authentication
  ├── ✅ Enable Email/Password
  ├── ✅ Set email template to default
  └── ✅ Add authorized domains (if custom template)

Firestore Database
  ├── ✅ Create database in test mode initially
  ├── ✅ Select region: asia-south1 (closest to Pakistan)
  └── ⚠️ Set proper security rules before production

Cloud Storage
  ├── ✅ Create storage bucket
  └── ✅ Set storage rules

Cloud Messaging
  ├── ✅ Copy Server API Key
  └── ✅ Download service account JSON (for Cloud Functions)

Cloud Functions
  └── ✅ Enable billing (required for external calls)
```

**Authorized Domains (Settings → Authorized Domains):**

```
✅ localhost
✅ fastable.firebaseapp.com
✅ fastable.page.link (if using Dynamic Links)
```

### Step 3: Download Configuration Files

**get google-services.json:**

1. Firebase Console → Project Settings → General
2. Click "Google-services.json" download
3. Place in: `app/google-services.json`

**Verify file content:**

```json
{
  "project_info": {
    "project_number": "YOUR_PROJECT_NUMBER",
    "project_id": "fastable-xxxxx",
    "storage_bucket": "fastable-xxxxx.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:YOUR_APP_ID:android:xxxxxxx"
      },
      "android_client_info": {
        "package_name": "com.example.fastable"
      }
    }
  ]
}
```

### Step 4: Update gradle Configuration

**File: `gradle/libs.versions.toml`**

Verify all dependencies are present:

```toml
[versions]
# ... existing versions ...
firebaseAuth = "24.0.1"
firestore = "24.9.1"
storage = "20.2.1"
messaging = "23.2.1"
crashlytics = "18.6.0"
roomRuntime = "2.6.1"
# Add new versions

[libraries]
# Firebase
firebase-firestore-ktx = { group = "com.google.firebase", name = "firebase-firestore-ktx", version = "24.9.1" }
firebase-storage-ktx = { group = "com.google.firebase", name = "firebase-storage-ktx", version = "20.2.1" }
firebase-messaging-ktx = { group = "com.google.firebase", name = "firebase-messaging-ktx", version = "23.2.1" }
firebase-crashlytics-ktx = { group = "com.google.firebase", name = "firebase-crashlytics-ktx", version = "18.6.0" }
firebase-analytics-ktx = { group = "com.google.firebase", name = "firebase-analytics-ktx", version = "21.5.0" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version = "2.6.1" }
room-ktx = { group = "androidx.room", name = "room-ktx", version = "2.6.1" }
room-compiler = { group = "androidx.room", name = "room-compiler", version = "2.6.1" }

# HTTP & JSON
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version = "4.11.0" }
gson = { group = "com.google.code.gson", name = "gson", version = "2.10.1" }

# Image Loading
glide = { group = "com.github.bumptech.glide", name = "glide", version = "4.16.0" }
glide-compiler = { group = "com.github.bumptech.glide", name = "compiler", version = "4.16.0" }

# Background Tasks
work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version = "2.8.1" }

# Security
security-crypto = { group = "androidx.security", name = "security-crypto", version = "1.1.0-alpha06" }

# Coroutines
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "1.7.3" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version = "1.7.3" }
```

**File: `app/build.gradle.kts`**

Update with all required dependencies:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.fastable"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fastable"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    // Enable viewBinding
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation(libs.firebase.auth)
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // Network & JSON
    implementation(libs.okhttp)
    implementation(libs.gson)

    // Image Loading
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Background Tasks
    implementation(libs.work.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Security
    implementation(libs.security.crypto)

    // Circle Image View (for profile pictures)
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
}
```

### Step 5: Update AndroidManifest.xml

**Add Permissions:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Internet & Connectivity -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />

    <!-- Camera & Storage -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

    <!-- Notifications -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Background Tasks -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@drawable/app_logo_white"
        android:label="@string/app_name"
        android:roundIcon="@drawable/app_logo_white"
        android:supportsRtl="true"
        android:theme="@style/Theme.FASTable">

        <!-- Activities -->
        <activity
            android:name=".SplashScreen"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".Login"
            android:exported="false" />

        <activity
            android:name=".Sign_Up"
            android:exported="false" />

        <activity
            android:name=".Home"
            android:exported="false" />

        <activity
            android:name=".Menu"
            android:exported="false" />

        <activity
            android:name=".Profile"
            android:exported="false" />

        <activity
            android:name=".Notification"
            android:exported="false" />

        <activity
            android:name=".Add_Timetable"
            android:exported="false" />

        <activity
            android:name=".CustomTimetable"
            android:exported="false" />

        <activity
            android:name=".AboutUs"
            android:exported="false" />

        <activity
            android:name=".OtpActivity"
            android:exported="false" />

        <!-- Firebase Cloud Messaging Service -->
        <service
            android:name=".services.MyFirebaseMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>

        <!-- WorkManager Job Service (for background sync) -->
        <service
            android:name="androidx.work.impl.foreground.SystemForegroundService"
            android:foregroundServiceType="dataSync"
            tools:node="merge" />

    </application>

</manifest>
```

---

## 🔐 Firebase Security Rules

### Firestore Security Rules

**Path: Firestore → Rules tab**

```javascript
rules_version = '3';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Allow users to read/write their own profile data
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
    }

    // Allow authenticated users to read timetable (shared data)
    match /timetables/{document=**} {
      allow read: if request.auth != null;
      allow write: if false; // Only backend can write
    }

    // Allow users to read/write their own custom timetable
    match /customTimetables/{userId}/{document=**} {
      allow read, write: if request.auth.uid == userId;
    }

    // Default deny
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

### Cloud Storage Security Rules

**Path: Storage → Rules tab**

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    
    // Allow users to upload to their folder
    match /users/{userId}/profile {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId
                   && request.resource.size < 5 * 1024 * 1024  // 5MB max
                   && request.resource.contentType.matches('image/.*');
    }

    // Default deny
    match /{allPaths=**} {
      allow read, write: if false;
    }
  }
}
```

---

## 📧 Firebase Email Template Configuration

### Default Email Verification Template

**Path: Firebase Console → Authentication → Templates → Email Verification**

**Keep the default template for now:**
- Subject: "Verify your email for FASTable"
- Body: Contains verification link
- Link: Uses Firebase secure link automatically

**Do NOT customize until Week 2** - causes domain issues

---

## 🗂️ Firestore Collection Structure

### Design Collections

**Collection: `/timetables/{batchId}/{degreeId}/{sectionId}/`**

```
Document: metadata
{
  lastUpdated: timestamp,
  version: 1,
  courseCount: 45,
  synced: true
}

