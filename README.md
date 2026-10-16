# Tk Koi Gelo 💰

A modern, offline-first personal finance app for Android — built with Kotlin & Jetpack Compose.

Track income & expenses, manage loans, organize money in separate accounts, and back everything up to Excel or JSON. No account, no internet, no ads — your data stays on your device.

## ✨ Features

- **Transactions** — monthly list view, calendar strip view, fast search
- **Loan Manager** — lent/borrowed tracking per person, animated repayment progress, Active/Done tabs
- **Accounts** — separate named accounts with dated entries (amount + note)
- **Dynamic categories** — add, edit & delete your own income/expense categories
- **Backup & restore** — export/import Excel (.xlsx) or JSON via the system file picker
- **130+ currencies** — searchable currency picker
- **Dark theme** — edge-to-edge, modern animated UI

## 📋 Requirements

- JDK 17
- Android SDK: Platform 35, Build Tools 35.0.0
- Gradle 8.9
- Android device with USB debugging enabled (for direct install)
- Internet on first build (Gradle downloads dependencies); later builds can work offline once cached

## 📂 Open the code

**VS Code**

1. Open the project folder (`MoneyManager/`)
2. Optional: install Kotlin / Gradle extensions for syntax highlighting

**Android Studio**

1. File → Open → select the `MoneyManager` folder
2. Wait for the Gradle sync to finish

## 🔨 Generate APK

From the project directory, run:

```bash
# Debug APK (for testing on your own device)
~/.local/opt/gradle-8.9/bin/gradle --no-daemon --max-workers=1 assembleDebug

# Release APK (for sharing)
~/.local/opt/gradle-8.9/bin/gradle --no-daemon --max-workers=1 assembleRelease
```

APK output:

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

> Using the Gradle wrapper instead? Replace the gradle path with `./gradlew`.

> Note: `assembleRelease` builds an unsigned APK unless signing is configured — fine for sideloading, but Play Store upload needs a signed bundle.

## ▶️ Run on a device (USB debugging)

1. On the phone: **Settings → About phone → tap Build number 7 times** → Developer options enabled
2. **Developer options → enable USB debugging**
3. Connect the phone via USB and accept the authorization prompt
4. Verify the device is detected:
   ```bash
   adb devices
   ```
5. Build, install & launch in one command:
   ```bash
   ~/.local/opt/gradle-8.9/bin/gradle --no-daemon --max-workers=1 installDebug && adb shell monkey -p com.masud.moneymanager 1
   ```

## 📲 Install the APK manually

1. Copy `app-debug.apk` (or `app-release.apk`) to your phone — via USB, Bluetooth, Drive, etc.
2. Open it with a file manager
3. When prompted, allow **"Install unknown apps"** for that app
4. Tap **Install**

## 🗺️ Project map

```
app/src/main/
├── AndroidManifest.xml                      # app label: "Tk Koi Gelo"
├── java/com/masud/moneymanager/
│   ├── MainActivity.kt                      # entry point, bottom nav, backup/import wiring
│   ├── data/
│   │   ├── AppDatabase.kt                   # Room database singleton
│   │   ├── TransactionEntity.kt / TransactionDao.kt
│   │   ├── CategoryEntity.kt / CategoryDao.kt          # dynamic categories
│   │   ├── LoanEntity.kt / LoanDao.kt
│   │   ├── LoanRepaymentEntity.kt
│   │   ├── AccountEntity.kt / AccountDao.kt
│   │   ├── AccountEntryEntity.kt
│   │   └── BackupManager.kt                 # Excel (.xlsx) + JSON export/import
│   └── ui/
│       ├── Theme.kt                         # MoneyManagerTheme
│       ├── HomeScreen.kt                    # Monthly / Calendar / Accounts tabs
│       ├── LoansScreen.kt                   # Loan Manager screen
│       ├── SettingsScreen.kt                # settings, currency, backup, categories
│       ├── UiUtils.kt                       # currency list, formatters, shared helpers
│       ├── AddTransactionDialog.kt
│       ├── EditTransactionDialog.kt
│       ├── CategoryManagementSheet.kt
│       └── YearMonthPickerSheets.kt
└── res/mipmap-*/                             # launcher icons (red ৳)
```

## 💾 Backup & restore

- **Export:** Settings → _Backup app data_ → choose Excel (.xlsx) or JSON
- **Import:** Settings → _Import Data_ → pick a backup file → current data is replaced after confirmation
- ⚠️ Tip: export a backup first before importing an unfamiliar file — import replaces all current data.

## 🛠️ Tech stack

- Kotlin + Jetpack Compose (Material 3)
- Room (SQLite) — offline-first, no server, no INTERNET permission
- Apache POI — Excel (.xlsx) export
- Coroutines + Flow

---

Built with ❤️ by Masud ibn Belat
