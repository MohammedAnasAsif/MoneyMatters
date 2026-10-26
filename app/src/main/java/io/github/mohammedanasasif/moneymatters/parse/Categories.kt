package io.github.mohammedanasasif.moneymatters.parse

object Categories {
    val all = listOf(
        "Food", "Groceries", "Transport", "Shopping", "Bills",
        "Rent", "Health", "Entertainment", "Transfers", "Other",
    )

    private val keywords = mapOf(
        "Food" to listOf("swiggy", "zomato", "domino", "pizza", "kfc", "mcdonald", "cafe", "restaurant", "starbucks", "chai", "eatclub"),
        "Groceries" to listOf("blinkit", "zepto", "bigbasket", "dmart", "instamart", "grofers", "jiomart", "kirana"),
        "Transport" to listOf("uber", "ola", "rapido", "irctc", "metro", "fastag", "petrol", "fuel", "hpcl", "bpcl", "iocl", "redbus"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa"),
        "Bills" to listOf("airtel", "jio", "vodafone", "bescom", "electricity", "bsnl", "tatapower", "recharge", "broadband"),
        "Entertainment" to listOf("netflix", "hotstar", "spotify", "bookmyshow", "pvr", "inox"),
        "Health" to listOf("pharmacy", "apollo", "medplus", "pharmeasy", "1mg", "hospital", "clinic"),
    )

    /** Best-effort category from payee name / UPI ID, or null when nothing matches. */
    fun guess(vararg hints: String?): String? {
        val text = hints.filterNotNull().joinToString(" ").lowercase()
        if (text.isBlank()) return null
        return keywords.entries.firstOrNull { (_, words) -> words.any { it in text } }?.key
    }
}
