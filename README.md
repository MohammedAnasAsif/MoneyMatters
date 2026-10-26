# MoneyMatters

An open-source Android expense tracker for India, styled after Apple Stocks.

- **Every UPI payment is logged automatically**, whichever app you paid with (GPay, PhonePe, Paytm, BHIM, CRED, bank apps…): amount, date, time, payee and UPI reference.
- **Cash payments** can be added with one tap on **+**.
- **No internet permission.** Your data never leaves the phone.

## Download

- **Latest build:** open the [Actions tab](https://github.com/MohammedAnasAsif/MoneyMatters/actions), pick the newest green run and download `moneymatters-debug-apk`.
- **Releases:** tagged versions are on the [Releases page](https://github.com/MohammedAnasAsif/MoneyMatters/releases).

Android 8.0 or newer is required.

## How auto-capture works

Android doesn't let one app read another app's payment history, and there is no public UPI API. MoneyMatters reads the two traces every UPI payment leaves:

1. **Bank debit SMS (main source).** Your bank texts you whichever UPI app you used, so this covers all of them. Only SMS from bank-style sender IDs (like `VM-HDFCBK`) are read, never messages from people.
2. **UPI app notifications (backup).** These are "payment successful" alerts from a fixed list of UPI apps, for when the SMS is late. Notifications from other apps are ignored.

If a payment shows up in both, it's stored once (matched on the UPI reference number, or on the same amount within 3 minutes). When a message can't be read confidently, it goes to a **Review** list for you to confirm or discard, so it never quietly lands in your totals.

### First-time setup

1. Install the APK, open **Settings** in the app and allow **Bank SMS**. Your last 90 days of UPI SMS are imported.
2. Optionally allow **UPI app notifications**.
3. On Android 13+, a sideloaded app may show **"Restricted setting"**. Go to *App info → ⋮ → Allow restricted settings*, then try again.

## The screen

The home screen is laid out like the Apple Stocks watchlist:

- Each row is a "ticker": **Spent**, **Received**, and one per category (Food, Transport…). The number is this month's total. The pill is the change against the same days of last month: **red** is bad (more spent, less received) and **green** is good. Tap a pill to switch between ₹ and %.
- Each sparkline shows the month building up, with a dotted line at last month's level.
- Pull up the **Transactions** sheet at the bottom (where Stocks has its news) to see the latest payments. Money in shows green with **+** and money out red with **−**.
- Tap a row to open it full screen, with a big chart you can drag across and 1D / 1W / 1M / 3M / 1Y / ALL ranges.

## Building

CI builds the APK on every push (`.github/workflows/build.yml`). To build locally, install JDK 17, the Android SDK and Gradle 8.11+, then run:

```
gradle assembleDebug
```

### Signed releases

Push a tag like `v0.1.0` to publish a release APK. To sign it with your own key, add these repository secrets: `KEYSTORE_BASE64` (your `.jks` file, base64-encoded), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Without them, the release is signed with the CI debug key.

## Contributing

Bank SMS formats differ. If yours isn't recognised, open an issue with the SMS (**mask your account number and name**). Or add the sample to `app/src/test/.../UpiParserTest.kt` and adjust `UpiParser.kt`.

## License

[GPL-3.0](LICENSE)
