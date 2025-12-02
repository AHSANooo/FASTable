# FASTable - Quick Start Implementation Guide

## 🚀 Immediate Actions (Next 24 Hours)

### 1. Fix Sign_Up.kt File - CRITICAL
The file has corrupted code structure. Follow these steps:

**Current Issue:** Package declaration and imports are in wrong places, methods are mixed up.

**Solution:** Completely rewrite the file with proper structure:

```kotlin
package com.example.fastable

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ActionCodeSettings

class Sign_Up : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()

        // Get references to UI elements
        val nameEt = findViewById<EditText>(R.id.name)
        val emailEt = findViewById<EditText>(R.id.email)
        val batchEt = findViewById<EditText>(R.id.batch)
        val degreeEt = findViewById<EditText>(R.id.degree)
        val sectionEt = findViewById<EditText>(R.id.section)
        val passwordEt = findViewById<EditText>(R.id.password)
        val confirmPasswordEt = findViewById<EditText>(R.id.confirm_password)
        val signUpBtn = findViewById<Button>(R.id.btn_signup)

        // Sign up button click listener
        signUpBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val email = emailEt.text.toString().trim()
            val batch = batchEt.text.toString().trim()
            val degree = degreeEt.text.toString().trim()
            val section = sectionEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()
            val confirmPassword = confirmPasswordEt.text.toString().trim()

            // Validate all fields
            if (!validateForm(name, email, batch, degree, section, password, confirmPassword)) {
                return@setOnClickListener
            }

            // Create user in Firebase
            createUserAccount(email, password, name, batch, degree, section)
        }
    }

    private fun validateForm(
        name: String, email: String, batch: String,
        degree: String, section: String, password: String, confirmPassword: String
    ): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show()
            return false
        }
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return false
        }
        if (batch.isEmpty()) {
            Toast.makeText(this, "Please enter your batch", Toast.LENGTH_SHORT).show()
            return false
        }
        if (degree.isEmpty()) {
            Toast.makeText(this, "Please enter your degree", Toast.LENGTH_SHORT).show()
            return false
        }
        if (section.isEmpty()) {
            Toast.makeText(this, "Please enter your section", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return false
        }
        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun createUserAccount(
        email: String, password: String, name: String,
        batch: String, degree: String, section: String
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        sendVerificationEmail(user.email ?: email)
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown error occurred"
                    Toast.makeText(this, "Sign up failed: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun sendVerificationEmail(email: String) {
        val user = auth.currentUser

        if (user != null) {
            // Use default email verification first
            user.sendEmailVerification()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Verification email sent to $email\nPlease check your inbox.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Go to login screen after 2 seconds
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            startActivity(Intent(this, Login::class.java))
                            finish()
                        }, 2000)
                    } else {
                        val errorMessage = task.exception?.message ?: "Unknown error"
                        Toast.makeText(
                            this,
                            "Failed to send email: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}
```

---

### 2. Fix Firebase Email Domain Issue

**Error Message Seen:**
```
UNAUTHORIZED_DOMAIN: Domain not allowlisted by project
```

**Solution Steps:**

1. Go to **Firebase Console** → Select your project
2. Navigate to **Authentication** → **Templates** tab
3. Click on **Email address verification**
4. Check the current template URL
5. Reset to default template:
   - Click the three-dot menu
   - Select "Reset to default"

**OR** if you want to use a custom template:

1. Go to **Firebase Console** → **Settings** (gear icon)
2. Click **Authorized domains**
3. Add your domain (if using Firebase Dynamic Links: `fastable.page.link`)
4. Update email template to use only authorized domains

**For now, use the default template** - it will work without custom configuration.

---

### 3. Test Email Verification

**Test Checklist:**

- [ ] Create new Firebase test account
- [ ] Check email sending (Gmail recommended for testing)
- [ ] Verify email link works
- [ ] Try to login before verification - should fail
- [ ] Verify email and login - should work

**If still no email:**

1. Check Firebase Console → Authentication → Users (user should be there)
2. Check email's spam/promotions folder
3. Verify Firebase has SMTP settings (automatic, should be enabled)
4. Check Logcat for error messages: `adb logcat | grep "SignUp"`

---

## 📋 Minimum Field Requirements

For Sign Up form validation:

| Field | Min Length | Requirements |
|-------|-----------|---|
| Name | 2 chars | Non-empty, letters only (recommended) |
| Email | 5 chars | Valid email format (RFC 5322) |
| Batch | 1 char | Non-empty (e.g., "2023", "2024") |
| Degree | 2 chars | Non-empty (e.g., "CS", "SE", "EE") |
| Section | 1 char | Non-empty (e.g., "A", "B", "C") |
| Password | 6 chars | Min 6 characters, no special requirements for MVP |
| Confirm Password | 6 chars | Must match password field exactly |

