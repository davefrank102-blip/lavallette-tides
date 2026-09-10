# Lavallette Tides

Sideloadable Android app for **local ocean tides** and **beach-day weather** in **Lavallette, NJ**.

- Kotlin + Jetpack Compose
- Ocean/beach-inspired UI with today's tide curve and a weekly highs/lows list
- Offline-friendly: last successful fetch is cached on device
- No accounts, no hosted backend, no end-user API keys

## Features

1. **Tides**
   - Current state (rising / falling)
   - Next high & next low (time + height, feet MLLW)
   - Today's tide curve (Compose Canvas chart)
   - Weekly view (~7 days of highs/lows)
2. **Weather** (Lavallette ~ 40.039, -74.050)
   - Current temperature, conditions, wind
   - Short hourly outlook useful for beach days
3. **UX**
   - Polished ocean palette, glass-style cards, refresh button
   - Cached last response for offline resilience

## Tide station

| Field | Value |
| --- | --- |
| **Station ID** | `8533071` |
| **Name** | Seaside Heights, ocean |
| **Source** | NOAA CO-OPS tide predictions API |
| **Why** | Closest **ocean** prediction station useful for Lavallette **beach** days (~6.7 miles south on the same barrier island). Nearer Barnegat Bay stations (e.g. Mantoloking `8532786`, Ocean Beach `8532885`) are geographically closer but only ~0.4 ft tidal range and do not reflect Atlantic ocean beach tides (~5 ft range). |

Predictions: [NOAA station 8533071](https://tidesandcurrents.noaa.gov/noaatidepredictions.html?id=8533071)

## Weather data

- **Open-Meteo** forecast API (no API key)
- Hardcoded coordinates: **40.039, -74.050** (Lavallette, NJ)
- Units: F, mph, America/New_York

## Requirements

- Android **8.0 (API 26)** or newer to run the APK
- To **build**: JDK 17+, Android SDK (API 35 / Build-Tools), or Android Studio Ladybug+

## Build (command line)

```bash
# From the project root (needs ANDROID_HOME or local.properties sdk.dir)
# If gradle-wrapper.jar is missing:
./scripts/fetch-wrapper-jar.sh
chmod +x gradlew
./gradlew assembleDebug
```

**Debug APK path:**

```text
app/build/outputs/apk/debug/app-debug.apk
```

Optional `local.properties` (Android Studio creates this automatically):

```properties
sdk.dir=/path/to/Android/sdk
```

On macOS Android Studio default SDK is often:

```text
~/Library/Android/sdk
```

## Build (Android Studio)

1. Open the project folder in Android Studio
2. Let Gradle sync finish
3. **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**
4. Or run on a device/emulator with the green Run button

## Sideload / install

### On the phone (file install)

1. Copy `app-debug.apk` to the phone
2. Enable install from unknown sources / Allow from this source for your file manager
3. Open the APK and install **Lavallette Tides**

### With adb

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

USB debugging must be enabled on the device.

> Note: the debug build uses application id `com.lavallette.tides.debug`.

## Gradle wrapper JAR

The repo includes `gradlew` / `gradlew.bat` and `gradle/wrapper/gradle-wrapper.properties`.
Binary `gradle-wrapper.jar` is restored with:

```bash
./scripts/fetch-wrapper-jar.sh
# downloads from https://github.com/gradle/gradle (tag v8.11.1)
```

Android Studio will also generate/sync the wrapper JAR on first open if needed.

## Project structure

```text
app/src/main/java/com/lavallette/tides/
  data/          # NOAA + Open-Meteo clients, cache, repository
  ui/            # Compose theme, charts, cards, home screen
  viewmodel/     # MainViewModel (MVVM)
  MainActivity.kt
```

## Tech stack

- AGP 8.7.x / Kotlin 2.0 / Compose BOM 2024.10
- Retrofit + OkHttp + Kotlin Serialization
- DataStore Preferences (cache)
- Min SDK 26 / Target/Compile SDK 35

## Privacy

- No analytics SDKs
- Network only to NOAA and Open-Meteo
- Cache stays on device

## License

Personal / sideload project for Lavallette tide & weather convenience. Tide predictions (c) NOAA/NOS/CO-OPS; weather (c) Open-Meteo.
