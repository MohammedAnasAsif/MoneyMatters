package io.github.mohammedanasasif.moneymatters.parse

import io.github.mohammedanasasif.moneymatters.data.TxnType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiParserTest {

    @Test
    fun hdfcSent() {
        val p = UpiParser.parse(
            "Sent Rs.100.00\nFrom HDFC Bank A/C x1234\nTo SWIGGY\nOn 26/10/24\nRef 429912345678\n" +
                "Not You?\nCall 18002586161/SMS BLOCK UPI to 7308080808"
        )!!
        assertEquals(10_000L, p.amountPaise)
        assertEquals(TxnType.DEBIT, p.type)
        assertEquals("Swiggy", p.payee)
        assertEquals("429912345678", p.upiRef)
        assertTrue(p.confident)
    }

    @Test
    fun sbiDebitedByWithoutCurrency() {
        val p = UpiParser.parse(
            "Dear UPI user A/C X1234 debited by 100.0 on date 26Oct24 trf to RAMESH KUMAR " +
                "Refno 429912345678. If not u? call 1800111109. -SBI"
        )!!
        assertEquals(10_000L, p.amountPaise)
        assertEquals(TxnType.DEBIT, p.type)
        assertEquals("Ramesh Kumar", p.payee)
        assertEquals("429912345678", p.upiRef)
    }

    @Test
    fun iciciPayeeBeforeCredited() {
        val p = UpiParser.parse(
            "ICICI Bank Acct XX123 debited for Rs 1,250.50 on 26-Oct-24; DOMINOS PIZZA credited. " +
                "UPI:429912345678. Call 18002662 for dispute. SMS BLOCK 123 to 9215676766."
        )!!
        assertEquals(125_050L, p.amountPaise)
        assertEquals(TxnType.DEBIT, p.type)
        assertEquals("Dominos Pizza", p.payee)
        assertEquals("429912345678", p.upiRef)
    }

    @Test
    fun axisUpiPath() {
        val p = UpiParser.parse(
            "INR 100.00 debited\nA/c no. XX1234\n26-10-24, 12:00:00\nUPI/P2M/429912345678/BLINKIT\n" +
                "Not you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank"
        )!!
        assertEquals(10_000L, p.amountPaise)
        assertEquals("Blinkit", p.payee)
        assertEquals("429912345678", p.upiRef)
    }

    @Test
    fun kotakSentToVpa() {
        val p = UpiParser.parse(
            "Sent Rs.250.00 from Kotak Bank AC X1234 to zomato@hdfcbank on 26-10-24.UPI Ref 429912345678. " +
                "Not you, kotak.com/fraud"
        )!!
        assertEquals(25_000L, p.amountPaise)
        assertEquals("zomato@hdfcbank", p.vpa)
        assertNull(p.payee)
        assertEquals("429912345678", p.upiRef)
    }

    @Test
    fun credit() {
        val p = UpiParser.parse(
            "Rs.500.00 credited to your A/c XX1234 by VPA friend@okicici on 26-10-24 (UPI Ref No 429912345678)"
        )!!
        assertEquals(50_000L, p.amountPaise)
        assertEquals(TxnType.CREDIT, p.type)
        assertEquals("friend@okicici", p.vpa)
        assertEquals("429912345678", p.upiRef)
    }

    @Test
    fun paidYouNotificationIsCredit() {
        val p = UpiParser.parse("Payment received. Ramesh paid you ₹1", requireUpiMarker = false)!!
        assertEquals(100L, p.amountPaise)
        assertEquals(TxnType.CREDIT, p.type)
        assertEquals("Ramesh", p.payee)
    }

    @Test
    fun hdfcMoneyReceivedIsCredit() {
        val p = UpiParser.parse(
            "Money Received - INR 1.00 in HDFC Bank A/c xx1234 on 26-10-26 by A/c linked to VPA " +
                "9876543210@ybl (UPI Ref No 429912345678)"
        )!!
        assertEquals(100L, p.amountPaise)
        assertEquals(TxnType.CREDIT, p.type)
        assertEquals("429912345678", p.upiRef)
    }

    @Test
    fun iciciIsCreditedFromName() {
        val p = UpiParser.parse(
            "Dear Customer, Acct XX123 is credited with Rs 1.00 on 26-Oct-26 from RAMESH. UPI:429912345678-ICICI Bank."
        )!!
        assertEquals(100L, p.amountPaise)
        assertEquals(TxnType.CREDIT, p.type)
        assertEquals("Ramesh", p.payee)
    }

    @Test
    fun debitThatMentionsPayeeCreditedStaysDebit() {
        val p = UpiParser.parse(
            "Rs.100 debited from your A/c XX1234 and credited to swiggy@icici. UPI Ref 429912345678"
        )!!
        assertEquals(TxnType.DEBIT, p.type)
        assertEquals("swiggy@icici", p.vpa)
    }

    @Test
    fun ignoresOtpAndCollectRequests() {
        assertNull(UpiParser.parse("123456 is your OTP for UPI txn of Rs 500. Do not share."))
        assertNull(UpiParser.parse("RAMESH has requested Rs.500 from you via UPI. Pay in the app."))
    }

    @Test
    fun ignoresNonUpiCardSms() {
        assertNull(UpiParser.parse("Rs 1,499.00 spent on HDFC Bank Card x1234 at AMAZON on 2024-10-26"))
    }

    @Test
    fun gpayNotification() {
        val p = UpiParser.parse("Payment successful. ₹349 paid to Swiggy", requireUpiMarker = false)!!
        assertEquals(34_900L, p.amountPaise)
        assertEquals(TxnType.DEBIT, p.type)
        assertEquals("Swiggy", p.payee)
        assertTrue(p.confident)
    }

    @Test
    fun phonePeNotification() {
        val p = UpiParser.parse("Paid ₹1,250 to RAMESH KUMAR", requireUpiMarker = false)!!
        assertEquals(125_000L, p.amountPaise)
        assertEquals("Ramesh Kumar", p.payee)
    }

    @Test
    fun notificationWithoutPayeeNeedsReview() {
        val p = UpiParser.parse("Paid ₹40", requireUpiMarker = false)
        assertNotNull(p)
        assertFalse(p!!.confident)
    }

    @Test
    fun ignoresPromotions() {
        assertNull(UpiParser.parse("Get ₹100 cashback on your next payment!", requireUpiMarker = false))
    }

    @Test
    fun bankSenderCheck() {
        assertTrue(UpiParser.isBankSender("VM-HDFCBK"))
        assertFalse(UpiParser.isBankSender("+919876543210"))
    }

    @Test
    fun categoryGuess() {
        assertEquals("Food", Categories.guess("Swiggy", null))
        assertEquals("Groceries", Categories.guess(null, "blinkit@hdfcbank"))
        assertNull(Categories.guess("Ramesh Kumar"))
    }
}
