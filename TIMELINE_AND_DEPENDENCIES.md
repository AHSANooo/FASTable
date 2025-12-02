# FASTable - Implementation Timeline & Dependencies

## 📊 Phase Overview & Timeline

```
Week 1-2: Authentication & Profile (Priority: CRITICAL)
├── ✅ Fix Sign_Up.kt syntax
├── ✅ Fix Firebase email domain
├── ✅ Email verification flow
├── ✅ Enhanced Login
├── ✅ User data model
├── ✅ Session management
├── ✅ SplashScreen
├── ✅ Profile screen
├── ✅ Menu navigation
└── ✅ AboutUs screen

Week 3-4: Timetable Backend & Display (Priority: HIGH)
├── ✅ SQLite setup with Room
├── ✅ Firestore structure design
├── ✅ SyncManager implementation
├── ✅ Google Sheets API integration
├── ✅ Home screen layout
├── ✅ TimetableAdapter
├── ✅ Offline data display
└── ✅ Sync status indicator

Week 5-6: Course Search & Notifications (Priority: MEDIUM)
├── ✅ Course list screen
├── ✅ Search functionality
├── ✅ Filter implementation
├── ✅ Custom timetable feature
├── ✅ Firebase Cloud Messaging setup
├── ✅ Notification display
├── ✅ Notification screen
└── ✅ Push notification logic

Week 7-8: Offline & Advanced Features (Priority: MEDIUM)
├── ✅ SQLite caching strategy
├── ✅ Connectivity management
├── ✅ Offline UI indicators
├── ✅ Conflict resolution
├── ✅ Cloud Functions for notifications
└── ✅ Automatic sync scheduling

Week 9: Testing & QA (Priority: HIGH)
├── ✅ Unit tests
├── ✅ UI tests
├── ✅ Manual testing checklist
├── ✅ Beta testing
├── ✅ Bug fixes
└── ✅ Performance optimization

Week 10: Release Preparation (Priority: HIGH)
├── ✅ Code optimization
├── ✅ Security hardening
├── ✅ Code quality review
├── ✅ Build optimization
└── ✅ Release configuration

Week 11+: Post-Release (Priority: ONGOING)
├── ⏳ Play Store release
├── ⏳ Monitoring & analytics
├── ⏳ Bug fixes & updates
└── ⏳ Feature requests
```

---

## 🔗 Feature Dependencies

```
User Authentication
    ↓
    └─→ Sign Up ✅
    └─→ Login ✅
    └─→ Email Verification ✅
    └─→ User Profile
         ↓
         └─→ Profile Picture Upload
         └─→ Edit User Data

Session Management
    ↓
    └─→ SharedPreferences
    └─→ Auto-login on restart
    └─→ Logout functionality

Database Layer (SQLite/Room)
    ↓
    ├─→ Course Table
    ├─→ Timetable Table
    ├─→ Custom Timetable Table
    ├─→ Notification Table
    └─→ Sync Metadata Table

Firestore Setup
    ↓
    ├─→ Collection: /timetables
    ├─→ Collection: /users
    └─→ Security Rules

Google Sheets API
    ↓
    └─→ Parse Data
    └─→ Upload to Firestore
    └─→ Sync to SQLite

Sync Manager
    ↓
    ├─→ Detect Internet
    ├─→ Fetch from Firestore
    ├─→ Save to SQLite
    ├─→ Handle Conflicts
    └─→ Update Timestamps

Timetable Display
    ↓
    ├─→ Home Screen
    ├─→ Timetable Adapter
    ├─→ Course Details View
    └─→ Current Day Filter

Search & Filter
    ↓
    ├─→ Course List Screen
    ├─→ Search Implementation
    └─→ Filter Options

Custom Timetable
    ↓
    ├─→ Add Custom Course
    ├─→ Edit Custom Course
    └─→ Delete Custom Course

Push Notifications
    ↓
    ├─→ Firebase Cloud Messaging
    ├─→ Notification Service
    ├─→ Notification Display
    ├─→ Notification Screen
    └─→ Cloud Functions (Backend)

Offline Support
    ↓
    ├─→ Cache Management
    ├─→ Network Detection
    ├─→ Offline UI Indicators
    └─→ Auto-sync When Online
```

