# Amarok Project Overview

## Introduction

Amarok is a lightweight Android app that quickly hides files and applications — ideal for casual privacy needs. Instead of encryption, it manages file and app visibility using various techniques.

Core Features

- **File Hiding:** Obfuscation, NoMedia, and Chmod modes.
- **App Hiding:** Supports Root, Shizuku, and Dhizuku modes.
- **XHide Module:** Uses Xposed to filter hidden apps from system queries.
- **Panic Button:** Floating button to trigger hide operations.
- **Quick Settings Tile:** Direct access for quick hide/unhide.
- **App Lock:** Protects with password/fingerprint.

## Functional Index

### 1. Hiding Management

**Files:** `app/src/main/java/deltazero/amarok/core/Hider.kt`, `HiderStateRepository.kt`, `SettingsRepository.kt`

The DataStore-backed repositories own persisted facts. `Hider` owns active hider strategies, activation errors, transient processing state, and hide/unhide orchestration using coroutines.

- **Persisted configuration:** `SettingsRepository.settings: StateFlow<SettingsSnapshot>` owns settings such as hider modes, XHide flags, quick-hide settings, security settings, appearance, and updates.
- **Persisted hide model:** `HiderStateRepository` owns `managedApps`, `managedFolders`, `hiddenApps`, and `hiddenFolders`. Managed items are user configuration; hidden sets are the persisted result of successful `Hider` operations.
- **State:** `Hider.state: StateFlow<State>` — derived from repository-backed app/folder state plus transient processing sets. `Hider.getState()` exists for synchronous Java/service reads.
- **Unified process:** `Hider.processAll(context, Hider.Action)` drives managed app + folder processing; blocking I/O runs on `Dispatchers.IO`.
- **Targeted operations:** `Hider.processApps()` and `Hider.processFolders()` handle explicit app/folder sets.
- **Per-item state:** `Hider.appStates` and `Hider.folderStates` are `StateFlow<Map<String, Hider.State>>`, derived from managed sets, hidden sets, and transient processing sets.
- **Mode source of truth:** hider modes are read from `SettingsRepository.settings`; ViewModels should use settings flows for display. `Hider` keeps only active strategy instances and observes settings changes to rebuild/reactivate them.
- **Strategy ownership:** `Hider.init(context, settingsRepo, hiderStateRepo)` builds the selected app/file hiders, observes relevant settings, and attempts one activation pass. `switchAppHider()` / `switchFileHider()` only persist desired modes; the settings observer rebuilds/reactivates strategies.
- **Java interop:** `Hider.getStateLiveData()` / `Hider.getAppStatesLiveData()` expose LiveData bridges for Java consumers.

### 2. File Hiding Implementations

**Directory:** `app/src/main/java/deltazero/amarok/filehider/`

- **Interface:** `FileHider` (sealed interface) with `suspend fun activate()` and `suspend fun process(targetDirs, Hider.Action)`.
- **Mode enum:** `FileHiderMode` — `NONE`, `OBFUSCATE`, `NOMEDIA`, `CHMOD` with string keys.
- **Implementations (all implement `FileHider`):**
    - `NoneFileHider.kt` — no-op
    - `ObfuscateFileHider.kt` — Base64 filename encoding + optional content obfuscation
    - `NoMediaFileHider.kt` — creates/removes .nomedia files
    - `ChmodFileHider.kt` — changes file permissions via root
- **Factory:** `FileHider.build(context, mode)` creates instances from enum values.

### 3. App Hiding Implementations

**Directory:** `app/src/main/java/deltazero/amarok/apphider/`

- **Interface:** `AppHider` (sealed interface) with `suspend fun activate()` and `suspend fun process(pkgNames, Hider.Action)`.
- **Mode enum:** `AppHiderMode` — `NONE`, `ROOT`, `SHIZUKU`, `DHIZUKU` with string keys.
- **Implementations (all implement `AppHider`):**
    - `NoneAppHider.kt` — no-op
    - `RootAppHider.kt` — pm disable/hide via libsu
    - `ShizukuAppHider.kt` — IPackageManager via Shizuku reflection
    - `DhizukuAppHider.kt` — DevicePolicyManager via Dhizuku
- **Factory:** `AppHider.build(context, mode, options)` creates configured instances from enum values.

### 4. Core Types

- **`apphider/AppHider.kt`:** `AppHiderOptions` carries app-specific runtime policy like `disableOnly`.
- **`core/ActivationResult.kt`:** Data class — `ActivationResult(success, msgResId)`.
- **`utils/ShellExt.kt`:** Coroutine bridges — `Shell.Job.await()`, `awaitShell()`.

