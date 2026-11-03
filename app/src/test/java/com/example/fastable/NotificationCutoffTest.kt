package com.example.fastable

import com.example.fastable.utils.NotificationScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Comprehensive unit tests for Milestone M1 (Backend Notification Scheduling Cutoff).
 *
 * Verifies:
 * - Canonical time bounds: 08:00 AM (480 mins) to 05:15 PM (1035 mins).
 * - Allowed notification window returns true strictly for Monday-Saturday between 08:00 and 17:15.
 * - Exact boundary conditions: 08:00, 08:15, 12:00, 17:15 allowed; 07:59, 17:16, 20:00, 20:05 rejected.
 * - Sunday is strictly blocked across all hours.
 * - Session scheduling rejects any session on Sunday or starting past 17:15.
 * - Reminder trigger window rejects trigger times outside 08:00 AM to 05:15 PM.
 */
class NotificationCutoffTest {

    private fun createCalendar(dayOfWeek: Int, hourOfDay: Int, minute: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun testCanonicalConstants() {
        assertEquals("Canonical min class hour must be 8 (08:00 AM)", 8, NotificationScheduler.MIN_CLASS_HOUR)
        assertEquals("Canonical min class minute must be 0", 0, NotificationScheduler.MIN_CLASS_MINUTE)
        assertEquals("Canonical max cutoff hour must be 17 (05:00 PM)", 17, NotificationScheduler.MAX_CUTOFF_HOUR)
        assertEquals("Canonical max cutoff minute must be 15", 15, NotificationScheduler.MAX_CUTOFF_MINUTE)

        assertEquals("Min class minutes of day must be 480 (8 * 60)", 480, NotificationScheduler.MIN_CLASS_MINUTES_OF_DAY)
        assertEquals("Max cutoff minutes of day must be 1035 (17 * 60 + 15)", 1035, NotificationScheduler.MAX_CUTOFF_MINUTES_OF_DAY)
    }

    @Test
    fun testAllowedWindow_BoundaryAllowedTimesOnWeekdays() {
        // Monday 08:00 AM (480 mins) - exact lower boundary
        val mon8am = createCalendar(Calendar.MONDAY, 8, 0)
        assertTrue("Monday 08:00 AM must be allowed", NotificationScheduler.isWithinAllowedNotificationWindow(mon8am))

        // Tuesday 08:15 AM (495 mins) - valid morning reminder time
        val tue815am = createCalendar(Calendar.TUESDAY, 8, 15)
        assertTrue("Tuesday 08:15 AM must be allowed", NotificationScheduler.isWithinAllowedNotificationWindow(tue815am))

        // Wednesday 12:00 PM (720 mins) - midday class
        val wed12pm = createCalendar(Calendar.WEDNESDAY, 12, 0)
        assertTrue("Wednesday 12:00 PM must be allowed", NotificationScheduler.isWithinAllowedNotificationWindow(wed12pm))

        // Thursday 03:30 PM (930 mins) - afternoon class
        val thu330pm = createCalendar(Calendar.THURSDAY, 15, 30)
        assertTrue("Thursday 03:30 PM must be allowed", NotificationScheduler.isWithinAllowedNotificationWindow(thu330pm))

        // Friday 05:15 PM (1035 mins) - exact upper cutoff boundary
        val fri515pm = createCalendar(Calendar.FRIDAY, 17, 15)
        assertTrue("Friday 05:15 PM must be allowed", NotificationScheduler.isWithinAllowedNotificationWindow(fri515pm))

        // Saturday 05:15 PM (1035 mins) - Saturday boundary allowed
        val sat515pm = createCalendar(Calendar.SATURDAY, 17, 15)
        assertTrue("Saturday 05:15 PM must be allowed", NotificationScheduler.isWithinAllowedNotificationWindow(sat515pm))
    }

    @Test
    fun testAllowedWindow_BoundaryRejectedTimesOnWeekdays() {
        // Monday 07:59 AM (479 mins) - 1 minute before morning window
        val mon759am = createCalendar(Calendar.MONDAY, 7, 59)
        assertFalse("Monday 07:59 AM must be rejected (before 08:00 AM)", NotificationScheduler.isWithinAllowedNotificationWindow(mon759am))

        // Tuesday 05:16 PM (1036 mins) - 1 minute after 5:15 PM hard cutoff
        val tue516pm = createCalendar(Calendar.TUESDAY, 17, 16)
        assertFalse("Tuesday 05:16 PM must be rejected (after 05:15 PM cutoff)", NotificationScheduler.isWithinAllowedNotificationWindow(tue516pm))

        // Wednesday 06:00 PM (1080 mins) - evening rejected
        val wed6pm = createCalendar(Calendar.WEDNESDAY, 18, 0)
        assertFalse("Wednesday 06:00 PM must be rejected", NotificationScheduler.isWithinAllowedNotificationWindow(wed6pm))

        // Thursday 08:00 PM (1200 mins) - night rejected
        val thu8pm = createCalendar(Calendar.THURSDAY, 20, 0)
        assertFalse("Thursday 08:00 PM must be rejected", NotificationScheduler.isWithinAllowedNotificationWindow(thu8pm))

        // Friday 08:05 PM (1205 mins) - old university threshold must be rejected
        val fri805pm = createCalendar(Calendar.FRIDAY, 20, 5)
        assertFalse("Friday 08:05 PM must be rejected", NotificationScheduler.isWithinAllowedNotificationWindow(fri805pm))

        // Saturday 11:59 PM (1439 mins) - late night rejected
        val sat1159pm = createCalendar(Calendar.SATURDAY, 23, 59)
        assertFalse("Saturday 11:59 PM must be rejected", NotificationScheduler.isWithinAllowedNotificationWindow(sat1159pm))

        // Tuesday 00:00 AM (midnight) rejected
        val tueMidnight = createCalendar(Calendar.TUESDAY, 0, 0)
        assertFalse("Tuesday midnight must be rejected", NotificationScheduler.isWithinAllowedNotificationWindow(tueMidnight))
    }

    @Test
    fun testAllowedWindow_SundayStrictlyRejected() {
        // Sunday morning
        val sun8am = createCalendar(Calendar.SUNDAY, 8, 0)
        assertFalse("Sunday 08:00 AM must be strictly rejected", NotificationScheduler.isWithinAllowedNotificationWindow(sun8am))

        // Sunday midday
        val sun12pm = createCalendar(Calendar.SUNDAY, 12, 0)
        assertFalse("Sunday 12:00 PM must be strictly rejected", NotificationScheduler.isWithinAllowedNotificationWindow(sun12pm))

        // Sunday cutoff time
        val sun515pm = createCalendar(Calendar.SUNDAY, 17, 15)
        assertFalse("Sunday 05:15 PM must be strictly rejected", NotificationScheduler.isWithinAllowedNotificationWindow(sun515pm))

        // Sunday evening
        val sun8pm = createCalendar(Calendar.SUNDAY, 20, 0)
        assertFalse("Sunday 08:00 PM must be strictly rejected", NotificationScheduler.isWithinAllowedNotificationWindow(sun8pm))
    }

    @Test
    fun testAllowedWindow_AllDaysMondayThroughSaturdaySupported() {
        val days = listOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY
        )

        for (day in days) {
            val cal = createCalendar(day, 10, 0)
            assertTrue("Class hours on day $day must be allowed", NotificationScheduler.isWithinAllowedNotificationWindow(cal))
        }
    }