---

## 📦 Dependency Installation Timeline

### Phase 1 (Week 1-2)
```gradle
// Already included
implementation("com.google.firebase:firebase-auth-ktx:22.1.0")
```

### Phase 3 (Week 3-4)
```gradle
// Add these
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

implementation("com.google.firebase:firebase-firestore-ktx:24.9.1")
implementation("com.google.firebase:firebase-storage-ktx:20.2.1")

// HTTP Client for Google Sheets
implementation("com.squareup.okhttp3:okhttp:4.11.0")
implementation("com.google.code.gson:gson:2.10.1")
```

### Phase 5-6 (Week 5-6)
```gradle
// Add these
implementation("com.google.firebase:firebase-messaging-ktx:23.2.1")

// Image loading for notifications & profile
implementation("com.github.bumptech.glide:glide:4.16.0")
kapt("com.github.bumptech.glide:compiler:4.16.0")

// RecyclerView if not included
implementation("androidx.recyclerview:recyclerview:1.3.2")
```

### Phase 7-8 (Week 7-8)
```gradle
// Add these
implementation("androidx.work:work-runtime-ktx:2.8.1")

// Coroutines for background tasks
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

### Phase 9 (Week 9)
```gradle
// Add these for testing
testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
testImplementation("androidx.test:core:1.5.0")
androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")