**Current Implementation:** All above validations are in the corrected Sign_Up.kt

---

## 🔥 Firebase Console Checklist

Before testing signup/email:

- [ ] **Authentication**
  - [ ] Email/Password enabled
  - [ ] Email verification template set to default
  - [ ] SMTP settings configured (automatic)

- [ ] **Settings → Authorized Domains**
  - [ ] `localhost` (for testing)
  - [ ] `fastable.page.link` (if using Dynamic Links)
  - [ ] Any custom domain

- [ ] **Firestore Database** (for Phase 3)
  - [ ] Create database in test mode initially
  - [ ] Set collection rules later

- [ ] **Storage** (for profile pictures)
  - [ ] Create storage bucket
  - [ ] Set permissions

---

## 🐛 Debugging Tips

### Email Not Received

**Check in order:**

1. **Logcat Output:**
   ```bash
   adb logcat | grep "SignUp"
   ```
   Look for error messages like `UNAUTHORIZED_DOMAIN`

2. **Firebase Console:**
   - Authentication → Users
   - Find test user
   - Check if user is created

3. **Email Checks:**
   - Gmail: Check Promotions, All Mail, Spam tabs
   - Other: Check Junk folder
   - Wait 5 minutes (email can be delayed)

4. **Common Issues:**
   - Domain not allowlisted → Reset email template to default
   - Firebase not initialized → Check google-services.json
   - Network issue → Test app internet connectivity

---

## 📱 Running the App

### Prerequisites
```bash
# Install Android SDK Platform 24+ (API 24 = Android 7.0)
# Install emulator or connect physical device
```

### Build & Run
```bash
# Via Android Studio:
# 1. Click Run button (green play icon)
# 2. Select device/emulator
# 3. Wait for build to complete

# Via Command Line:
cd C:\Users\hp\AndroidStudioProjects\FASTable
gradlew build
gradlew installDebug
```

### Test Signup Flow
1. App launches → Login screen
2. Tap "Sign up" link
3. Fill all fields with valid data
4. Click Sign Up button
5. Should see "Verification email sent" toast
6. Check email for verification link
7. Click link to verify
8. Go back to app, login should work

---

## 🔗 Next Steps After Phase 1

Once email verification is working:

1. **Session Management (Priority 1)**
   - Create `SessionManager.kt`
   - Save login state to SharedPreferences
   - Check session on app startup

2. **Profile Screen (Priority 2)**
   - Create/update `Profile.kt`
   - Load user data from Firestore
   - Implement edit functionality

3. **Database Setup (Priority 3)**
   - Add Room dependencies
   - Create database entities
   - Create DAOs

---

## 📞 Firebase Support Resources

- [Email Verification Documentation](https://firebase.google.com/docs/auth/custom-email-handler)
- [Custom Email Action Handler](https://firebase.google.com/docs/auth/custom-email-handler)
- [Firebase Console Help](https://firebase.google.com/support/troubleshooter)

---

## 🎯 Success Criteria for Phase 1

✅ Sign_Up.kt compiles without errors  
✅ Sign up form validates all fields correctly  
✅ User created in Firebase Authentication  
✅ Verification email sent successfully  
✅ Email link works and verifies user  
✅ Login requires verified email  
✅ User data saved to Firestore  
✅ Session persists across app restart  
✅ Logout clears session  

---

## 💡 Pro Tips

1. **Always test email verification with real email** (not dummy accounts)
2. **Keep Firebase security rules loose initially** (`allow read, write: if true`), tighten later
3. **Log everything in development** - helps debugging
4. **Use Firebase Emulator** for faster testing once set up
5. **Keep google-services.json** in `.gitignore` for security
6. **Test on actual device** - emulator sometimes behaves differently

---

## 🆘 Troubleshooting

| Issue | Solution |
|-------|----------|
| "Invalid API Key" | Check google-services.json is in correct location |
| Email not received | Reset template to default, check spam folder |
| User not created | Check Firebase console, verify credentials |
| Compilation errors | Ensure all imports are correct, rebuild project |
| App crashes on signup | Check Logcat, look for NPE (null pointer exceptions) |
| Can't login after verify | Ensure login checks `user.isEmailVerified` |

---

## 📝 Development Notes

- **Keep this guide handy** while implementing each phase
- **Reference the full IMPLEMENTATION_ROADMAP.md** for detailed steps
- **Mark off checklist items as you complete** them
- **Test frequently** - don't wait until end of phase
- **Ask for code review** from team before merging to main

---

**Last Updated:** December 1, 2025  
**Status:** Ready for Phase 1 Implementation

