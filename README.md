# Daily Wellness — Android app

Single-user, offline-first native Android wellness tracker.

## Included
- Kotlin Android app and Room/SQLite persistence in app-private internal storage
- PIN setup (4–8 digits), salted PBKDF2-HMAC-SHA256 verifier
- Fingerprint / Android device-credential unlock when supported
- Water, weekly juice, Omega-3, creatine, multivitamin, night magnesium
- Gym/rest-day log, workout type, duration, intensity, notes
- Date navigation and monthly summary
- Android Auto Backup rules for the database and PIN preferences
- No INTERNET permission, analytics SDK, cloud database or external tracker

## Build APK
GitHub Actions builds a debug APK on pushes to main or manually:
1. Open repository → Actions → Build Daily Wellness APK.
2. Run workflow if needed.
3. Download artifact daily-wellness-debug-apk from completed run.

For a long-term installation, create a signed release APK in Android Studio and keep the signing key private.

## Data and security
Daily entries are stored in Room SQLite under app-private internal storage, not cache. Android sandbox protection applies. The app has no Internet permission. PIN verifier is salted and derived; the PIN itself is not stored. Fingerprint templates remain with Android and are never read by the app.

Automatic Android backup is controlled by the phone OS and user backup settings; it may use the user's Google backup account. It is not an app analytics service. Clearing app storage, uninstalling without successful backup, or factory reset can still remove local data. Verify backup and restore on the target phone.

This starter does not yet use SQLCipher database encryption. For stronger database-at-rest encryption, add SQLCipher and protect its key with Android Keystore before production use. This is a functional starter prototype, not a security-audited health app.