    @Test
    fun testSessionTimeInMillis_ClassHoursAndSundayFiltering() {
        // Valid class times on weekdays
        assertNotNull("Monday 08:30 AM should be valid", NotificationScheduler.getSessionTimeInMillis("Monday", "08:30 AM - 10:00 AM"))
        assertNotNull("Tuesday 12:00 PM should be valid", NotificationScheduler.getSessionTimeInMillis("Tuesday", "12:00 PM - 01:30 PM"))
        assertNotNull("Wednesday 08:00 AM should be valid", NotificationScheduler.getSessionTimeInMillis("Wednesday", "08:00 AM - 09:30 AM"))
        assertNotNull("Friday 03:45 PM should be valid", NotificationScheduler.getSessionTimeInMillis("Friday", "03:45 PM - 05:15 PM"))
        assertNotNull("Saturday 08:30 AM should be valid", NotificationScheduler.getSessionTimeInMillis("Saturday", "08:30 AM - 10:00 AM"))

        // Classes starting past 5:15 PM hard cutoff must return null
        assertNull("Thursday 05:16 PM session must be rejected", NotificationScheduler.getSessionTimeInMillis("Thursday", "05:16 PM - 06:45 PM"))
        assertNull("Friday 06:00 PM session must be rejected", NotificationScheduler.getSessionTimeInMillis("Friday", "06:00 PM - 07:30 PM"))
        assertNull("Friday 08:05 PM session must be rejected", NotificationScheduler.getSessionTimeInMillis("Friday", "08:05 PM - 09:35 PM"))

        // Sessions before 08:00 AM must return null
        assertNull("Monday 07:30 AM session must be rejected", NotificationScheduler.getSessionTimeInMillis("Monday", "07:30 AM - 09:00 AM"))

        // Sunday sessions must ALWAYS return null
        assertNull("Sunday 08:30 AM session must be rejected", NotificationScheduler.getSessionTimeInMillis("Sunday", "08:30 AM - 10:00 AM"))
        assertNull("Sunday 12:00 PM session must be rejected", NotificationScheduler.getSessionTimeInMillis("Sunday", "12:00 PM - 01:30 PM"))
        assertNull("Sunday 05:15 PM session must be rejected", NotificationScheduler.getSessionTimeInMillis("Sunday", "05:15 PM - 06:45 PM"))
    }

