# QR Studio

An offline-first QR generator for Android, built with Kotlin and Jetpack Compose. QR Studio creates, styles, saves, and shares eight kinds of QR codes without an account, backend, analytics, or network permission.

> Everything stays on your device.

## Features

### Create

- Eight formats: Wi-Fi, vCard contact, URL, text, email, phone, SMS, and location.
- One reusable generator screen with format-specific forms, inline validation, and a live 250 ms debounced preview.
- Capacity feedback for long payloads, with an error state instead of a crash.
- Automatic draft persistence across rotation and process death.

### Customize

- Foreground/background colors with a live contrast check (warns below 3:1 and on inverted codes).
- Module styles: square, rounded, and dots. Finder patterns stay recognizable in every style.
- Export sizes: 512, 1024, or 2048 px.
- Error correction levels L, M, Q, and H.
- Optional center logo from the photo picker, copied into app-private storage. Logos force EC level H and are capped at 20% of the QR width.
- Quiet-zone margin from 2 to 10 modules.

### Save and share

- PNG saved to `Pictures/QR Studio` through MediaStore on API 29+.
- On API 24–28, PNG is written to the public Pictures directory after the `WRITE_EXTERNAL_STORAGE` runtime grant.
- Share sheet via FileProvider with a temporary read grant.
- PDF export with two layouts: full A4 page or compact 4 × 5 inch sticker, saved to `Downloads/QR Studio`.
- Copy the raw payload to the clipboard. Wi-Fi payloads are marked as sensitive on Android 13+.

### History

- Room-backed local history with generated thumbnails.
- Search by title or payload, favorites filter, and per-type filter chips.
- Swipe to delete with Undo, plus long-press multi-select with bulk delete and batch favorite.
- Reopen any saved code in the editor with all fields and style restored.

### Settings

- Theme: System, Light, or Dark.
- Defaults for new codes: error correction, image size, and foreground/background colors.
- Save-history toggle. Favorites are always kept.
- Clear all history behind a confirmation dialog, which also removes copied logo files.
- About section with the app version and privacy statement.

## Tech stack

| Area | Choice |
| --- | --- |
| Language | Kotlin 2.2.21 |
| UI | Jetpack Compose, Material 3, dynamic color on Android 12+ with light/dark fallback |
| Architecture | MVVM, Repository, unidirectional StateFlow |
| DI | Hilt 2.57.2 |
| Navigation | Navigation Compose 2.9.0, single activity |
| Persistence | Room 2.7.2, DataStore Preferences 1.1.7 |
| QR engine | ZXing core 3.5.3 only, with custom Canvas module painting |
| Build | Gradle Kotlin DSL, AGP 8.13.0, `libs.versions.toml` |
| SDK | minSdk 24, compileSdk/targetSdk 36 |

## Architecture

```
feature/            Compose screens + ViewModels (create, generator, history, settings)
domain/             Pure Kotlin: payload builders, validators, QR encoding, models, repository contracts
data/               Room, DataStore, MediaStore/PDF export, clipboard, repository implementations
core/               Theme, contrast calculation, QR painter, logo loader, shared UI components
navigation/         Destinations and the adaptive NavigationBar/NavigationRail host
di/                 Hilt modules and qualifiers
```

- Every screen exposes a single immutable `UiState` from a `StateFlow` and sends one-off events through a `Channel`.
- `GeneratorViewModel` runs two independent pipelines from the same input flow:
  - render: `debounce(250)` + `collectLatest` so stale renders are cancelled;
  - autosave: `debounce(700)` + `collect` so writes are serialized and never duplicate records.
- QR encoding and bitmap rendering run on `Dispatchers.Default`; Room and file I/O run on the injected IO dispatcher.
- The generator form is draft-serialized into `SavedStateHandle`, so rotation and process death do not lose input.
- Layouts adapt by width: bottom navigation below 840 dp, navigation rail above it, and a two-pane generator at 720 dp and wider.

## Payload formats

