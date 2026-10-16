# Money Manager (Native Android starter)

A small offline-first Kotlin + Jetpack Compose starter. Transactions are stored locally in Room/SQLite. JSON export/import uses Android's system file picker; no storage permission or server is needed.

## Requirements
- JDK 17
- Android SDK Platform 35
- Android Build Tools 35.0.0
- Gradle 8.9 (compatible with Android Gradle Plugin 8.7.3)
- Internet for the first Gradle dependency download; later builds may work offline once dependencies are cached.

## Build without Android Studio
From this project directory, run:

```bash
gradle --version
gradle assembleDebug --no-daemon --max-workers=1
```

APK output:
`app/build/outputs/apk/debug/app-debug.apk`

Install to a USB-debugging-enabled phone:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Project map
- `MainActivity.kt`: Compose screen, add/delete transactions, backup/import UI
- `data/TransactionEntity.kt`: Room table model
- `data/TransactionDao.kt`: local database operations
- `data/AppDatabase.kt`: Room database singleton
- `data/JsonBackup.kt`: JSON serialization and validation

## Important starter limitations
- This is a starter/MVP, not yet the complete planned app.
- JSON import appends imported transactions to existing data; it does not erase current data.
- Loan/repayment module, search/filter, editable transactions, category management, and currency settings are not implemented yet.
- Before importing an unfamiliar JSON backup, keep a separate copy. The app validates transaction type and positive amount but this is not a signed backup format.
- No INTERNET permission is declared in the manifest. Gradle may need internet while building to download dependencies.
