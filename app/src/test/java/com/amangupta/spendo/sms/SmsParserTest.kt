package com.amangupta.spendo.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParserTest {

    @Test
    fun testUpiDebit() {
        val sms = "Debited for Rs.500.00 to VPA zomato@icici on 01-05-26"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.01)
        assertEquals("zomato@icici", parsed.merchantVpa)
        assertEquals("Zomato", parsed.merchantName)
    }

    @Test
    fun testUpiDebitAltFormat() {
        val sms = "Rs.123.45 debited from A/c XX1234 to merchant@okaxis. UPI Ref 123456789012"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(123.45, parsed!!.amount, 0.01)
        assertEquals("merchant@okaxis", parsed.merchantVpa)
        assertEquals("Merchant", parsed.merchantName)
        assertEquals("123456789012", parsed.bankRef)
    }

    @Test
    fun testCreditIgnored() {
        val sms = "Credited for Rs.100.00 from friend@upi"
        val parsed = SmsParser.parse(sms)
        assertNull(parsed)
    }

    @Test
    fun testNonUpiIgnored() {
        val sms = "Your OTP is 123456"
        val parsed = SmsParser.parse(sms)
        assertNull(parsed)
    }

    @Test
    fun testCleanName() {
        assertEquals("Zomato", SmsParser.cleanName("zomato@icici"))
        assertEquals("Merchant Name", SmsParser.cleanName("merchant.name123@okaxis"))
        assertEquals("Phonepe", SmsParser.cleanName("phonepe@ybl"))
    }
}