| Type | Format |
| --- | --- |
| Wi-Fi | `WIFI:T:<WPA|WEP|nopass>;S:<ssid>;P:<password>;H:<true|false>;;` |
| vCard | 3.0 with CRLF line endings: `BEGIN:VCARD`, `VERSION:3.0`, `N`, `FN`, optional `ORG`, `TITLE`, `TEL`, `EMAIL`, `URL`, `ADR`, `NOTE`, `END:VCARD` |
| URL | User text, prefixed with `https://` when no scheme is present |
| Text | Raw UTF-8 text |
| Email | `mailto:<address>?subject=<encoded>&body=<encoded>` |
| Phone | `tel:<number>` |
| SMS | `SMSTO:<number>:<message>` |
| Location | `geo:<latitude>,<longitude>` with range validation |

Escaping rules:

- Wi-Fi escapes `\`, `;`, `,`, `:`, and `"` in SSID and password. The password is omitted for `nopass`.
- vCard escapes commas, semicolons, and newlines, and skips empty fields. A name or phone number is required.
- Email subject and body are URL-encoded.
- URL input is validated and rejected when it cannot form a valid link.

## Privacy and permissions

- No `INTERNET` permission. The app cannot make network requests.
- No accounts, backend, cloud sync, analytics, ads, or in-app purchases.
- `allowBackup="false"`, so codes and Wi-Fi passwords are not included in Android backups.
- Only declared storage permission is `WRITE_EXTERNAL_STORAGE` with `maxSdkVersion="28"`, used solely for saving on older devices.
- MediaStore is used for API 29+ writes.
- Payloads and passwords are never logged.
- Wi-Fi payloads are flagged sensitive when copied on Android 13+.
- Imported logos are copied into app-private storage instead of keeping a transient content URI.

## Build and run

Requirements: JDK 17+ (the project was built with JDK 21), Android SDK 36, and an emulator or device with API 24+.

```bash
export JAVA_HOME=/path/to/jdk21
./gradlew assembleDebug
```

Install the debug build:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Unit tests:

```bash
./gradlew testDebugUnitTest
```

Instrumented tests (Room DAO + Compose Wi-Fi form). Set `ANDROID_SERIAL` when more than one device is connected:

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest
```

Release build with R8 and resource shrinking:

```bash
./gradlew assembleRelease
```

The release build is intentionally unsigned. Add your own signing configuration before distribution; do not commit signing secrets.

## Testing

- 48 local unit tests cover every payload builder, escaping, URL normalization, contrast calculation, capacity handling, and ViewModel validation/action behavior.
- ZXing round-trip tests decode rendered QR images to confirm payload fidelity.
- Instrumented tests cover Room query behavior (search, filters, favorites, deletion) and the Compose Wi-Fi form (including switching to an open network).
- Manual smoke tests were run on an API 36 emulator in portrait and landscape, in light and dark themes, including PNG/PDF export, share sheet, favorites, history swipe/undo/multi-select, theme switching, and process-death draft restoration.

## Project notes

- Room schema JSON is exported to `app/schemas`. The database is version 1; future versions must register a `Migration` in `QrMigrations` rather than enabling destructive upgrades.
- The only bundled font and text rendering are platform defaults; PDF output uses `PdfDocument` and `StaticLayout`.
- All user-facing strings live in `res/values/strings.xml`.
- The adaptive launcher icon includes a monochrome layer for themed icons.

## Assumptions and known limitations

- Network access is deliberately absent, so there is no QR scanning, cloud sync, or online validation in v1.
- Release signing is left to the distributor. The checked-in project produces an unsigned release APK.
- The logo picker imports the image as-is; there is no in-app cropping or background-removal tool. The renderer only scales and frames it.
- PDF layouts use a fixed A4 or sticker geometry with the app's title and type label; custom page sizes and vector output are out of scope.
- The app UI is English only. All copy is centralized in `strings.xml` so additional locales can be added without touching code.
- The preview is a raster bitmap at 640 px. Exports use the selected 512/1024/2048 px size with a minimum quiet zone.
- History autosave creates one record per draft and updates it in place; browsing between many drafts intentionally produces one entry per saved code, not a revision history.
