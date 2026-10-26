package io.github.mohammedanasasif.moneymatters.parse

import io.github.mohammedanasasif.moneymatters.data.TxnType
import java.math.RoundingMode

data class ParsedTxn(
    val amountPaise: Long,
    val type: TxnType,
    val payee: String?,
    val vpa: String?,
    val upiRef: String?,
    /** False when the text matched loosely; such entries go to the Review inbox. */
    val confident: Boolean,
)

/**
 * Turns bank SMS and UPI app notifications into transactions.
 *
 * Bank formats vary a lot, so this works on keywords instead of one regex per bank:
 * find the amount, decide debit vs credit by which keyword comes first, then pull out the
 * UPI reference, the payee's UPI ID and a payee name if one is present.
 * When you add support for a new format, add the sample to UpiParserTest.
 */
object UpiParser {
    /** Bump when parsing changes, so stored payments are re-checked on the next app start. */
    const val VERSION = 2

    private val I = RegexOption.IGNORE_CASE

    private const val NUM = """([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""

    private val IGNORE = Regex(
        """\b(otp|one[- ]time password|collect request|requested|will be debited|will be credited|is due|due on|reminder|failed|declined|unsuccessful|pending|cashback|offer|reward)\b""",
        I,
    )
    private val UPI_MARKER = Regex("""\b(upi|vpa)\b""", I)

    private val AMOUNT_AFTER_VERB = Regex("""\b(?:debited|credited)\s+(?:by|with|for)\s+(?:(?:rs\.?|inr|₹)\s*)?$NUM""", I)
    private val AMOUNT = Regex("""(?:(?<![a-z])(?:rs\.?|inr)|₹)\s*$NUM""", I)

    private val DEBIT = Regex("""\b(debited|debit|paid|sent|spent|withdrawn|transferred|payment of|txn of)\b""", I)
    private val CREDIT = Regex("""\b(credited|received|deposited|refunded|refund|reversed)\b""", I)

    // Phrases that settle the direction on their own, because they say whose money moved.
    // Needed for texts like "Ramesh paid you ₹1", where "paid" alone would look like a debit.
    private val STRONG_CREDIT = Regex(
        """\b(paid you|sent you|you(?:'ve| have)? received|money received|received from|credited (?:to|in|into) (?:your|ur)|(?:is|has been|was) credited|deposited (?:in|to|into) (?:your|ur))\b""",
        I,
    )
    private val STRONG_DEBIT = Regex(
        """\b(you paid|you sent|you have paid|you have sent|debited from (?:your|ur)|(?:is|has been|was) debited|paid to|sent to)\b""",
        I,
    )

    private val REF = Regex("""(?:ref(?:erence)?|rrn|utr|txn|transaction|upi)[\s\w.:#/-]{0,15}?(?<!\d)(\d{12})(?!\d)""", I)
    private val ANY_12_DIGITS = Regex("""(?<!\d)(\d{12})(?!\d)""")

    private val VPA = Regex("""([a-zA-Z0-9][a-zA-Z0-9._-]*@[a-zA-Z][a-zA-Z0-9]+)(?![a-zA-Z0-9]|\.[a-zA-Z])""")

    private const val NAME = """([A-Za-z0-9][A-Za-z0-9 .&'_@-]{1,40}?)"""
    private const val STOP = """(?=\s+(?:on|at|ref|refno|upi|via|from|using|for|thru|through|dated|avl|bal)\b|\s*[;,(]|\.\s|\.?$)"""

    private val PAYEE_UPI_PATH = Regex("""\bupi/[a-z0-9]{2,4}/\d{6,}/([A-Za-z0-9 .&'_-]{2,40}?)(?=/|\s|$)""", I)
    private val PAYEE_BEFORE_CREDITED = Regex("""[;.]\s*([A-Za-z][A-Za-z0-9 .&'_-]{1,40}?)\s+credited\b""", I)
    private val PAYEE_TO = Regex("""\b(?:paid to|sent to|trf to|transferred to|to vpa|to)\s+$NAME$STOP""", I)
    private val PAYEE_FROM = Regex("""\b(?:received from|from vpa|from)\s+$NAME$STOP""", I)
    private val PAYEE_BEFORE_PAID_YOU = Regex("""(?:^|[.:]\s*)([A-Za-z][A-Za-z0-9 &'_-]{1,40}?)\s+(?:paid|sent) you\b""", I)

    private val BAD_PAYEE = Regex(
        """^(?:(?:a/?c|ac|acct|account|your|you|the|bank|card|mobile|block|dispute)\b|\d+(?:\s|$))""",
        I,
    )
    private val WHITESPACE = Regex("""\s+""")

    /** Bank SMS come from alphanumeric sender IDs (e.g. "VM-HDFCBK"), never plain phone numbers. */
    fun isBankSender(address: String): Boolean = address.any { it.isLetter() }

    /**
     * @param requireUpiMarker true for SMS, so card and ATM messages are skipped. Notifications
     * from UPI apps are UPI by definition and pass false.
     */
    fun parse(text: String, requireUpiMarker: Boolean = true): ParsedTxn? {
        val t = text.replace(WHITESPACE, " ").trim()
        if (t.isEmpty() || IGNORE.containsMatchIn(t)) return null

        val vpa = VPA.find(t)?.groupValues?.get(1)
        if (requireUpiMarker && vpa == null && !UPI_MARKER.containsMatchIn(t)) return null

        val amountMatch = AMOUNT_AFTER_VERB.find(t) ?: AMOUNT.find(t) ?: return null
        val amount = toPaise(amountMatch.groupValues[1]) ?: return null

        val type = direction(t) ?: return null

        val ref = (REF.find(t) ?: ANY_12_DIGITS.find(t))?.groupValues?.get(1)
        val payee = findPayee(t, type)

        return ParsedTxn(
            amountPaise = amount,
            type = type,
            payee = payee,
            vpa = vpa,
            upiRef = ref,
            confident = ref != null || payee != null || vpa != null,
        )
    }

    /** Strong phrases win; otherwise whichever of debit/credit is mentioned first. */
    private fun direction(t: String): TxnType? {
        val strongCredit = STRONG_CREDIT.find(t)?.range?.first
        val strongDebit = STRONG_DEBIT.find(t)?.range?.first
        if (strongCredit != null && (strongDebit == null || strongCredit < strongDebit)) return TxnType.CREDIT
        if (strongDebit != null) return TxnType.DEBIT

        val debitAt = DEBIT.find(t)?.range?.first ?: Int.MAX_VALUE
        val creditAt = CREDIT.find(t)?.range?.first ?: Int.MAX_VALUE
        if (debitAt == Int.MAX_VALUE && creditAt == Int.MAX_VALUE) return null
        return if (debitAt < creditAt) TxnType.DEBIT else TxnType.CREDIT
    }

    private fun findPayee(t: String, type: TxnType): String? {
        val patterns = if (type == TxnType.DEBIT) {
            listOf(PAYEE_UPI_PATH, PAYEE_BEFORE_CREDITED, PAYEE_TO)
        } else {
            listOf(PAYEE_UPI_PATH, PAYEE_BEFORE_PAID_YOU, PAYEE_FROM)
        }
        for (pattern in patterns) {
            for (match in pattern.findAll(t)) {
                clean(match.groupValues[1])?.let { return it }
            }
        }
        return null
    }

    private fun clean(raw: String): String? {
        val s = raw.trim().trimEnd('.', '-', '_').replace(WHITESPACE, " ")
        if (s.length < 2 || '@' in s || BAD_PAYEE.containsMatchIn(s)) return null
        // Banks often SHOUT names; "RAMESH KUMAR" reads better as "Ramesh Kumar".
        return if (s == s.uppercase()) {
            s.lowercase().split(' ').joinToString(" ") { word -> word.replaceFirstChar { it.titlecase() } }
        } else {
            s
        }
    }

    private fun toPaise(number: String): Long? =
        number.replace(",", "").toBigDecimalOrNull()
            ?.movePointRight(2)
            ?.setScale(0, RoundingMode.DOWN)
            ?.toLong()
            ?.takeIf { it in 1L..10_000_000_000L }
}
