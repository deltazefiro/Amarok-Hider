# Target Utils Build-Only Characterization

These Java utilities are part of the migration safety set, but several behaviors are intentionally
covered by compilation/assembly only because stable unit tests would require brittle Android
framework, static library, network, or device storage setup.

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
