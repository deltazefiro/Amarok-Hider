# Xposed Build-Only Characterization

These Xposed classes are part of the Java-to-Kotlin migration safety set. The behaviors below are
intentionally covered by compilation and flavor assembly instead of JVM execution because stable unit
tests would require Xposed, EzXHelper, zygote, or Android framework/package-manager internals.

- `XPref`: preference key constants and default inactive cache behavior are unit-tested; real
  `XSharedPreferences` path resolution, file readability, reload, and cross-process preference reads
  remain build-only.
- `IHook`: the minimal `getName()`/`load()` interface contract is unit-tested.
- `FilterHookFactory`: class loading, missing-target fallback, method discovery,
  `HookFactory.createMethodHook`, callback execution, result replacement, and unhook-on-error remain
  build-only because the EzXHelper/Xposed loading path is not stable in JVM tests.
- `FilterHooks`: target SDK-to-class/method hook lists are characterized by compilation/assembly only
  because invoking them tries to resolve Android package-manager server classes that are not present in
  JVM tests.
- `FilterUtils`: filtering is characterized by compilation/assembly only because it depends on
  `XPref` runtime cache state and EzXHelper field lookup against Android `ApplicationInfo`/
  `PackageInfo`-like objects.
- `ParceledListSliceUtil`: `sliceToList` reflection against `getList()` is unit-tested; real
  `android.content.pm.ParceledListSlice` class loading and list-to-slice construction remain
  build-only.
- `XposedEntry`: the public self-hook method signature is unit-tested; zygote init, package dispatch,
  Android-version hook selection, self-hook field writes, and system hook loading remain build-only.
