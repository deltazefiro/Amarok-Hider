# Quick Hide

Quick Hide is the umbrella for all the "trigger a hide/unhide from outside the main UI"
entrypoints. Every trigger funnels into the same `Hider.processAll(context, Action)` call, so
they stay consistent with each other and with the main screens.

## Triggers at a glance

| Trigger | File | What fires it |
|---------|------|---------------|
| Panic button | `QuickHideService.kt` | Floating overlay tap → `HIDE` |
| Quick Settings tile | `QSTileService.kt` | Tile tap → toggle |
| Home-screen widget | `widget/ToggleWidget.kt` | Widget tap → toggle |
| Intent API | `receivers/ActionReceiver.kt` | External broadcast → `HIDE`/`UNHIDE`/`TOGGLE` |
| Auto-hide on screen off | `receivers/ScreenStatusReceiver.kt` + `utils/AutoHideUtil.kt` | `ACTION_SCREEN_OFF` → delayed `HIDE` |

All of them read the shared `Hider.state` (`VISIBLE` / `HIDDEN` / `PROCESSING`) and guard against
acting while `PROCESSING`. Unhide paths route through `SecurityUtil.isUnlockRequired()` and launch
`SecurityAuthForQSActivity` when a lock is set.

## Startup wiring

`AmarokApplication.onCreate()` (after `settingsRepo`/`hiderStateRepo` are loaded and `Hider.init`):

```
Hider.init(this)
repositoryEntryPoint.quickHideController().init()   // reactive service authority
QSTileService.init(...)                              // request tile re-listen on state change
ToggleWidget.init(...)                               // refresh widgets on state change
registerReceiver(ScreenStatusReceiver, SCREEN_ON | SCREEN_OFF)
```

Each `init()` is idempotent and must run after `Hider` is initialized because they observe
`Hider`/`HiderController` state.

## QuickHideController (`core/QuickHideController.kt`)

`@Singleton`, Hilt-injected with `SettingsRepository` + `HiderController` (mirroring the
`HiderController` pattern). It is the **single authority** over the `QuickHideService` lifecycle — no
other code starts/stops the service. This replaced the old scattered static
`sync()`/`refresh()`/`startService()` API and its shared mutable `isServiceRunning`/`initialized`
booleans, which raced under rapid toggling and leaked overlay windows.

**When the service runs:**

```kotlin
val shouldRun = quickHideService && state != Hider.State.HIDDEN
```

This `shouldRun` flow is **debounced (~150 ms)** so rapid hide/unhide bursts collapse to one settled
decision — no start/stop churn. Rationale for stopping at `HIDDEN`: the panic button (and its
foreground notification) should leave no trace once everything is hidden.

**Reconciliation:** the service reports liveness back via `onServiceCreated()`/`onServiceDestroyed()`.
`reconcile()` only starts when `shouldRun && !serviceAlive && !startPending` and only stops when
running, so redundant starts (the old double `Service start.`) can't happen. Starting is guarded by
`NOTIFICATION_SERVICE` permission and wrapped in try/catch (background-start safety).

**Self-healing:** `onServiceDestroyed()` re-runs `reconcile()`, and a `ProcessLifecycleOwner`
`ON_START` observer re-asserts the desired state — so a service killed while the process survives is
restarted (instead of staying dead until the next state/setting change).

**`panicButtonState`** (`StateFlow<PanicButtonState>`): derived from `settings.panicButton`,
`settings.panicButtonColor`, and `Hider.state`. The running service renders from this; the controller
owns the derivation.

## QuickHideService (`QuickHideService.kt`)

A dumb, idempotent foreground host for the floating `SYSTEM_ALERT_WINDOW` panic button. It does **not**
decide when to run — the controller does.

- `onCreate`: build the single `EasyWindow` **once**, call `quickHideController.onServiceCreated()`,
  and collect `quickHideController.panicButtonState` to render that one overlay instance.
- `onStartCommand`: just `startForeground(...)`; returns `START_STICKY`.
- `onDestroy`: cancel the single overlay, call `onServiceDestroyed()`.

Because the overlay is created once and only shown/hidden/recolored (never reallocated per start),
start/stop cycles can no longer orphan a window.

**Panic button rendering** (`renderPanicButton`, only while the service runs):
- `!panicButton` setting → cancel overlay. `SYSTEM_ALERT_WINDOW` denied → auto-disables the setting.
- `PROCESSING` → shown red, disabled. Otherwise → shown, configured color, enabled.
- Position (`panicButtonY`, `panicButtonLeftEdge`) and color persist via `SettingsRepository`.

## QSTileService

`onStartListening()` maps `Hider.state` to tile label/state; `invertTileColor` flips active/inactive
coloring. `onClick()` toggles — `VISIBLE`→HIDE, `HIDDEN`→unhide (auth first if required). The static
`init()` observes `Hider.stateLiveData` and `invertTileColor`, calling
`TileService.requestListeningState()` so the tile re-renders on change (wrapped in try/catch because
QS is unavailable in work profiles).

## ToggleWidget

`AppWidgetProvider` whose `onReceive(ACTION_TOGGLE)` mirrors the tile toggle logic. `init()` observes
`Hider.stateLiveData` and repaints every widget instance: `PROCESSING` shows a spinner; otherwise a
moon/empty icon + black/white background reflects `VISIBLE`/`HIDDEN`.

## ActionReceiver (Intent API)

Broadcast receiver for `ACTION_HIDE` / `ACTION_UNHIDE` / `ACTION_TOGGLE`. Validates the action,
ignores while `PROCESSING`, and routes unhide/toggle-to-visible through the security gate. This is
also the `contentIntent` target of the QuickHideService notification (fires `ACTION_HIDE`).

## Auto-hide on screen off

`ScreenStatusReceiver` (registered at runtime for `SCREEN_ON`/`SCREEN_OFF`):
- `SCREEN_OFF` → `SecurityUtil.onTrigger(SCREEN_OFF, …)` (see lock/disguise) **and** `AutoHideUtil.setAutoHide()`.
- `SCREEN_ON` → `AutoHideUtil.cancelAutoHide()`.

`AutoHideUtil` enqueues a unique `AutoHideWorker` (`WorkManager`, `REPLACE`) delayed by
`autoHideDelay` minutes that runs `processAll(HIDE)`. Skipped if `autoHide` is off or state is already
`HIDDEN`; cancelled on screen-on unless already running.

## Related docs

See `overview.md` → Hiding Management for `Hider`/state, and the App Lock & Disguise section for the
`SecurityUtil` gating used by the unhide paths.
