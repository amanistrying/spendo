package com.amangupta.spendo.sms

object SmsParser {

    data class ParsedTransaction(
        val amount: Double,
        val merchantVpa: String,   // clean VPA e.g. "zomato@icici"
        val merchantName: String,  // display name e.g. "Zomato"
        val bankRef: String
    )

    private val AMOUNT_PATTERNS = listOf(
        Regex("""(?:INR|Rs\.?|₹)\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
        Regex("""([\d,]+\.?\d*)\s*(?:INR|Rs\.?)""", RegexOption.IGNORE_CASE)
    )

    // Matches UPI VPAs: localpart@psp
    private val VPA_PATTERN = Regex(
        """([a-zA-Z0-9._\-]{2,}@(?:ybl|okaxis|okhdfcbank|okicici|oksbi|paytm|upi|apl|ibl|pnb|yesbank|indus|dbs|rbl|freecharge|airtel|jio|icici|hdfc|sbi|axis|kotak|federal|kvb|jpmorgan|hsbc|citi|sc|aubank|equitas|idfc|slice|jupiter|fi|niyox|razorpay|phonepe|gpay|amazonpay))""",
        RegexOption.IGNORE_CASE
    )

    private val REF_PATTERN = Regex(
        """(?:UPI\s*[Rr]ef\.?\s*(?:No\.?)?|Ref\s*No\.?|Txn\s*ID|UPI Ref)[:\s]*([0-9]{10,20})"""
    )

    private val DEBIT_SIGNALS = listOf(
        "debited", "debit", "paid", "sent", "payment successful", "transferred"
    )
    private val CREDIT_SIGNALS = listOf("credited", "received", "credit", "refund", "cashback")

    fun isUpiDebit(sms: String): Boolean {
        val lower = sms.lowercase()
        return DEBIT_SIGNALS.any { lower.contains(it) }
            && CREDIT_SIGNALS.none { lower.contains(it) }
            && (lower.contains("@") || lower.contains("upi"))
    }

    fun parse(sms: String): ParsedTransaction? {
        if (!isUpiDebit(sms)) return null
        val amount = extractAmount(sms) ?: return null
        val vpa = extractVpa(sms) ?: "unknown@upi"
        return ParsedTransaction(
            amount = amount,
            merchantVpa = vpa.lowercase(),
            merchantName = cleanName(vpa),
            bankRef = extractRef(sms)
        )
    }

    private fun extractAmount(sms: String): Double? {
        for (p in AMOUNT_PATTERNS) {
            val v = p.find(sms)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull()
            if (v != null && v > 0) return v
        }
        return null
    }

    private fun extractVpa(sms: String): String? {
        return VPA_PATTERN.find(sms)?.value
    }

    private fun extractRef(sms: String): String =
        REF_PATTERN.find(sms)?.groupValues?.get(1) ?: ""

    fun cleanName(vpa: String): String {
        val local = vpa.substringBefore("@")
            .trimEnd { it.isDigit() }
            .replace(Regex("[._\\-]"), " ")
        return local.split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            .trim()
            .ifBlank { vpa.substringBefore("@") }
    }
}
