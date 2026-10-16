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

---

# 📲 USER GUIDE — App Install Process

For anyone who just wants to install and use the app (no coding needed).

## Step 1 — Download the APK

**Phone diye directly:**

1. Phone er browser (Chrome) open koro
2. GitHub repo link e jao
3. Dan dike **"Releases"** e tap koro
4. Latest version (e.g. `v1.0.0`) te tap koro
5. **"Assets"** section er niche `app-release.apk` file e tap koro → download start hobe

**Computer diye download kore phone e pathate chaile:**

1. Computer er browser e GitHub Releases page theke `app-release.apk` download koro
2. Phone e pathao — USB cable / Bluetooth / WhatsApp / Google Drive jekono vabe
3. Phone e file ta **Downloads** folder e thakbe

## Step 2 — Install koro

1. Notification theke **"Download complete"** te tap koro — athoba **Files / My Files** app open kore **Downloads** folder e jao
2. `app-release.apk` file e tap koro
3. Jodi Android block kore ("Install blocked" / "Unknown apps" warning ashe):
   - Popup e **"Settings"** te tap koro
   - **"Allow from this source"** ON koro
   - Back button chepe fire asho
4. Ebar **"Install"** button e tap koro
5. Install sesh hole **"Done"** ba **"Open"** e tap koro

## Step 3 — App open koro

1. Home screen / app drawer e **"Tk Koi Gelo"** (red ৳ icon) khujo
2. Tap kore open koro — bas, use kora start!

> 💡 Tip: Settings → _Backup app data_ theke majhe majhe backup niye rakho, data safe thakbe.

---

# 🛠️ DEVELOPER GUIDE — Code, Build & Install

For developers who want to build the app from source.

## 1. Requirements

- **JDK 17**
- **Android SDK:** Platform 35, Build Tools 35.0.0
- **Gradle 8.9**
- USB debugging enabled Android device (direct install er jonno)
- First build e internet lagbe (Gradle dependencies download); pore cached thakle offline eo cholbe

## 2. Code clone koro

```bash
git clone <repository-url>
cd TkKoiGelo
```

> `<repository-url>` er jaygay tomar GitHub repo link boshao.

## 3. Code open koro

**VS Code:**

1. `TkKoiGelo` folder ta VS Code e open koro
2. Chaile Kotlin / Gradle extension install kore nio (syntax highlight er jonno)

**Android Studio:**

1. **File → Open** → `TkKoiGelo` folder select koro
2. Gradle sync sesh howa porjonto wait koro

## 4. Device e install koro (USB debugging)

**Phone ready koro:**

1. **Settings → About phone → Build number** e 7 bar tap koro → Developer options on hobe
2. **Developer options → USB debugging** ON koro
3. USB diye phone computer e lagao, authorization popup e **Allow** dao
4. Device detect hocche kina check koro:
   ```bash
   adb devices
   ```

**Build + install + launch (ek command e):**

```bash
~/.local/opt/gradle-8.9/bin/gradle --no-daemon --max-workers=1 installDebug && adb shell monkey -p com.masud.moneymanager 1
```

> Gradle wrapper use korte chaile `~/.local/opt/gradle-8.9/bin/gradle` er bodle `./gradlew` likho.

## 5. APK generate koro

```bash
# Debug APK (nijer device e test er jonno)
~/.local/opt/gradle-8.9/bin/gradle --no-daemon --max-workers=1 assembleDebug

# Release APK (share / GitHub release er jonno)
~/.local/opt/gradle-8.9/bin/gradle --no-daemon --max-workers=1 assembleRelease
```

> Note: `assembleRelease` signing configure na thakle unsigned APK banabe — sideload e cholbe, kintu Play Store e dite signed bundle lagbe.

## 6. APK location

Build sesh hole APK paba ekhane:

- **Debug:** `app/build/outputs/apk/debug/app-debug.apk`
- **Release:** `app/build/outputs/apk/release/app-release.apk`

Ei APK file tai user der majhe share korba / GitHub Releases e upload korba.

---

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

## 🛠️ Tech stack

- Kotlin + Jetpack Compose (Material 3)
- Room (SQLite) — offline-first, no server, no INTERNET permission
- Apache POI — Excel (.xlsx) export
- Coroutines + Flow

---

Built with ❤️ by Masud ibn Belat
