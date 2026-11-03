package com.example.fastable

import com.example.fastable.data.models.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthAndProfileTest {

    @Test
    fun testRollNumberBatchExtraction() {
        fun extractBatchFromEmail(email: String): String {
            if (email.contains("@")) {
                val prefix = email.substringBefore("@").lowercase().trim()
                val match = Regex("^[a-z](\\d{2})\\d{4}$").find(prefix)
                if (match != null) {
                    return "20" + match.groupValues[1]
                }
            }
            return ""
        }

        assertEquals("2023", extractBatchFromEmail("i230553@isb.nu.edu.pk"))
        assertEquals("2021", extractBatchFromEmail("p210123@nu.edu.pk"))
        assertEquals("2024", extractBatchFromEmail("k249999@cfd.nu.edu.pk"))
        assertEquals("", extractBatchFromEmail("john.doe@gmail.com"))
        assertEquals("", extractBatchFromEmail("invalid"))
    }

    @Test
    fun testGoogleAccountPermissiveness() {
        fun isValidGoogleEmail(email: String): Boolean {
            return email.isNotEmpty() && email.contains("@") && email.contains(".")
        }

        // Standard Gmail accounts are now permitted
        assertTrue(isValidGoogleEmail("student@gmail.com"))
        // Multi-campus FAST university accounts are permitted
        assertTrue(isValidGoogleEmail("i230553@isb.nu.edu.pk"))
        assertTrue(isValidGoogleEmail("l220123@lhr.nu.edu.pk"))
        assertTrue(isValidGoogleEmail("k210456@khi.nu.edu.pk"))
        assertTrue(isValidGoogleEmail("user@nu.edu.pk"))
        // Empty or malformed emails are rejected
        assertFalse(isValidGoogleEmail(""))
        assertFalse(isValidGoogleEmail("not-an-email"))
    }

    @Test
    fun testProfileImageUrlProtocolDetection() {
        fun isRemoteImageUrl(url: String): Boolean {
            return url.startsWith("http", ignoreCase = true)
        }

        // Google profile photo URLs must be recognized as remote HTTP/HTTPS
        assertTrue(isRemoteImageUrl("https://lh3.googleusercontent.com/a/ACg8ocL_abc123"))
        assertTrue(isRemoteImageUrl("http://example.com/avatar.jpg"))
        // Local file paths must not be recognized as remote HTTP/HTTPS
        assertFalse(isRemoteImageUrl("/data/user/0/com.example.fastable/files/profile_images/123.jpg"))
        assertFalse(isRemoteImageUrl(""))
    }

    @Test
    fun testUserProfilePreservesGooglePhotoUrl() {
        val googlePhotoUrl = "https://lh3.googleusercontent.com/a/ACg8ocL_abc123"
        val profile = UserProfile(
            uid = "google_user_123",
            name = "Test Student",
            email = "student@gmail.com",
            batch = "2023",
            degree = "Computer Science",
            section = "A",
            profileImageUrl = googlePhotoUrl
        )

        assertEquals("google_user_123", profile.uid)
        assertEquals(googlePhotoUrl, profile.profileImageUrl)
        assertTrue(profile.profileImageUrl.startsWith("http"))
    }
}
