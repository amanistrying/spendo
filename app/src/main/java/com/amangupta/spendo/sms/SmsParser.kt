package com.amangupta.spendo.sms

import java.util.regex.Pattern

object SmsParser {
    data class ParsedTransaction(
        val amount: Double,
        val merchant: String,
        val date: String // We can improve this to Long/Date later
    )

    // Regex to match "Debited", "Sent", "Paid" followed by an amount and merchant
    // Example: "Debited for Rs.500.00 to merchant@upi"
    // Example: "Rs.500.00 debited to merchant"
    
    private val AMOUNT_PATTERN = Pattern.compile("(?i)(?:rs\\.?|inr)\\s*([\\d,]+\\.?\\d*)")
    private val MERCHANT_PATTERN = Pattern.compile("(?i)(?:to|at)\\s+([^\\s,]+(?:\\s+[^\\s,]+){0,2})")

    fun parse(message: String): ParsedTransaction? {
        val lowerCaseMsg = message.lowercase()
        
        // Basic filter for debit messages
        if (!lowerCaseMsg.contains("debited") && !lowerCaseMsg.contains("paid") && !lowerCaseMsg.contains("sent")) {
            return null
        }

        val amountMatcher = AMOUNT_PATTERN.matcher(message)
        val amount = if (amountMatcher.find()) {
            amountMatcher.group(1)?.replace(",", "")?.toDoubleOrNull()
        } else {
            null
        }

        // Refined merchant extraction: try to find merchant after "to" or "at", but stop before "on", "from", etc.
        var merchant: String? = null
        val merchantMatcher = MERCHANT_PATTERN.matcher(message)
        if (merchantMatcher.find()) {
            val rawMerchant = merchantMatcher.group(1)?.trim() ?: ""
            // Clean up: remove "VPA ", "on ...", etc.
            merchant = rawMerchant
                .replace("(?i)^VPA\\s+".toRegex(), "")
                .split("(?i)\\s+on\\s+".toRegex())[0]
                .split("(?i)\\s+from\\s+".toRegex())[0]
                .trim()
        }

        return if (amount != null) {
            ParsedTransaction(amount, merchant ?: "Unknown Merchant", "Today")
        } else {
            null
        }
    }
}