### 5. Main UI — Single Activity with NavHost

**Entry:** `app/src/main/java/deltazero/amarok/ui/MainActivity.kt`

The app uses a single `MainActivity` that hosts a bottom-navigation `NavHost` with four tabs:

| Tab | Screen | File |
|-----|--------|------|
| Dashboard | `DashboardScreen` | `DashboardScreen.kt` |
| Apps | `AppsScreen` | `AppsScreen.kt` |
| Files | `FilesScreen` | `FilesScreen.kt` |
| Settings | `SettingsScreen` | `settings/SettingsScreen.kt` |

**Navigation setup:** `AmarokNavigation.kt` defines the `NavHost`, routes, and bottom nav bar.

**ViewModels:**
- `MainViewModel.kt` — hider state, managed counts, hider mode names (used by `DashboardScreen`)
- `AppsViewModel.kt` — managed apps list, per-app hidden/processing state, hide/unhide actions
- `FilesViewModel.kt` — managed folders list, per-folder hidden/processing state, add/remove/hide/unhide actions
- `settings/SettingsViewModel.kt` — settings state and action methods
- `AppPickerViewModel.kt` — app picker for selecting which apps to hide (used by `AppPickerScreen`)

**App picker:** `AppPickerScreen.kt` — navigated to from `AppsScreen`'s edit button via NavHost route `app_picker`.

### 6. Settings and Persistence

- Settings persistence is handled by `SettingsRepository.kt` using Preferences DataStore. It exposes a typed `SettingsSnapshot` flow and setter methods.
- Hide-model persistence is handled by `HiderStateRepository.kt` using Preferences DataStore. It exposes managed/hidden app and folder flows plus managed-item mutation helpers.
- UI is implemented under `app/src/main/java/deltazero/amarok/ui/settings/`.
- `SettingsRoute.kt` wires the screen to `SettingsViewModel`, platform callbacks, and update checks.
- `SettingsScreen.kt` owns the scaffold and section ordering.
- `settings/sections/*.kt` holds one composable file per preference category.
- `SettingsState.kt` groups `SettingsUiState` into section-specific sub-states; `SettingsActions.kt` mirrors that with section action bundles.
- Business logic for settings is in `SettingsViewModel.kt`, which derives `SettingsUiState` from repository and `Hider` flows. Actions persist settings through repositories.
- Activation is flow-driven: changing persisted app/file hider mode updates `SettingsRepository`; `Hider` observes the settings flow, rebuilds the active strategy, and updates activation error flows.

#### Preference Categories

Settings sections are defined in `settings/sections/`:
- Workmode, XHide, Privacy, Quick Hide, Appearance, Update, About

#### To Add a New Setting

1. Add a key, field, default, decoder entry, and setter in `SettingsRepository.kt`.
2. Add the field to the appropriate section state in `SettingsState.kt` and a corresponding action method to `SettingsViewModel`.
3. Add the setting UI in the appropriate file under `settings/sections/`. Use `R.drawable.ic_null` as placeholder icon for new options.

### 7. XHide

**Directory:** `app/src/main/java/deltazero/amarok/xposed/`

Amarok is both an Android app and an Xposed module. The Xposed part is implemented in the `xposed` package.

- **Purpose:** Provides system-level app hiding by intercepting Android's PackageManager queries.
- **Limitations:** Does not hide apps from launchers; use with other app hiding modes.

#### Key Components

- **XposedEntry:** Module's main entry point.
- **FilterHooks:** Contains version-specific implementations.

#### Communication and Preference Management

The main app and the module run in different processes, so they communicate with each other through `XHidePrefBridge`.

- **XHidePrefBridge:**
    - Receives module status/version (`isModuleActive`, `xposedVersion`)
    - Observes `HiderStateRepository.hiddenApps` and `SettingsRepository.settings.xHideEnabled`
    - Sends updated module preferences with `commitNewValues()`
- **XPref:** Manages module-side cached preferences.

When hidden apps or XHide enablement changes, the bridge updates module preferences for system query filtering.

### 8. Quick Hide

Provides multiple triggers for instant hide/unhide operations:

- **Quick Settings Tile:** `QSTileService.java`
- **Quick Hide Service:** `QuickHideService.kt` owns the foreground service and observes settings plus `Hider.state` to start/stop/react to panic button changes
- **Auto Hide on Screen Off:** Implemented via `ScreenStatusReceiver.java` and `AutoHideUtil.java`
- **Intent API:** Managed by `ActionReceiver.java`
- **Widget:** `ToggleWidget.java`
