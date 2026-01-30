# Dynamic Spreadsheet Link Configuration

## Overview
The app now fetches the Google Sheets link from Firebase Realtime Database instead of using a hardcoded value. This allows you to update the timetable source without releasing a new APK every semester.

## How It Works

1. **First Launch / Cache Expired**: App fetches spreadsheet link from Firebase
2. **Cache Valid (< 6 days)**: App uses locally cached spreadsheet ID
3. **Firebase Unavailable**: App falls back to the hardcoded ID in `GoogleSheetsConfig.kt`

## Firebase Setup

### 1. Update Security Rules

Add the `config` section to your existing rules:

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "$uid === auth.uid",
        ".write": "$uid === auth.uid"
      }
    },
    "config": {
      ".read": true,
      ".write": false
    }
  }
}
```

### 2. Add the Spreadsheet Link

In your Firebase Realtime Database, create this structure:

```json
{
  "config": {
    "spreadsheet_link": "YOUR_GOOGLE_SHEETS_LINK_HERE"
  }
}
```

**You can paste either:**

✅ **Full URL** (recommended - easier to copy/paste):
```
https://docs.google.com/spreadsheets/d/1ZQJqdArlwCS965uw4sbJrB6j8rEPfZerMT7X8qkXSzY/edit?gid=1882612924#gid=1882612924
```

✅ **Just the ID**:
```
1ZQJqdArlwCS965uw4sbJrB6j8rEPfZerMT7X8qkXSzY
```

The app will automatically extract the spreadsheet ID from the URL!

### Example Database Structure:

```
├── config
│   └── spreadsheet_link: "https://docs.google.com/spreadsheets/d/1ZQJqdArlwCS965uw4sbJrB6j8rEPfZerMT7X8qkXSzY/edit"
├── users
│   └── [user_uid]
│       └── ... user data ...
```

## Updating for New Semester

When a new semester starts and you have a new Google Sheets timetable:

1. **Share the new sheet** with the service account email:
   ```
   timetable-bot-876@time-table-project-450013.iam.gserviceaccount.com
   ```
   (Give it "Viewer" access)

2. **Go to Firebase Console** → Realtime Database

3. **Update** `config/spreadsheet_link` with the new URL (just copy-paste the full link!)

4. The app will automatically use the new spreadsheet within 6 days (or immediately for new users)

## Code Files

- `SpreadsheetConfigManager.kt` - Manages fetching, URL parsing, and caching
- `FASTableApplication.kt` - Initializes the config manager with fallback ID
- `GoogleSheetsService.kt` - Uses dynamic spreadsheet ID

## Configuration

To change the cache expiration time, edit `SpreadsheetConfigManager.kt`:

```kotlin
// Cache expiration: 6 days in milliseconds
private const val CACHE_EXPIRATION_MS = 6L * 24 * 60 * 60 * 1000
```

## Fallback Behavior

If Firebase is unreachable:
1. Uses cached ID if available (even if expired)
2. Falls back to hardcoded ID in `GoogleSheetsConfig.kt`

This ensures the app always works, even without internet connectivity (for cached data).

## Supported URL Formats

The app can extract the spreadsheet ID from any of these formats:

| Format | Example |
|--------|---------|
| Full URL with params | `https://docs.google.com/spreadsheets/d/1ABC123/edit?gid=0#gid=0` |
| Full URL simple | `https://docs.google.com/spreadsheets/d/1ABC123/edit` |
| Short URL | `https://docs.google.com/spreadsheets/d/1ABC123` |
| Just ID | `1ABC123` |
