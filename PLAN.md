# Plan: open-source UPI + cash expense tracker for Android

Name: **MoneyMatters** (repo: github.com/MohammedAnasAsif/MoneyMatters).

## 1. What the app does

1. **Logs UPI payments automatically.** Every payment made from any UPI app (GPay, PhonePe, Paytm, BHIM, CRED, Amazon Pay, bank apps…) is saved with amount, date, time, payee and UPI reference number.
2. **Lets you add cash transactions by hand.** Amount, note/payee, category, date and time (defaults to now).

Everything stays on the phone. The app has **no INTERNET permission**, so no data can leave the device.

## 2. How auto-capture works (the hard part)

Android has no public UPI API, and one app can't read another app's payment history. Every UPI payment still leaves two traces we *can* read:

| Source | How | Strengths | Weaknesses |
|---|---|---|---|
| **Bank SMS** (primary) | `BroadcastReceiver` on `SMS_RECEIVED` + a one-time inbox import | Works for **every** UPI app, because the bank sends the debit SMS whatever app you used. Has the amount, UPI ref no. and often the payee VPA | Needs the SMS permission. Formats differ by bank |
| **UPI app notifications** (backup) | `NotificationListenerService`, filtered to known UPI app package names | Instant. Catches payments when the bank SMS is late or turned off | Needs "Notification access". Some apps don't post a notification on success |

**Parsing:** a rule table of regexes per bank/app (SBI, HDFC, ICICI, Axis, Kotak, PNB, BoB, etc.), plus a generic fallback that looks for `debited|paid|sent` + `Rs|INR|₹` + an amount + `UPI`. Only debits are logged by default; logging credits is a setting.

**De-duplication:** one payment can show up as an SMS *and* a notification. Match first on the 12-digit UPI reference (RRN). If there isn't one, match on the same amount within ±3 minutes.

**Unsure matches:** when the generic fallback catches something, it goes to a "Review" inbox with one-tap Confirm or Discard, so nothing bad lands in your totals silently.

**Distribution note:** Google Play heavily restricts the SMS permission, so we ship through **GitHub Releases** (and F-Droid later). For a sideloaded open-source APK this works fine.

## 3. UI: Apple Stocks style

Dark (true black) theme, big numbers, thin lines, green and red pills.

**Home screen**
```
┌──────────────────────────────────┐
│ Spent · October                  │
│ ₹12,340            ▲ 8.2% vs Sep │  ← big number + red/green pill
│                                  │
│  ╱╲    ╱╲__╱‾‾╲    ╱‾            │  ← cumulative-spend line chart,
│ ╱  ╲__╱        ╲__╱              │    drag a finger to scrub
│ 1D  1W  [1M]  3M  1Y  ALL        │  ← range chips
├──────────────────────────────────┤
│ Swiggy        ~~~~   [ -₹349 ]   │  ← "watchlist" rows: payee,
│ Cash · Tea    ~~~~   [  -₹20 ]   │    mini sparkline, amount pill
│ Ramesh (VPA)  ~~~~   [ -₹1,200]  │
│ ...                              │
└──────────────────────────────────┘
                           ( + )     ← add cash transaction
```

- **Scrubbing** the chart shows the running total and date under your finger, the way Stocks shows the price.
- **Tapping a row** opens a detail sheet: full info, that payee's own spend chart, edit/delete, change category.
- **Toggle on the header:** Payees ⇄ Categories, so the list can be grouped by category instead.
- **Review badge** when auto-captured items need confirming.
- **Settings:** permissions status, log credits on/off, categories, CSV export/import, app lock (biometric).

## 4. Tech stack

- **Kotlin + Jetpack Compose** (Material 3, custom dark theme)
- **Room** (SQLite) for storage, **Coroutines/Flow** for reactive UI
- **Custom Compose Canvas chart**, so the scrub interaction can match Stocks exactly (no chart library)
- minSdk 26 (Android 8.0), targetSdk 35
- No DI framework and no network libraries: small, auditable, and easy for open-source contributors

### Data model
```kotlin
Transaction(
  id, amountPaise: Long, type: DEBIT|CREDIT,
  timestamp: Long, payee: String?, vpa: String?,
  upiRef: String?, category: String?, note: String?,
  source: SMS|NOTIFICATION|MANUAL_CASH,
  status: CONFIRMED|NEEDS_REVIEW, rawText: String?
)
```
Amounts are stored as integer paise, so there are no floating-point rounding errors.

### Project layout
```
app/src/main/java/.../
  data/        Room db, DAO, repository
  capture/     SmsReceiver, SmsInboxImporter, UpiNotificationListener
  parse/       BankRules, GenericParser, Deduplicator   ← unit-tested
  ui/home/     header, StockChart, TransactionList
  ui/add/      add/edit cash sheet
  ui/detail/   transaction & payee detail
  ui/settings/
app/src/test/  parser tests with real-world SMS samples
.github/workflows/build.yml
```

## 5. Building the APK

This PC doesn't have Java, the Android SDK, Gradle or git. Two options:

- **A. GitHub Actions (recommended).** Each push builds the APK in the cloud and attaches it to the workflow run. Pushing a tag like `v0.1.0` publishes a signed release APK on GitHub Releases. Nothing needs to be installed here except git.
- **B. Local build.** Install JDK 17 + Android command-line tools (~3–4 GB), then run `gradlew assembleRelease`.

## 6. Milestones

1. **Scaffold:** Gradle project, theme, Room, CI workflow producing a debug APK
2. **Manual cash entry** + Stocks-style home (header, chart, list)
3. **SMS capture:** receiver, inbox import, bank rules, parser tests
4. **Notification capture** + de-duplication + Review inbox
5. **Polish:** detail sheet, categories, CSV export, app lock, onboarding for permissions
6. **v0.1.0 release:** signed APK on GitHub Releases, README with screenshots

## 7. Defaults (say if you want any changed)

- License: **GPL-3.0** (keeps forks open source; fine for F-Droid). MIT is the alternative.
- Package id: `io.github.<your-github-username>.tikr`
- Credits (money received): captured but **off by default**
- Currency: INR only for v1
