# XHide

## Processes

- **Main app** (`deltazero.amarok*`): pure AIDL client. Builds hidden-app snapshots, pushes them to the module, reads status, renders UI. No Xposed or PackageManager sentinel probing.
- **Module app** (`deltazero.amarok.xhide`): owns `XHideSyncService`. Validates caller package names against `XHideContract.ALLOWED_MAIN_PACKAGES`, writes snapshots to libxposed remote prefs group `xhide`, and relays hook status to the main app.
- **system_server hooks**: read snapshots from remote prefs, filter package/application query results, and inject hook status into the module app's `getInstalledPackages` result.

## Channels

```text
Main app ── IXHideSyncService ──> Module app ── remote prefs ──> system_server hooks
Main app <─ status Bundle ─────── Module app <─ PM sentinel ───── system_server hooks
```

## Wire contracts

- AIDL stays unchanged: `pushSnapshot(Bundle): Boolean`, `getStatus(): Bundle`.
- Snapshot keys: `protocolVersion`, `enabled`, `hiddenPackages`, `mainAppPackage`, `mainAppVersionCode`, `updatedAt`.
- Status keys: `moduleActive`, framework fields, `lastSyncTime`, `hooksLive`, `hookCount`, `hookError`.
- Sentinel package: `deltazero.amarok.xhide.status`.
- Sentinel payload: `PackageInfo.packageName = SENTINEL_PACKAGE`, `longVersionCode = hookCount`, `versionName = hookError`.

## Status rules

Status precedence is fixed:

1. module package missing -> `NotInstalled`
2. module not bound by Xposed -> `NotActivated`
3. protocol mismatch -> `Incompatible`
4. no sentinel from system_server -> `PendingReboot`
5. sentinel reports error or zero hooks -> `Error`
6. sentinel reports successful hooks -> `Active`

`moduleActive` only proves the module app process was bound by Xposed. Hook liveness is proven only by the sentinel injected from system_server in the current boot.

## Retry rules

- Retries exist only to land the latest snapshot.
- Status never controls retry continuation.
- Retry is bounded by `XHideModuleBridge.RETRY_DELAYS_MS`.
- Refresh triggers: app startup, hidden-app/XHide-enabled changes, and opening XHide settings.

## Security boundary

- Access is package-name allowlisted to Amarok package variants.
- Signature matching is intentionally not required, so official apps can interoperate with self-built or resigned XHide modules.
- The main app never reads remote prefs and never talks to system_server hooks directly.