// Crashlytics
implementation("com.google.firebase:firebase-crashlytics-ktx:18.6.0")
```

### Phase 10 (Week 10)
```gradle
// Add these for security & optimization
implementation("androidx.security:security-crypto:1.1.0-alpha06")
implementation("com.google.firebase:firebase-analytics-ktx:21.5.0")
```

---

## 🎯 Key Milestones Checklist

### End of Week 1
- [ ] Sign_Up.kt syntax fixed ✅
- [ ] Firebase email domain fixed ✅
- [ ] User can sign up ✅
- [ ] Email verification sends ✅
- [ ] User can verify email ✅

### End of Week 2
- [ ] Login checks email verified ✅
- [ ] Profile screen works ✅
- [ ] Picture upload works ✅
- [ ] Session persists ✅
- [ ] Logout clears session ✅

### End of Week 4
- [ ] SQLite database ready ✅
- [ ] Firestore structure ready ✅
- [ ] Home screen displays timetable ✅
- [ ] Manual sync works ✅
- [ ] Offline mode works ✅

### End of Week 6
- [ ] Course search works ✅
- [ ] Custom timetable works ✅
- [ ] FCM setup complete ✅
- [ ] Push notifications received ✅
- [ ] Notification screen works ✅

### End of Week 8
- [ ] Offline sync queue working ✅
- [ ] Cloud Functions deployed ✅
- [ ] Auto-sync on schedule working ✅
- [ ] All edge cases handled ✅

### End of Week 9
- [ ] 95%+ test coverage ✅
- [ ] 0 crashes in 8+ hours testing ✅
- [ ] All manual tests pass ✅
- [ ] Beta testers onboarded ✅

### End of Week 10
- [ ] App optimized for release ✅
- [ ] Security audit complete ✅
- [ ] Code review passed ✅
- [ ] Signed APK ready ✅

### Week 11
- [ ] Play Store listing ready ✅
- [ ] App published ✅
- [ ] Monitoring set up ✅

---

## 🔍 Feature Checklist by Phase

### Phase 1: Authentication (Week 1-2)

**Sign Up**
- [ ] All fields validated
- [ ] Password strength check
- [ ] Email format validation
- [ ] Firebase user created
- [ ] Verification email sent
- [ ] Error handling
- [ ] Loading state

**Email Verification**
- [ ] Link in email works
- [ ] Verification completes
- [ ] User marked as verified
- [ ] Resend email option
- [ ] Error messages

**Login**
- [ ] Email/password validation
- [ ] Email verified check
- [ ] Session created
- [ ] Redirect to Home
- [ ] Error handling
- [ ] Forgot password option

**Profile**
- [ ] Display user data
- [ ] Edit all fields
- [ ] Save to Firestore
- [ ] Picture upload
- [ ] Picture delete
- [ ] Logout button

---

### Phase 2: Timetable Backend (Week 3-4)

**Database**
- [ ] Room entities created
- [ ] DAOs implemented
- [ ] Database created
- [ ] Migrations handled
- [ ] Queries optimized

**Firestore**
- [ ] Collection structure defined
- [ ] Security rules set
- [ ] Sample data loaded
- [ ] Queries tested
- [ ] Permissions working

**Sync Manager**
- [ ] Network check working
- [ ] Fetch logic implemented
- [ ] Save to SQLite working
- [ ] Error handling
- [ ] Retry logic
- [ ] Conflict resolution

**Home Screen**
- [ ] Display today's courses
- [ ] Show upcoming days
- [ ] Color coding
- [ ] Refresh button
- [ ] Offline indicator
- [ ] Last sync time

---

### Phase 3: Course Search (Week 5)

**Search**
- [ ] Search by course code
- [ ] Search by name
- [ ] Search by instructor
- [ ] Real-time results
- [ ] No results message
- [ ] Clear search

**Filter**
- [ ] Filter by batch
- [ ] Filter by degree
- [ ] Filter by section
- [ ] Filter by instructor
- [ ] Combined filters
- [ ] Reset filters

**Custom Timetable**
- [ ] Add custom course
- [ ] Edit custom course
- [ ] Delete custom course
- [ ] Time validation
- [ ] Duplicate check
- [ ] Appears in main timetable

---

### Phase 4: Notifications (Week 6)

**FCM Setup**
- [ ] Service implemented
- [ ] Token stored in Firestore
- [ ] Token refresh working
- [ ] Permissions granted
- [ ] Service registered

**Local Notifications**
- [ ] Notification created
- [ ] Channel created
- [ ] Icon set
- [ ] Sound playing
- [ ] Vibration working
- [ ] Click handling

**Notification Screen**
- [ ] List all notifications
- [ ] Show newest first
- [ ] Delete notification
- [ ] Mark as read
- [ ] Empty state
- [ ] Filter by type

**Change Detection**
- [ ] Detect course reschedule
- [ ] Detect course cancel
- [ ] Detect new course
- [ ] Create notification
- [ ] Send push notification
- [ ] Log changes

---

### Phase 5: Offline Support (Week 7-8)

**Caching**
- [ ] Cache valid check
- [ ] Cache expiration (7 days)
- [ ] Clear old cache
- [ ] Compression working
- [ ] Size management

**Connectivity**
- [ ] Detect online/offline
- [ ] Listen to changes
- [ ] Show indicator
- [ ] Auto-sync when online
- [ ] Queue actions offline

**UI Indicators**
- [ ] Offline banner
- [ ] Last sync time
- [ ] Sync in progress
- [ ] Sync failed message
- [ ] Retry button

**WorkManager Sync**
- [ ] Daily sync scheduled
- [ ] Periodic sync working
- [ ] Cancel sync
- [ ] Backoff strategy
- [ ] Network constraint

---

## 📱 Testing Strategy by Phase

### Phase 1: Manual Testing
- [ ] Sign up with valid data
- [ ] Sign up with invalid data
- [ ] Email received and verified
- [ ] Login with unverified email
- [ ] Login with verified email
- [ ] Session persistence

### Phase 2: Integration Testing
- [ ] Firestore connection
- [ ] SQLite storage
- [ ] Data sync accuracy
- [ ] Sync error handling
- [ ] Offline data display

### Phase 3: UI Testing
- [ ] Search results display
- [ ] Filter combinations
- [ ] Custom course add/edit/delete
- [ ] Time pickers work
- [ ] Validation messages

### Phase 4: Push Notification Testing
- [ ] FCM message received
- [ ] Notification displayed
- [ ] Click opens app
- [ ] Notification stored
- [ ] Notification screen loads

### Phase 5: Offline Testing
- [ ] App works completely offline
- [ ] Can view timetable offline
- [ ] Can search offline
- [ ] Sync when online
- [ ] No data loss

---

## 🚨 Critical Issues to Watch

### Week 1-2
🔴 **CRITICAL:** Firebase email domain mismatch  
🟡 **HIGH:** Sign_Up.kt syntax errors  
🟡 **HIGH:** Email not being received

### Week 3-4
🟡 **HIGH:** Database migrations failing  
🟡 **HIGH:** Firestore security rules blocking access  
🟡 **MEDIUM:** Google Sheets API key limits

### Week 5-6
🟡 **MEDIUM:** Search performance slow with large datasets  
🟡 **MEDIUM:** FCM token not updating  
🟡 **MEDIUM:** Notifications not displaying

### Week 7-8
🟡 **MEDIUM:** Sync conflicts corrupting data  
🟡 **MEDIUM:** WorkManager not triggering  
🟡 **LOW:** Cache growing too large

### Week 9
🟡 **HIGH:** Crash on low-end devices (API 24)  
🟡 **MEDIUM:** Memory leaks in image loading  
🟡 **LOW:** UI freezing during sync

---

## 💾 Backup & Version Control Strategy

### Git Workflow
```bash
# Main branches
main/             # Production ready code
develop/          # Development branch

