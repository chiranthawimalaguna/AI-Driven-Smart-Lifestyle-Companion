# AI-Driven Smart Lifestyle Companion

Project skeleton for the CMP 7003 PRAC1 project. It compiles conceptually against
current stable library versions (Aug 2026) but **you still need to**:

## 1. Open it
Open the `AI-Driven-Smart-Lifestyle-Companion/` folder as a project in Android Studio (Koala or
newer). Let Gradle sync — it will download the dependencies listed in
`app/build.gradle.kts`.

## 2. Connect Firebase
1. Go to the Firebase console, create a project, add an Android app with package name
   `com.smartlifestyle.companion`.
2. Download `google-services.json` and place it in `app/` (same folder as
   `build.gradle.kts`). This file is **not** included here — it's project-specific and
   should never be committed publicly since it identifies your Firebase project.
3. Enable Authentication (Email/Password) and Firestore in the Firebase console.
4. In the Firestore console, open the **Rules** tab and paste in the contents of
   `firestore.rules` (at the project root) — this restricts every user to reading and
   writing only their own data (`users/{their-uid}/...`), enforced server-side by
   Firebase rather than just trusted client-side code.

## 3. Weather API key
Sign up for a free OpenWeatherMap API key, then add it to `local.properties`
(a placeholder line is already there - just replace the value):
```
WEATHER_API_KEY=your_key_here
```
This is read into `BuildConfig.WEATHER_API_KEY` automatically by `app/build.gradle.kts`
— never hard-code the key in source.

## 4. Health & activity data (device sensors, no external watch)
The app reads two real signals directly from the phone's own hardware, via
`DeviceSensorManager`:
- **Step count** — `Sensor.TYPE_STEP_COUNTER`
- **Ambient light** — `Sensor.TYPE_LIGHT` (the same sensor that drives auto-brightness),
  used to detect a dark room in the evening and boost "wind down"/"sleep" task
  suggestions

No external wearable, companion app, or third-party sync chain is involved, so
there's nothing to pair and nothing that can fail to sync on demo day. Heart rate and
sleep are generated as clearly-labelled **simulated** values (phones don't have a
heart-rate sensor) — see the doc comment in `DeviceSensorManager.kt` for the
reasoning. If a test device/emulator has no step or light sensor, the UI falls back
to manual entry via `HealthRepository.recordManualSteps()`.

## Data storage: Firestore (cloud) is primary, Room is a local buffer
- **Routine tasks** live directly in **Firestore** (`RoutineRepository`) — Firestore's
  Android SDK ships with offline persistence built in, so reads/writes work with no
  network and sync automatically once connectivity returns. There's no separate Room
  table for tasks any more; duplicating Firestore's own offline cache would just be
  extra code for no benefit.
- **Health/sensor data** is buffered in **Room** first (`HealthRepository`), since
  sensor events can arrive frequently and writing every single one straight to the
  cloud would be wasteful. Each reading is still pushed to Firestore as a durable
  cloud copy, best-effort — a failed push never blocks the UI, since Room already has
  what the user needs to see right now.
- Both approaches are legitimate cloud-database usage; the point worth making in your
  report is that the write frequency of the data informed *where* it's buffered, not
  whether it ends up in the cloud.

## What's implemented vs. left as a TODO
Implemented (architecture + core logic):
- Firestore as the primary cloud database for routine tasks, with security rules
  scoping every document to its owning user — `data/repository/RoutineRepository.kt`,
  `firestore.rules`
- Device sensor integration (real step counter + real ambient light sensor) with
  simulated heart-rate/sleep, a Room local buffer, Firestore cloud push, and a
  manual-entry fallback — `data/health/DeviceSensorManager.kt`,
  `data/repository/HealthRepository.kt`
- Rule-based adaptive recommendation engine, including an ambient-light-driven
  wind-down nudge and a real (not hardcoded) sedentary-time signal derived from
  step-count history — `domain/usecase/GetSmartSuggestionsUseCase.kt`,
  `HealthRepository.minutesSinceLastMovement()`
- MVVM wiring for the dashboard, including working UI for adding tasks (with
  quick-pick scheduling), manually entering steps, and marking tasks complete —
  `presentation/dashboard/`
- Firebase Auth (email/password sign-in and sign-up, with validation and a gated nav
  graph) — `data/remote/AuthRepository.kt`, `presentation/auth/`
- Weather + location integration, failing safely to a neutral state on any error —
  `data/repository/WeatherRepository.kt`
- Context-aware smart notifications via WorkManager — checks overdue tasks (via a
  one-shot Firestore query) and rain status every 2 hours, only notifying when
  there's something worth surfacing — `work/SmartNotificationWorker.kt`,
  `work/WorkScheduler.kt`
- Runtime permission requests for location and notifications — `MainActivity.kt`
- Unit tests for the recommendation engine covering every scoring rule (overdue,
  urgency, sedentary nudge, rain deprioritisation, wind-down boost, sort order) —
  `app/src/test/java/.../GetSmartSuggestionsUseCaseTest.kt`. Run these from Android
  Studio (right-click the test file → Run) or `./gradlew test` — they're plain JVM
  tests, no emulator needed.
- A full Material 3 theming pass — custom colour scheme, typography, and shape system
  (not the default out-of-the-box theme) — `presentation/theme/Color.kt`,
  `Theme.kt`, `Type.kt`
- Two additional screens plus bottom navigation tying the app together: a Trends
  screen with simple bar-chart visualisations of step/light/heart-rate history, and
  a Profile screen (account info, sign-out) — `presentation/history/`,
  `presentation/profile/`, `presentation/navigation/AppNavHost.kt`
- Dashboard visual polish — a top app bar, icons for each status readout, and an
  empty-state message when there are no tasks yet — `presentation/dashboard/DashboardScreen.kt`
- Gradle dependencies for the full stack (Compose, Room, Firebase, WorkManager,
  Retrofit, location, encrypted storage)

**Important — this has not been built or run.** Every file here is believed correct
by careful manual review (import completeness, brace/paren balance, consistent
naming across files), but none of it has been through an actual Kotlin/Gradle
compiler or run on a device/emulator — the environment this was written in has no
Android SDK or access to Google's Maven repositories. Budget real time for your
first Android Studio build: expect at least a few dependency-version or Gradle-sync
issues on the first attempt, which is normal for any project's first build, not a
sign something is fundamentally wrong. Fix errors top-to-bottom as Android Studio
reports them.

Left for you to build out (in priority order for the rubric):
1. Google Sign-In (currently only email/password is wired up — add the Google
   provider in Firebase console + `AuthRepository` if you want it)
2. A periodic retry/resync for failed Firestore pushes in `HealthRepository` (rare,
   but worth mentioning as a known limitation in your Evaluation section)
3. Further polish once it's running — real device testing of the theming, empty/error
   states for the Trends screen when Firestore has no data yet, form validation edge
   cases

## Architecture reference
See the layered diagram we discussed: Presentation → Business logic → Data, with the
Data layer as the single point of contact for Firebase, device sensors, and the
weather API. Use this as the basis for the System Architecture section of your report
— worth noting that the data layer now fronts *two* storage strategies (Firestore-
primary for tasks, Room-buffered-then-synced for sensor data), which is a good,
concrete example of matching architecture to data characteristics.
