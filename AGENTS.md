# Amarok-Hider

Amarok is a lightweight Android app that hides files and applications for casual privacy needs. Instead of encryption, it manages file and app visibility using various techniques.

Core Features
- **File Hiding:** Obfuscation, NoMedia, and Chmod modes.
- **App Hiding:** Supports Root, Shizuku, and Dhizuku modes.
- **XHide Module:** Uses Xposed to filter hidden apps from system queries.
- **Panic Button:** Floating button to trigger hide operations.
- **Quick Settings Tile:** Direct access for quick hide/unhide.
- **App Lock:** Protects with password/fingerprint.

## Documentations

We maintain development docs in `./docs` folder. Update it as the code change.  
Keep each file concise (within 300 lines), split if necessary.  
Always read @docs/overview.md before starting a task. Keep the overview as a concise index.

## Build

Requires JDK 21+:
```bash
./gradlew assemble  # Build APKs
# Output: app/build/outputs/apk/{flavor}/{debug|release}/Amarok-v{version}+{commit}-{flavor}.apk
# Example (foss debug): app/build/outputs/apk/foss/debug/Amarok-v0.10.0+caa1716-foss.apk
```

Note: `./gradlew build` includes lint checks (mostly missing translations warnings), use `assemble` instead.

## Guidelines

- For temporary files, always use `./tmp` folder under project root to avoid sandbox permission issues. Do not use `/tmp` or other system temp directories.
