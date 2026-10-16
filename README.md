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

**Directly on your phone:**

1. Open your phone's browser (Chrome)
2. Go to the GitHub repository link
3. Tap **"Releases"** on the right side
4. Tap the latest version (e.g. `v1.0.0`)
5. Under the **"Assets"** section, tap the `app-release.apk` file → the download will start

**Download on a computer and send it to your phone:**

1. On your computer's browser, download `app-release.apk` from the GitHub Releases page
2. Send it to your phone — via USB cable, Bluetooth, WhatsApp, Google Drive, or any method you like
3. The file will be in the phone's **Downloads** folder

## Step 2 — Install it

1. Tap the **"Download complete"** notification — or open the **Files / My Files** app and go to the **Downloads** folder
2. Tap the `app-release.apk` file
3. If Android blocks it ("Install blocked" / unknown apps warning):
   - Tap **"Settings"** on the popup
   - Turn ON **"Allow from this source"**
   - Press back to return
4. Now tap the **"Install"** button
5. When finished, tap **"Done"** or **"Open"**

## Step 3 — Open the app

1. Find **"Tk Koi Gelo"** (red ৳ icon) on your home screen or app drawer
2. Tap to open — that's it, you're ready to use it!

> 💡 Tip: Take a backup from time to time via Settings → _Backup app data_, so your data stays safe.

---

# 🛠️ DEVELOPER GUIDE — Code, Build & Install

For developers who want to build the app from source.

## 1. Requirements

- **JDK 17**
- **Android SDK:** Platform 35, Build Tools 35.0.0
- **Gradle 8.9**
- An Android device with USB debugging enabled (for direct install)
- Internet on the first build (Gradle downloads dependencies); later builds can work offline once cached

## 2. Clone the code

```bash
git clone <repository-url>
cd TkKoiGelo
```

> Replace `<repository-url>` with your GitHub repository link.

## 3. Open the code

**VS Code:**

1. Open the `TkKoiGelo` folder in VS Code
2. Optionally install the Kotlin / Gradle extensions for syntax highlighting

**Android Studio:**

1. **File → Open** → select the `TkKoiGelo` folder
2. Wait for the Gradle sync to finish

## 4. Install on a device (USB debugging)

**Prepare the phone:**

1. **Settings → About phone → tap Build number 7 times** → Developer options will be enabled
2. **Developer options → turn on USB debugging**
3. Connect the phone via USB and tap **Allow** on the authorization prompt
4. Check that the device is detected:
   ```bash
   adb devices
   ```

**Build + install + launch (single command):**

```bash
~/.local/opt/gradle-8.9/bin/gradle --no-daemon --max-workers=1 installDebug && adb shell monkey -p com.masud.moneymanager 1
```

> Prefer the Gradle wrapper? Replace the gradle path with `./gradlew`.

## 5. Generate the APK

```bash
# Debug APK (for testing on your own device)
~/.local/opt/gradle-8.9/bin/gradle --no-daemon --max-workers=1 assembleDebug

# Release APK (for sharing / GitHub releases)
~/.local/opt/gradle-8.9/bin/gradle --no-daemon --max-workers=1 assembleRelease
```

> Note: `assembleRelease` produces an unsigned APK unless signing is configured — fine for sideloading, but the Play Store requires a signed bundle.

## 6. APK location

After the build finishes, you'll find the APK here:

- **Debug:** `app/build/outputs/apk/debug/app-debug.apk`
- **Release:** `app/build/outputs/apk/release/app-release.apk`

Share this APK file with users or upload it to GitHub Releases.

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



# github push
# 1. Sob change commit koro
git add -A
git commit -m "v4.0.0"

# 2. Tag dao
git tag v4.0.0

# 3. Tag confirm koro — v4.0.0 print hotei hobe (na hole build korba na)
git describe --tags --abbrev=0

# 4. Release APK build koro
~/.local/opt/gradle-8.9/bin/gradle --no-daemon --max-workers=1 assembleRelease

# 5. APK rename kore rakho
cp app/build/outputs/apk/release/app-release.apk ~/Tk-Koi-Gelo-v4.apk
ls -lh ~/Tk-Koi-Gelo-v4.apk

# 6. Push koro (code + tag)
git push origin main
git push origin v4.0.0


## 🛠️ Tech stack

- Kotlin + Jetpack Compose (Material 3)
- Room (SQLite) — offline-first, no server, no INTERNET permission
- Apache POI — Excel (.xlsx) export
- Coroutines + Flow

---

Built with ❤️ by Masud ibn Belat
