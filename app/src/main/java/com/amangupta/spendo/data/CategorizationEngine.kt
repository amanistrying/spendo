package com.amangupta.spendo.data

object CategorizationEngine {
    private val DEFAULT_RULES = mapOf(
        "myntra" to "Shopping",
        "amazon" to "Shopping",
        "flipkart" to "Shopping",
        "nykaa" to "Shopping",
        "ajio" to "Shopping",
        "swiggy" to "Food",
        "zomato" to "Food",
        "blinkit" to "Food",
        "zepto" to "Food",
        "uber" to "Travel",
        "ola" to "Travel",
        "rapido" to "Travel",
        "petrol" to "Fuel",
        "shell" to "Fuel",
        "jio" to "Bills",
        "airtel" to "Bills",
        "vi" to "Bills",
        "netflix" to "Subscription",
        "spotify" to "Subscription"
    )

    data class CategoryResult(
        val name: String,
        val category: String
    )

    suspend fun categorize(merchant: String, dao: CategoryRuleDao): CategoryResult {
        val merchantLower = merchant.lowercase()
        
        // 1. Check user-defined rules in DB
        val dbRule = dao.getRuleForMerchant(merchant)
        if (dbRule != null) {
            return CategoryResult(dbRule.friendlyName, dbRule.category)
        }

        // 2. Check hardcoded common rules
        for ((key, category) in DEFAULT_RULES) {
            if (merchantLower.contains(key)) {
                // Capitalize the first letter for display if found in common rules
                val name = key.replaceFirstChar { it.uppercase() }
                return CategoryResult(name, category)
            }
        }

        // 3. Fallback
        return CategoryResult(merchant, "Unknown")
    }
}
