# Amarok

Amarok is a lightweight Android app that hides files and applications for casual privacy needs. Instead of encryption, it manages file and app visibility using various techniques.

## Project Overview

See @.cursor/rules/overview.mdc

## Build

Requires JDK 17+:
```bash
./gradlew assemble  # Build APKs (requires dangerouslyDisableSandbox: true for Gradle wrapper)
# Output: app/build/outputs/apk/{flavor}/{debug|release}/*.apk
```

Note: `./gradlew build` includes lint checks (mostly missing translations warnings), use `assemble` instead.

## E2E Test

1. Launch AVD (if not already running)
    ```bash
    adb devices  # Check if emulator-5554 already exists
    # If not running:
    emulator -avd android_16_avd -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect &
    # Wait ~30s until "mobile_list_available_devices" shows "emulator-5554"
    ```
2. Get device ID: `mobile_list_available_devices` → `"emulator-5554"` (more reliable than `adb devices`)
3. Use this device ID in all mobile MCP tools (screenshot, launch app, click, swipe, etc.)
4. Use `mobile_list_elements_on_screen(device)` to find coordinates for clicking

## Agent Rules
- Update @.cursor/rules/overview.mdc to reflect structural changes. Keep it as a concise index.
- Modify @.claude/settings.local.json for permission updates. A restart of Claude Code is required to take effect.
