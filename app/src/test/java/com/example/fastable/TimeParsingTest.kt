package com.example.fastable

import com.example.fastable.utils.TimeParser
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeParsingTest {

    @Test
    fun testParseTimeSlot_Ranges() {
        // Range with PM at the end - should be parsed as AM start
        assertEquals("10:30 - 01:00 PM should be 10:30 AM (630 mins)", 630L, TimeParser.parseTimeSlot("10:30 - 01:00 PM"))
        
        // Range without PM - should be parsed as AM start
        assertEquals("08:30 - 09:50 should be 08:30 AM (510 mins)", 510L, TimeParser.parseTimeSlot("08:30 - 09:50"))
        
        // Range starting in PM
        assertEquals("01:00 - 02:20 should be 01:00 PM (780 mins)", 780L, TimeParser.parseTimeSlot("01:00 - 02:20"))
    }

    @Test
    fun testParseTimeSlot_UniversitySchedule() {
        // 8:30 AM (University convention: 8:30 is AM)
        assertEquals("08:30 should be 8:30 AM (510 mins)", 510L, TimeParser.parseTimeSlot("08:30"))
        
        // 8:05 PM (University convention: 8:05 is PM as per user feedback)
        assertEquals("08:05 should be 8:05 PM (1205 mins)", 1205L, TimeParser.parseTimeSlot("08:05"))
        
        // 12:00 (University convention: 12:00 is PM)
        assertEquals("12:00 should be 12:00 PM (720 mins)", 720L, TimeParser.parseTimeSlot("12:00"))
        
        // 11:30 (University convention: 11:30 is AM)
        assertEquals("11:30 should be 11:30 AM (690 mins)", 690L, TimeParser.parseTimeSlot("11:30"))
    }

    @Test
    fun testParseTimeSlot_ExplicitAmPm() {
        assertEquals("10:30 AM should be 630 mins", 630L, TimeParser.parseTimeSlot("10:30 AM"))
        assertEquals("10:30 PM should be 1350 mins", 1350L, TimeParser.parseTimeSlot("10:30 PM"))
        assertEquals("12:00 AM should be 0 mins", 0L, TimeParser.parseTimeSlot("12:00 AM"))
        assertEquals("12:00 PM should be 720 mins", 720L, TimeParser.parseTimeSlot("12:00 PM"))
    }
}
