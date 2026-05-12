# Target Utils Build-Only Characterization

Several utility behaviors are intentionally covered by compilation/assembly only because stable unit
tests would require brittle Android framework, static library, network, or device storage setup.

- `SDCardUtil`: `getSdCardPaths` and URI-to-SD-card resolution depend on `StorageManager`,
  `StorageVolume`, mounted external cache directories, API level, and readable filesystem roots.
- `PermissionUtil`: permission flows depend on `XXPermissions`, Material dialogs, user button
  interaction, and Android permission state.
- `SecurityUtil`: static lock/disguise state transitions are unit-tested; app/settings/Hider-state
  integration remains build-only.
- `AppInfoUtil`: private query matching and `AppInfo` record behavior are unit-tested; installed-app
  refresh/filter behavior remains build-only because it depends on `PackageManager`, app resources,
  and `AmarokApplication` repositories.
- `UpdateUtil`: update channel parsing and version comparison are unit-tested; network release
  fetching, current package version lookup, main-thread toast/dialog behavior, and browser intent
  launch remain build-only.

# Entrypoint Build-Only Characterization

The receiver/widget/tile entrypoints are covered by lightweight unit tests where behavior is stable
without app-wide initialization. The remaining behavior is intentionally characterized by successful
unit-test compilation and `assembleFossDebug` only:

- `AutoHideUtil`: the unique WorkManager name is unit-tested; enqueue/cancel behavior remains
  build-only because it depends on `AmarokApplication`, settings `LiveData`, `Hider` static state,
  and WorkManager runtime state.
- `ActionReceiver`: public action constants, manifest wiring, and invalid-action toast behavior are
  unit-tested; hide/unhide/toggle processing remains build-only because it invokes `Hider` and
  security-gated activity launches from app state.
- `ScreenStatusReceiver`: unrelated-action no-op behavior is unit-tested; screen-on/screen-off
  runtime handling remains build-only because it calls `SecurityUtil` static state and
  `AutoHideUtil` WorkManager integration.
- `DialerReceiver`: the current launch intent to `MainActivity` with `FLAG_ACTIVITY_NEW_TASK` is
  unit-tested.
- `ToggleWidget`: action string, manifest provider metadata, exported state, and initial observer
  flag are unit-tested; widget rendering and toggle processing remain build-only because they depend
  on `RemoteViews`, `AppWidgetManager`, `Hider`, and security state.
- `QSTileService`: manifest tile contract and initial observer flag are unit-tested; tile state
  rendering, click handling, and listener requests remain build-only because they depend on platform
  `TileService`, `Tile`, app settings, `Hider`, and OS-version-specific APIs.
