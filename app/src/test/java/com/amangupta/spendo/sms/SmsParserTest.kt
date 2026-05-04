package com.amangupta.spendo.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParserTest {

    @Test
    fun testDebitMessage() {
        val msg = "Debited for Rs.500.00 to AMAZON on 02-05-26"
        val parsed = SmsParser.parse(msg)
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.01)
        assertEquals("AMAZON", parsed.merchant)
    }

    @Test
    fun testUPIMessage() {
        val msg = "Rs.123.45 debited from A/c XX1234 on 02-May-26 to VPA merchant@upi"
        val parsed = SmsParser.parse(msg)
        assertNotNull(parsed)
        assertEquals(123.45, parsed!!.amount, 0.01)
        assertEquals("merchant@upi", parsed.merchant)
    }

    @Test
    fun testSentMessage() {
        val msg = "Sent Rs.1,000.00 to Myntra on 02-05-26"
        val parsed = SmsParser.parse(msg)
        assertNotNull(parsed)
        assertEquals(1000.0, parsed!!.amount, 0.01)
        assertEquals("Myntra", parsed.merchant)
    }

    @Test
    fun testCreditMessageIgnored() {
        val msg = "Credited for Rs.500.00 from Friend"
        val parsed = SmsParser.parse(msg)
        assertNull(parsed)
    }
}
