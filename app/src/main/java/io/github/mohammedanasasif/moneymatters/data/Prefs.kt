package io.github.mohammedanasasif.moneymatters.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("moneymatters", Context.MODE_PRIVATE)

    /** Whether money received over UPI is logged too. Off by default: the app tracks spending. */
    var logCredits: Boolean
        get() = sp.getBoolean("log_credits", false)
        set(value) = sp.edit().putBoolean("log_credits", value).apply()
}