# Feature branches (per phase)
feature/auth       # Week 1-2
feature/timetable  # Week 3-4
feature/search     # Week 5
feature/notifications # Week 6
feature/offline    # Week 7-8

# Hotfix branches
hotfix/email-issue
hotfix/sync-crash

# Commit often, push daily
# Tag at each phase end: v1.0.0-phase1
```

### Backup Schedule
- [ ] Daily: Commit to git
- [ ] Weekly: Push to GitHub
- [ ] Phase-end: Tag release
- [ ] Production: Backup Firestore data

---

## 📊 Success Metrics

### Performance Targets
| Metric | Target | Current |
|--------|--------|---------|
| App startup | <3 sec | - |
| Timetable load | <2 sec | - |
| Search results | <1 sec | - |
| Image upload | <5 sec | - |
| Sync operation | <10 sec | - |
| Email delivery | <5 min | - |

### Stability Targets
| Metric | Target | Current |
|--------|--------|---------|
| Crash rate | <2% | - |
| ANR rate | <1% | - |
| Test pass rate | >95% | - |
| Beta tester feedback | >4.0★ | - |

### User Adoption Targets
| Metric | Target | Current |
|--------|--------|---------|
| Beta signups | 50+ users | - |
| Play Store rating | >4.0★ | - |
| DAU retention (Day 7) | >30% | - |
| Feature usage | >80% | - |

---

## 📚 Documentation Requirements

### Code Documentation
- [ ] JavaDoc for all public methods
- [ ] README for each module
- [ ] Architecture decision records (ADRs)
- [ ] Database schema documentation
- [ ] API endpoint documentation

### User Documentation
- [ ] User guide PDF
- [ ] FAQ document
- [ ] Troubleshooting guide
- [ ] Video tutorials (optional)

### Developer Documentation
- [ ] Setup instructions
- [ ] Architecture overview
- [ ] Database schema
- [ ] Firebase configuration
- [ ] Google Sheets API setup

---

## 🎓 Learning Resources Required

- [ ] Android Room Database
- [ ] Firebase Firestore
- [ ] Firebase Cloud Functions
- [ ] Firebase Cloud Messaging
- [ ] Google Sheets API
- [ ] Android WorkManager
- [ ] RecyclerView & Adapters
- [ ] Coroutines & Async Programming
- [ ] Material Design
- [ ] Android Security Best Practices

---

**Chart Last Updated:** December 1, 2025  
**Status:** Ready for Implementation  
**Next Review:** End of Week 2

