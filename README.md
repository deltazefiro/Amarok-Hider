
![banner](https://github.com/user-attachments/assets/63a8fc76-30a0-4460-85be-4264b1f69334)

# Amarok-Hider

[![](https://img.shields.io/github/v/release/deltazefiro/Amarok-Hider?label=GithubRelease)](https://github.com/deltazefiro/Amarok-Hider/releases)
[![](https://img.shields.io/f-droid/v/deltazero.amarok.foss?color=blue)](https://f-droid.org/zh_Hans/packages/deltazero.amarok.foss/)
[![](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/deltazero.amarok.foss&color=orange)](https://apt.izzysoft.de/fdroid/index/apk/deltazero.amarok.foss)

🌐 **ENGLISH** | [简体中文](https://github.com/deltazefiro/Amarok-Hider/blob/main/README.zh.md)

## What is Amarok?

Amarok is a lightweight Android privacy tool designed to keep your personal files & apps away from prying eyes without the hassle of heavy encryption.

Traditional encryption tools are secure but slow — decrypting a 4GB movie or unlocking an encrypted app takes time. Amarok takes a different approach: it uses **file obfuscation** and **app hiding** to make your content effectively invisible to the casual snoopers, all without the performance penalty of heavy encryption.

> [!WARNING]
> **Amarok is NOT an encryption tool.**
> It hides files by scrambling file headers and names. While this stops file managers from seeing them, it **does not** cryptographically secure the data. Do not use Amarok to protect high-value secrets or sensitive data.

## Features

* **One-Click Privacy**: Instantly hide or unhide your selected files and apps.
* **High-Speed Performance**: Hides large files instantly by altering signatures (Non-root) or adjusting file permissions (Root).
* **Versatile App Hiding**:
  * **Root-Free Support**: Hides apps from the launcher (supports Shizuku, Dhizuku, and DSM modes).
  * **Root Support**: Leverages Xposed to *thoroughly* scrub apps from system menus and 3rd-party detection.
* **Panic Button**: A discreet floating button to instantly hide everything.
* **Quick Settings Tile**: Toggle visibility directly from your control center without opening the main app.
* **Secure Access**: Protect Amarok itself with a Password or Fingerprint lock.
* **Camouflage Mode**: Disguise Amarok as a Calendar app.

## Screenshots

<table>
  <tr>
    <td><img src="https://github.com/deltazefiro/Amarok-Hider/assets/41465688/b3fe6b18-cb3e-488b-81cb-ff5ed005664b" alt="Image 1"></td>
    <td><img src="https://github.com/deltazefiro/Amarok-Hider/assets/41465688/1a6f147c-286a-428c-9470-a469b4dd9f4e" alt="Image 2"></td>
    <td><img src="https://github.com/deltazefiro/Amarok-Hider/assets/41465688/9b61b94f-26f2-4457-b189-93c75a09e7d5" alt="Image 3"></td>
  </tr>
  <tr>
    <td><img src="https://github.com/deltazefiro/Amarok-Hider/assets/41465688/9ef70932-c242-4cc6-a84d-5b14ddf8a814" alt="Image 4"></td>
    <td><img src="https://github.com/deltazefiro/Amarok-Hider/assets/41465688/3702143d-dac5-435d-9615-323ada02c63e" alt="Image 5"></td>
    <td><img src="https://github.com/deltazefiro/Amarok-Hider/assets/41465688/a2016488-0c13-4144-93ed-5ca35179df79" alt="Image 6"></td>
  </tr>
</table>

## Download

[![](https://img.shields.io/github/v/release/deltazefiro/Amarok-Hider?label=GithubRelease)](https://github.com/deltazefiro/Amarok-Hider/releases)  
[![](https://img.shields.io/f-droid/v/deltazero.amarok.foss?color=blue)](https://f-droid.org/zh_Hans/packages/deltazero.amarok.foss/) (FOSS)  
[![](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/deltazero.amarok.foss&color=orange)](https://apt.izzysoft.de/fdroid/index/apk/deltazero.amarok.foss)  (FOSS) (Pre-release channel)

## Documentation & Usage

New to Amarok? Check out our documentation for setup guides on Root, Shizuku, Dhizuku, and other app hiders.

[![](https://img.shields.io/badge/AmarokDocs-ClickToView-brightgreen)](https://deltazefiro.github.io/Amarok-doc/en-US/)  

## Contributing

Thank you for dedicating your time to contribute to this project!
Contributions in all forms are welcomed, including reporting bugs, proposing new features, performing language translations, and submitting code development PRs.

> 🏗️ **Project Structure**  
> Please consult `.cursor/rules` for a detailed overview of the project structure. This file is designed to guide both **human contributors** and **AI assistants** in navigating the codebase effectively.

We use [weblate](https://hosted.weblate.org/engage/amarok-hider/) for translations.

<a href="https://hosted.weblate.org/engage/amarok-hider/">
<img src="https://hosted.weblate.org/widgets/amarok-hider/-/multi-auto.svg" alt="Translation status" />
</a>

## Credits

A massive thank you to the open-source community:

* **[aistra0528/Hail](https://github.com/aistra0528/Hail):** Core reference for the app hiding logic.
* **[RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku) & [iamr0s/Dhizuku](https://github.com/iamr0s/Dhizuku):** For enabling root-less permissions.
* **[Icongeek26](https://www.flaticon.com/authors/icongeek26) & [Freepik](https://www.freepik.com):** For the beautiful iconography.
* **[Jetbrains](https://www.jetbrains.com/community/opensource/#support):** For providing IDE support.

... and all the dedicated [contributors](https://github.com/deltazefiro/Amarok-Hider/graphs/contributors)!

## Disclaimers

**Amarok is provided "as is" without any warranties or conditions.**
The user is fully responsible for any harm, data loss, or consequences that may arise from using Amarok. Please backup important data before using hiding tools.