    @Test
    fun testReminderTriggerTimeEvaluation() {
        // Class at 08:30 AM -> 15 min reminder trigger is 08:15 AM (495 mins) -> Allowed
        val trigger815 = createCalendar(Calendar.MONDAY, 8, 15)
        assertTrue("08:15 AM reminder trigger for 08:30 AM class must be allowed", NotificationScheduler.isWithinAllowedNotificationWindow(trigger815))

        // Class at 08:15 AM -> 15 min reminder trigger is 08:00 AM (480 mins) -> Allowed (lower boundary)
        val trigger800 = createCalendar(Calendar.MONDAY, 8, 0)
        assertTrue("08:00 AM reminder trigger for 08:15 AM class must be allowed", NotificationScheduler.isWithinAllowedNotificationWindow(trigger800))

        // Class at 08:00 AM -> 15 min reminder trigger is 07:45 AM (465 mins) -> Rejected (before 08:00 AM)
        val trigger745 = createCalendar(Calendar.MONDAY, 7, 45)
        assertFalse("07:45 AM reminder trigger for 08:00 AM class must be rejected", NotificationScheduler.isWithinAllowedNotificationWindow(trigger745))

        // Class at 05:15 PM -> 15 min reminder trigger is 05:00 PM (1020 mins) -> Allowed
        val trigger500 = createCalendar(Calendar.MONDAY, 17, 0)
        assertTrue("05:00 PM reminder trigger for 05:15 PM class must be allowed", NotificationScheduler.isWithinAllowedNotificationWindow(trigger500))

        // Late session trigger at 05:16 PM -> Rejected
        val trigger516 = createCalendar(Calendar.MONDAY, 17, 16)
        assertFalse("Trigger at 05:16 PM must be rejected", NotificationScheduler.isWithinAllowedNotificationWindow(trigger516))
    }
}
