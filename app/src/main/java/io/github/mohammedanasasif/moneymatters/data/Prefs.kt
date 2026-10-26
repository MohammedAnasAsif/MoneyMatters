package io.github.mohammedanasasif.moneymatters.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("moneymatters", Context.MODE_PRIVATE)

    /** Whether money received over UPI is logged too. */
    var logCredits: Boolean
        get() = sp.getBoolean("log_credits", true)
        set(value) = sp.edit().putBoolean("log_credits", value).apply()

    /** The UpiParser.VERSION that stored payments were last checked with. */
    var parserVersion: Int
        get() = sp.getInt("parser_version", 0)
        set(value) = sp.edit().putInt("parser_version", value).apply()
}
