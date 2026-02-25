# Amarok

Amarok is a lightweight Android app that hides files and applications for casual privacy needs. Instead of encryption, it manages file and app visibility using various techniques.

## Project Overview

See @.cursor/rules/overview.mdc

## Build

Requires JDK 21+:
```bash
./gradlew assemble  # Build APKs
# Output: app/build/outputs/apk/{flavor}/{debug|release}/Amarok-v{version}+{commit}-{flavor}.apk
# Example (foss debug): app/build/outputs/apk/foss/debug/Amarok-v0.10.0+caa1716-foss.apk
```

Note: `./gradlew build` includes lint checks (mostly missing translations warnings), use `assemble` instead.

## E2E Test

1. Launch AVD (if not already running)
    Check if device running with mcp `mobile_list_available_devices` (more reliable than `adb devices`). If not, start AVD with:
    ```bash
    emulator -avd android_16_avd -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect
    # Runs in background, wait ~30s until "mobile_list_available_devices" shows "emulator-*"
    ```
2. Install the latest build of the app with adb.
3. Spawn agent `mobile-e2e-tester` to test the app.
    - **Package name**: Use `deltazero.amarok.foss` for FOSS flavor builds (not `deltazero.amarok`)
    - **Note**: Each agent invocation should test **one** simple, focused task. For complex test plans, decompose into multiple simple tasks and run agents sequentially. The agent only test user-facing behavior (black-box approach).

## Agent Rules
- Update @.cursor/rules/overview.mdc to reflect structural changes. Keep it as a concise index.
- Modify @.claude/settings.local.json for permission updates. A restart of Claude Code is required to take effect.
- For temporary files, always use `./tmp` folder under project root to avoid sandbox permission issues. Do not use `/tmp` or other system temp directories.
