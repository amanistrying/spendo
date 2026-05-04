package com.amangupta.spendo.data

object CategorizationEngine {

    // VPA prefix map — checked before keyword fallback
    // Key = everything before @ in VPA, lowercased, digits stripped from end
    private val VPA_MAP: Map<String, String> = mapOf(
        // Food delivery
        "zomato" to "Food", "swiggy" to "Food", "eatsure" to "Food",
        "faasos" to "Food", "box8" to "Food", "freshmenu" to "Food",
        // QSR
        "dominos" to "Food", "pizzahut" to "Food", "kfc" to "Food",
        "mcdonalds" to "Food", "burgerking" to "Food", "subway" to "Food",
        "dunkin" to "Food", "starbucks" to "Food", "cafeday" to "Food",
        "barista" to "Food", "chaayos" to "Food", "theobroma" to "Food",
        "haldirams" to "Food", "barbeque" to "Food",
        // Groceries / quick commerce
        "blinkit" to "Groceries", "zepto" to "Groceries", "dunzo" to "Groceries",
        "bigbasket" to "Groceries", "jiomart" to "Groceries", "grofers" to "Groceries",
        "dmart" to "Groceries", "reliancefresh" to "Groceries", "spar" to "Groceries",
        "moreretail" to "Groceries", "spencers" to "Groceries",
        // E-commerce / shopping
        "amazon" to "Shopping", "amazonpay" to "Shopping", "flipkart" to "Shopping",
        "myntra" to "Shopping", "ajio" to "Shopping", "meesho" to "Shopping",
        "snapdeal" to "Shopping", "nykaa" to "Shopping", "nykaafashion" to "Shopping",
        "purplle" to "Shopping", "tatacliq" to "Shopping", "croma" to "Shopping",
        "reliancedigital" to "Shopping", "vijaysales" to "Shopping",
        "shoppersstop" to "Shopping", "westside" to "Shopping",
        "pantaloons" to "Shopping", "maxfashion" to "Shopping",
        "lifestyle" to "Shopping", "fabindia" to "Shopping",
        "pepperfry" to "Shopping", "ikea" to "Shopping",
        "bewakoof" to "Shopping", "lenskart" to "Shopping",
        "titan" to "Shopping", "tanishq" to "Shopping",
        // Fuel
        "indianoil" to "Fuel", "iocl" to "Fuel", "hpcl" to "Fuel",
        "bpcl" to "Fuel", "shell" to "Fuel", "nayara" to "Fuel",
        "indane" to "Fuel", "bharatgas" to "Fuel", "hpgas" to "Fuel",
        // Travel
        "uber" to "Travel", "uberpay" to "Travel", "ola" to "Travel",
        "olamoney" to "Travel", "rapido" to "Travel", "meru" to "Travel",
        "irctc" to "Travel", "makemytrip" to "Travel", "goibibo" to "Travel",
        "cleartrip" to "Travel", "redbus" to "Travel", "yulu" to "Travel",
        "bounce" to "Travel", "bluesmartmobility" to "Travel",
        // Entertainment
        "netflix" to "Subscription", "hotstar" to "Subscription",
        "disneyplushotstar" to "Subscription", "zee5" to "Subscription",
        "sonyliv" to "Subscription", "primevideo" to "Subscription",
        "jiocinema" to "Subscription", "altbalaji" to "Subscription",
        "spotify" to "Subscription", "gaana" to "Subscription",
        "wynk" to "Subscription", "bookmyshow" to "Entertainment",
        "pvrinox" to "Entertainment", "cinepolis" to "Entertainment",
        // Bills / utilities
        "airtel" to "Bills", "jio" to "Bills", "vodafoneidea" to "Bills",
        "vi" to "Bills", "bsnl" to "Bills", "tatasky" to "Bills",
        "dishtv" to "Bills", "actfibernet" to "Bills", "hathway" to "Bills",
        "bescom" to "Bills", "tneb" to "Bills", "msedcl" to "Bills",
        "bses" to "Bills", "cesc" to "Bills",
        // Health
        "apollopharmacy" to "Health", "medplus" to "Health",
        "netmeds" to "Health", "tata1mg" to "Health", "pharmeasy" to "Health",
        "practo" to "Health", "cultfit" to "Health", "curefit" to "Health",
        "healthkart" to "Health"
    )

    // Keyword fallback for anything not in VPA_MAP
    private val KEYWORD_RULES: List<Pair<List<String>, String>> = listOf(
        listOf("petrol", "diesel", "fuel", "filling", "pump", "cng") to "Fuel",
        listOf("zomato", "swiggy", "restaurant", "dhaba", "biryani", "cafe",
               "canteen", "bakery", "food", "kitchen") to "Food",
        listOf("kirana", "grocery", "vegetables", "sabzi", "provision",
               "supermarket", "mart") to "Groceries",
        listOf("amazon", "flipkart", "myntra", "fashion", "clothing") to "Shopping",
        listOf("uber", "ola", "taxi", "cab", "auto", "metro", "irctc",
               "fastag", "toll", "parking") to "Travel",
        listOf("netflix", "spotify", "hotstar", "prime") to "Subscription",
        listOf("hospital", "clinic", "pharmacy", "medical",
               "medicine", "doctor", "lab") to "Health",
        listOf("electricity", "broadband", "internet", "recharge", "dth", "bill") to "Bills"
    )

    data class CategoryResult(val name: String, val category: String)

    suspend fun categorize(merchantVpa: String, merchantName: String, dao: CategoryRuleDao): CategoryResult {
        // 1. User-defined rule (highest priority)
        dao.getRuleForMerchant(merchantVpa)?.let {
            return CategoryResult(it.friendlyName, it.category)
        }

        // 2. VPA prefix exact match
        val prefix = merchantVpa.substringBefore("@")
            .lowercase().trimEnd { it.isDigit() }
        VPA_MAP[prefix]?.let {
            return CategoryResult(merchantName, it)
        }

        // 3. VPA prefix partial match (e.g. "zomato" matches "zomato.food")
        VPA_MAP.entries.firstOrNull { (k, _) ->
            prefix.startsWith(k) || k.startsWith(prefix)
        }?.let {
            return CategoryResult(merchantName, it.value)
        }

        // 4. Keyword fallback
        val combined = (merchantVpa + " " + merchantName).lowercase()
        for ((keywords, cat) in KEYWORD_RULES) {
            if (keywords.any { combined.contains(it) }) {
                return CategoryResult(merchantName, cat)
            }
        }

        // 5. Unknown — will prompt user to tag
        return CategoryResult(merchantName, "Unknown")
    }
}
