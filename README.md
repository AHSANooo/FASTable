# FASTable 📱⚡

[![License: MIT](https://img.shields.io/badge/License-MIT-orange.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Material 3](https://img.shields.io/badge/UI-Material%203-purple.svg)](https://m3.material.io)

**FASTable** is the official timetable and campus utility companion for FAST NUCES students. Designed with modern Material 3 design principles, offline-first caching, and university Google authentication to give students an effortless, instant overview of their classes, faculty offices, free rooms, and schedules.

🌐 Website: [www.fastable.site](https://www.fastable.site/)  
🔗 Clash Detector: [clashes-detector.vercel.app](https://clashes-detector.vercel.app/)

---

## ✨ Features

- **⚡ Instant Timetable Sync & Smart Caching**: Room SQLite-backed offline-first caching with a 2-hour TTL and manual pull-to-refresh. Timetables load in milliseconds.
- **🎨 Brand Identity & Adaptive Dark Mode**: High-contrast Vibrant Orange (`#FF6D05`) and Deep Navy (`#0A2756`) Material 3 theming with full adaptive day/night mode support.
- **🔑 1-Tap University Google Sign-In**: Instant domain-restricted sign-in (`@isb.nu.edu.pk` and `@nu.edu.pk`) that automatically retrieves student name, email, and Google profile picture without passwords.
- **📋 Unified Schedule Preview**: Custom course selection includes a floating bottom dock, expandable course chips with quick removal (`✕`), and a combined preview of newly selected and existing dashboard classes.
- **🏷️ Card Section Signifiers & 3-Dot Menu**: Dashboard class cards show section badges (e.g. `BCS-4A`), location, time, session type, and a 3-dot overflow menu with confirmation to remove.
- **🔄 Automated GitHub Releases Updater**: Background check against the latest GitHub release to notify users and download updates seamlessly.
- **🏢 Campus Utilities**: Faculty offices directory with 1-tap email, real-time free rooms finder, and campus block map.

---

## 🏗️ Architecture & Tech Stack

- **Language**: Kotlin 2.0
- **UI Architecture**: MVVM (Model-View-ViewModel) with Android Architecture Components
- **Local Persistence**: Room SQLite Database (`AppDatabase`)
- **Remote Services**: Google Sheets API v4 (Service Account) & Firebase Realtime Database
- **Authentication**: Android Credential Manager / Google Sign-In & Firebase Auth
- **Asynchronous**: Kotlin Coroutines & Flow
- **Image Loading**: Picasso & Glide
- **Design System**: Material Design 3 (Material Components 1.13)

---

## 🚀 Building & Running Locally

### Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 17 or Android Studio JBR
- Android SDK 36 (compileSdk) / MinSdk 26

### Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/AHSANooo/FASTable.git
   cd FASTable
   ```
2. Open the project in Android Studio.
3. Build the debug APK:
   ```powershell
   ./gradlew assembleDebug
   ```

---

## 👥 Contributors

- **Muhammad Ahsan** ([LinkedIn](https://www.linkedin.com/in/muhammad-ahsan-7612701a7) • i230553@isb.nu.edu.pk)
- **Ali Ahmad Riaz** ([LinkedIn](https://www.linkedin.com/in/aliahmadriaz) • i232528@isb.nu.edu.pk)
- **Farzeen Khan Tareen** ([LinkedIn](https://www.linkedin.com/in/muhammad-farzeen-khan-tareen) • i230721@isb.nu.edu.pk)

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
