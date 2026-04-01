# ❄️ Freezing Apps

A powerful Android app for **freezing and unfreezing installed apps** on rooted devices with KernelSU (KSU) root authentication. Frozen apps are completely disabled and won't run in the background, saving battery and resources.

## ✨ Features

### Core Features
- **Root Access Verification** — Verifies KSU root at app launch with graceful fallback
- **App Management** — List all installed apps (system + user) with icons, names, package names, and freeze status
- **Freeze/Unfreeze Toggle** — One-tap freeze/unfreeze for each app
- **Multi-Select** — Bulk freeze/unfreeze multiple apps at once
- **Search & Filter** — Search apps by name or package name, filter by system/frozen status

### Advanced Features
- **Scheduled Tasks** — Schedule freeze/unfreeze operations using WorkManager
- **Action History** — Full log of all freeze/unfreeze operations with timestamps
- **Notifications** — Local notifications for successful/failed operations
- **Backup/Restore** — Export and import freeze states as JSON
- **Biometric Auth** — Optional fingerprint/PIN authentication for advanced actions
- **Dark Mode** — Full dark mode support with Material Design 3

### UI/UX
- Modern Material Design 3 with smooth animations
- Floating Action Button for bulk operations
- Swipe-to-refresh for reloading the app list
- Empty states and loading indicators
- Snackbar feedback for all operations

## 🏗️ Architecture

The app follows the **MVVM (Model-View-ViewModel)** architecture pattern:

```
app/
├── data/
│   ├── model/         # Data classes (AppInfo, ActionLog, ScheduledTask)
│   ├── db/            # Room database (ActionLogDao, AppDatabase)
│   └── repository/    # AppRepository - single source of truth
├── root/              # Root command execution (RootCommandExecutor)
├── ui/
│   ├── activity/      # Activities (Main, History, Schedule, Settings)
│   ├── adapter/       # RecyclerView adapters
│   └── viewmodel/     # ViewModels with LiveData
├── worker/            # WorkManager workers for scheduled tasks
├── util/              # Utility classes (BackupUtils)
└── FreezingApp.kt     # Application class
```

### Key Technologies
- **Kotlin** with Coroutines for async operations
- **Room** for local database storage
- **LiveData** for reactive UI updates
- **WorkManager** for reliable scheduled tasks
- **Material Design 3** for modern UI components
- **ViewBinding** for type-safe view access

## 🔧 How It Works

### Freeze/Unfreeze Mechanism

The app uses Android's Package Manager commands executed with root privileges:

- **Freeze (disable):** `pm disable-user --user 0 <package_name>`
- **Unfreeze (enable):** `pm enable --user 0 <package_name>`
- **Check status:** `pm list packages -d` (lists disabled packages)

All commands are executed through the `su` binary, which requires KSU or similar root solution.

### Security

- Package names are validated against a strict regex pattern before use in commands
- Only `pm disable-user` and `pm enable` commands are executed
- Root commands are isolated in `RootCommandExecutor` for maintainability
- Optional biometric authentication for critical operations

## 📦 Building

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 17
- Android SDK 35
- A rooted Android device with KSU for testing

### Build from Android Studio
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Build → Make Project
5. Run on a rooted device

### Build from Command Line
```bash
# Clone the repo
git clone https://github.com/mkmk749278/Freezing-apps.git
cd Freezing-apps

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# APK location
# Debug:   app/build/outputs/apk/debug/app-debug.apk
# Release: app/build/outputs/apk/release/app-release-unsigned.apk
```

### Automated Builds (CI/CD)
The project includes a GitHub Actions workflow that automatically:
- Builds both debug and release APKs on push/PR to main
- Uploads APKs as downloadable artifacts
- See `.github/workflows/build-apk.yml`

## 📱 Installation

1. Download the APK from [GitHub Actions artifacts](../../actions) or build locally
2. Enable "Install from unknown sources" on your device
3. Install the APK
4. Grant root access when prompted (requires KSU or similar)

## 🚀 Usage

### Basic Usage
1. Launch the app — it will verify root access
2. Browse the list of installed apps
3. Tap **Freeze** to disable an app, **Unfreeze** to re-enable it
4. Use the search bar to find specific apps

### Bulk Operations
1. Long-press any app to enter multi-select mode
2. Select apps you want to freeze/unfreeze
3. Tap the FAB (Floating Action Button) to choose an action
4. Or use the menu → "Select All" for batch operations

### Scheduled Tasks
1. Open the menu → "Schedule Task"
2. Enter the package name of the target app
3. Select the action (Freeze/Unfreeze)
4. Set the date and time
5. Tap "Schedule Task"

### Action History
- Open the menu → "Action History" to view all past operations
- Clear history from the button at the bottom

### Settings
- **Dark Mode** — Toggle dark/light theme
- **Biometric Auth** — Enable fingerprint/PIN for operations

## 🗂️ Project Structure

| File/Directory | Description |
|---|---|
| `app/build.gradle.kts` | App-level Gradle config with dependencies |
| `build.gradle.kts` | Root Gradle config with plugin versions |
| `settings.gradle.kts` | Project settings and repository configuration |
| `AndroidManifest.xml` | App manifest with activities and permissions |
| `FreezingApp.kt` | Application class, notification channel setup |
| `root/RootCommandExecutor.kt` | Root command execution with validation |
| `data/model/` | Data classes for app info, action logs |
| `data/db/` | Room database and DAO definitions |
| `data/repository/` | Repository pattern for data access |
| `ui/activity/` | All activity implementations |
| `ui/adapter/` | RecyclerView adapters |
| `ui/viewmodel/` | ViewModel with business logic |
| `worker/FreezeWorker.kt` | WorkManager worker for scheduled tasks |
| `util/BackupUtils.kt` | Backup/restore utility |
| `.github/workflows/build-apk.yml` | CI/CD workflow for APK builds |

## ⚠️ Important Notes

- **Root Required**: This app requires a rooted device with KSU (KernelSU) or similar root solution
- **Use with Caution**: Freezing system apps may cause instability. Only freeze apps you understand
- **This app cannot freeze itself**: It's excluded from the app list for safety
- **Frozen apps**: Will not appear in the launcher and cannot receive notifications or updates

## 🔒 Permissions

| Permission | Purpose |
|---|---|
| `QUERY_ALL_PACKAGES` | List all installed apps |
| `POST_NOTIFICATIONS` | Show operation result notifications |
| `USE_BIOMETRIC` | Optional biometric authentication |
| `RECEIVE_BOOT_COMPLETED` | Restore scheduled tasks after reboot |

## 📄 License

This project is open source. Feel free to use, modify, and distribute.

## 🤝 Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Submit a pull request

## 🔮 Future Enhancements

- Backend integration hooks for cloud sync
- App categories and grouping
- Freeze profiles (work, sleep, gaming)
- Widget for quick freeze/unfreeze
- Tasker/Automate integration
- Per-app scheduling from the app list