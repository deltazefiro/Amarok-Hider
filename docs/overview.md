# Amarok Project Overview

## Hiding Management

**Files:** `app/src/main/java/deltazero/amarok/core/Hider.kt`, `HiderController.kt`, `HiderStateRepository.kt`, `SettingsRepository.kt`

The DataStore-backed repositories own persisted facts. `HiderController` (`@Singleton`, Hilt-injected) owns active hider strategies, activation errors, transient processing state, and hide/unhide orchestration using coroutines. `Hider` is a thin static facade that delegates to `HiderController` for entrypoints that lack DI access.

- **Persisted configuration:** `SettingsRepository.settings: StateFlow<SettingsSnapshot>` owns settings such as hider modes, XHide flags, quick-hide settings, security settings, appearance, and updates.
- **Persisted hide model:** `HiderStateRepository` owns `managedApps`, `managedFolders`, `hiddenApps`, and `hiddenFolders`. Managed items are user configuration; hidden sets are the persisted result of successful `Hider` operations.
- **State:** `Hider.state: StateFlow<State>` — derived from repository-backed app/folder state plus transient processing sets. `Hider.getState()` exists for synchronous service/entrypoint reads.
- **Unified process:** `Hider.processAll(context, Hider.Action)` drives managed app + folder processing; blocking I/O runs on `Dispatchers.IO`.
- **Targeted operations:** `Hider.processApps()` and `Hider.processFolders()` handle explicit app/folder sets.
- **Per-item state:** `Hider.appStates` and `Hider.folderStates` are `StateFlow<Map<String, Hider.State>>`, derived from managed sets, hidden sets, and transient processing sets.
- **Mode source of truth:** hider modes are read from `SettingsRepository.settings`; ViewModels should use settings flows for display. `Hider` keeps only active strategy instances and observes settings changes to rebuild/reactivate them.
- **Strategy ownership:** `HiderController` receives `settingsRepo` and `hiderStateRepo` via Hilt; `Hider.init(context)` triggers it to build the selected app/file hiders, observe relevant settings, and attempt one activation pass. `switchAppHider()` / `switchFileHider()` only persist desired modes; the settings observer rebuilds/reactivates strategies.
- **State observation:** `Hider.stateLiveData` / `Hider.appStatesLiveData` expose LiveData bridges for UI observers.

## File Hiding Implementations

**Directory:** `app/src/main/java/deltazero/amarok/filehider/`

- **Interface:** `FileHider` (sealed interface) with `suspend fun activate()` and `suspend fun process(targetDirs, Hider.Action)`.
- **Mode enum:** `FileHiderMode` — `NONE`, `OBFUSCATE`, `NOMEDIA`, `CHMOD` with string keys.
- **Implementations (all implement `FileHider`):**
    - `NoneFileHider.kt` — no-op
    - `ObfuscateFileHider.kt` — Base64 filename encoding + optional content obfuscation
    - `NoMediaFileHider.kt` — creates/removes .nomedia files
    - `ChmodFileHider.kt` — changes file permissions via root
- **Factory:** `FileHider.build(context, mode)` creates instances from enum values.

## App Hiding Implementations

**Directory:** `app/src/main/java/deltazero/amarok/apphider/`

- **Interface:** `AppHider` (sealed interface) with `suspend fun activate()` and `suspend fun process(pkgNames, Hider.Action)`.
- **Mode enum:** `AppHiderMode` — `NONE`, `ROOT`, `SHIZUKU`, `DHIZUKU` with string keys.
- **Implementations (all implement `AppHider`):**
    - `NoneAppHider.kt` — no-op
    - `RootAppHider.kt` — pm disable/hide via libsu
    - `ShizukuAppHider.kt` — IPackageManager via Shizuku reflection
    - `DhizukuAppHider.kt` — DevicePolicyManager via Dhizuku
- **Factory:** `AppHider.build(context, mode, options)` creates configured instances from enum values.

## Core Types

- **`apphider/AppHider.kt`:** `AppHiderOptions` carries app-specific runtime policy like `disableOnly`.
- **`core/ActivationResult.kt`:** Data class — `ActivationResult(success, msgResId)`.
- **`utils/ShellExt.kt`:** Coroutine bridges — `Shell.Job.await()`, `awaitShell()`.

## Main UI — Single Activity with NavHost

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

## Settings and Persistence

- Settings persistence is handled by `SettingsRepository.kt` using Preferences DataStore. It exposes a typed `SettingsSnapshot` flow and setter methods.
- Hide-model persistence is handled by `HiderStateRepository.kt` using Preferences DataStore. It exposes managed/hidden app and folder flows plus managed-item mutation helpers.
- UI is implemented under `app/src/main/java/deltazero/amarok/ui/settings/`.
- `SettingsRoute.kt` wires the screen to `SettingsViewModel`, platform callbacks, and update checks.
- `SettingsScreen.kt` owns the scaffold and section ordering.
- `settings/sections/*.kt` holds one composable file per preference category.
- `SettingsState.kt` groups `SettingsUiState` into section-specific sub-states; `SettingsActions.kt` mirrors that with section action bundles.
- Business logic for settings is in `SettingsViewModel.kt`, which derives `SettingsUiState` from repository and `Hider` flows. Actions persist settings through repositories.
- Activation is flow-driven: changing persisted app/file hider mode updates `SettingsRepository`; `Hider` observes the settings flow, rebuilds the active strategy, and updates activation error flows.

## Preference Categories

Settings sections are defined in `settings/sections/`:
- Workmode, XHide, Privacy, Quick Hide, Appearance, Update, About

### To Add a New Setting

1. Add a key, field, default, decoder entry, and setter in `SettingsRepository.kt`.
2. Add the field to the appropriate section state in `SettingsState.kt` and a corresponding action method to `SettingsViewModel`.
3. Add the setting UI in the appropriate file under `settings/sections/`. Use `R.drawable.ic_null` as placeholder icon for new options.

## XHide (`docs/xhide.md`)

Optional companion app + libxposed module for PackageManager query filtering.

- Main app: AIDL client; pushes hidden-app snapshots and reads status.
- Module app: AIDL service; writes snapshots to remote prefs and relays hook status.
- system_server hooks: filter PackageManager results and inject the status sentinel.

## Quick Hide

Provides multiple triggers for instant hide/unhide operations:

- **Quick Settings Tile:** `QSTileService.kt`
- **Quick Hide Service:** `QuickHideService.kt` owns the foreground service and observes settings plus `Hider.state` to start/stop/react to panic button changes
- **Auto Hide on Screen Off:** Implemented via `ScreenStatusReceiver.kt` and `AutoHideUtil.kt`
- **Intent API:** Managed by `ActionReceiver.kt`
- **Widget:** `ToggleWidget.kt`
