# Merchant Payout App – implementation notes

Android (Kotlin, Jetpack Compose) implementation of the six steps of the challenge. [`README.md`](README.md)
is the original brief; this file explains how to run the project, how it is built, and why.

## Running it

| What | How |
| --- | --- |
| Build | `./gradlew assembleDebug` (JDK 21; the Gradle daemon JVM is pinned in `gradle/gradle-daemon-jvm.properties`) |
| Unit tests | `./gradlew testDebugUnitTest` |
| Run | Android Studio, any emulator or device on **API 34+**. The mock API starts with the app. |
| Error states | A payout of `999.99` returns 503 (service unavailable), `888.88` returns 400 (insufficient funds). |
| Biometrics | Enrol a fingerprint on the emulator (Settings → Security), then simulate a touch from the extended controls, or with `adb emu finger touch 1`. |
| Screenshot warning | Take a screenshot on the payout screen (emulator toolbar, or `adb shell input keyevent KEYCODE_SYSRQ`). |

## What is covered

| Step | Where it lives |
| --- | --- |
| 1. Home: balances, 3 latest items, "Show More" | `ui/home`, `GetMerchantOverviewUseCase` |
| 2. Activity list: cursor pagination, infinite scroll, grouping by local day | `ui/activity`, `GetActivityPageUseCase`, `GroupActivityByDateUseCase` |
| 3. Payout form, validation, confirmation, success and failures | `ui/payout`, `domain/validation`, `SubmitPayoutUseCase` |
| 4. Device identity sent as `device_id` | `DeviceIdentityRepositoryImpl`, `SharedPreferencesDeviceIdStore` |
| 5. Biometrics above £1,000.00 | `BiometricPolicy` (rule), `ui/payout/biometric` (prompt) |
| 6. Screenshot awareness on the payout screen | `ui/payout/ScreenshotDetection.kt` |

## Architecture

Three layers; dependencies only point inwards (`ui` → `domain` ← `data`).

```
com.example.androidinterview
├── domain       Plain Kotlin, JDK only: models, rules, use cases, repository interfaces
│   ├── model        Money, Iban, ActivityItem, Payout, Outcome / DomainError …
│   ├── validation   PayoutAmountValidator, PayoutDraft
│   ├── security     BiometricPolicy
│   ├── repository   MerchantRepository, PayoutRepository, DeviceIdentityRepository
│   └── usecase
├── data         Retrofit API + DTOs, error translation, mappers, repository implementations
├── ui           Compose screens and their ViewModels (home, activity, payout, navigation, theme)
├── di           AppContainer: the dependency graph, wired by hand
└── mock         Provided mock server (untouched)
```

* **ViewModels** expose one immutable `StateFlow<UiState>`; screens collect it with
  `collectAsStateWithLifecycle()`. Every screen has distinct *loading / error / empty / success* states.
* **Errors are values.** Repositories return `Outcome<T>` (`Success` or `Failure(DomainError)`); HTTP, network and parsing
  failures are translated once, in `data/remote/ApiCall.kt`. Nothing above the data layer catches exceptions.
* **The client tolerates a growing API.** An activity type, activity status or payout status it has never seen degrades to
  `OTHER` / `UNKNOWN` (an unknown payout status is simply not final, so it keeps being followed) instead of failing the whole
  page. A currency is different: an amount cannot be shown or trusted without it, so an unsupported one still fails.
* **Dependency injection** is plain constructor injection with a small `AppContainer`. A DI framework would add build time
  and indirection for a graph of a dozen objects.

## Decisions worth knowing

* **Money is never a float.** Amounts are `Long` minor units (pence, cents) end to end; user input is parsed with
  `BigDecimal`. Formatting uses `NumberFormat` with UK conventions whatever the device language (`£5,000.00`, `-£979.74`).
* **The mock server start-up is racy.** It starts on a background thread, and reading `MockServerManager.baseUrl` before it
  has started would start it a second time with the wrong dispatcher. `InterviewApplication` therefore publishes the URL once
  `start()` has returned, and `BaseUrlInterceptor` re-targets requests (built against a placeholder host) after waiting for it.
  This is the only change made to the provided starter code besides the manifest and theme.
* **Pagination** is cursor-based with one rule: only an *idle* footer may request a page, so a page is never requested
  twice. Items are de-duplicated by id, and a page that brings nothing new ends paging instead of looping.
* **Grouping by day** uses the merchant's local zone, read from an injected `Clock` on each call (time zone changes are
  picked up and the rule is testable at midnight boundaries).
* **Payout flow** is an explicit state machine (`Editing → Confirming → [Authenticating] → Submitting → Succeeded/Failed`).
  Input is frozen after confirmation, a second confirmation is ignored, and Back is blocked while the request is on its way
  (leaving would destroy the ViewModel and lose the outcome of a payout the server may well have created), so a payout cannot be
  sent twice from the UI. "Try Again" keeps what was typed. The form values live in `SavedStateHandle` (they survive process
  death) and are cleared as soon as the payout exists, so a restored form never invites sending the same money again. The phase
  deliberately is not saved: after a kill the merchant lands on a form rather than in the middle of a submission whose outcome
  is unknown.
* **Amounts are read, never guessed.** The input field only filters characters (digits, `.`, `,`, a leading `-`); the
  validator decides what the separators mean: `1,250.50` (commas group thousands, UK style), `1250.5`, and `1250,5` (a lone
  comma followed by one or two digits is a decimal comma) are accepted, while ambiguous spellings such as `1,2500` or `1.250,50`
  are rejected with a message instead of silently becoming a different amount. A pasted `£5,000.00` is £5,000.00.
* **Payout status.** The API creates payouts as `pending` and moves them to `completed` a few seconds later, so the
  success screen first says *submitted* and polls `GET /api/payouts/:id` (every 2 s, at most 20 times) until the status is
  final. "Payout Completed" is only shown once the API says so; a payout that ends `failed` is reported as a failure. If
  polling gives up before a final status (slow payout, or the network kept failing) the screen stops the spinner, says so, and
  offers "Check again" rather than promising a result nobody is waiting for.
* **Biometrics.** The rule ("strictly above 100,000 minor units of the selected currency, no exchange rate") lives in
  `BiometricPolicy` and is unit-tested at the boundaries. The prompt is only ever requested by the UI layer, so ViewModels never hold an
  `Activity`; they receive a `BiometricResult`. Only `BIOMETRIC_STRONG` is accepted. Cancelling, lock-out, no sensor
  and "nothing enrolled" each end the payout with a specific message; for the last one an *Open Settings* button leads to
  the enrolment screen. A rotation during the prompt does not dismiss it.
* **Device identity.** The identifier comes from `GET /api/devices` and is persisted after the first success: the mock
  generates a new random id every time the process starts, which would otherwise make it unstable across launches. The
  preferences file is excluded from backups and device transfers (and backups are off), so the id never follows data to another device.
* **Screenshots** are handled in two complementary ways, both scoped to the payout screen. `FLAG_SECURE` is set while it is
  shown (and removed when it leaves the composition), so captures and screen recordings come out black and the Recents thumbnail
  is blank: `adb exec-out screencap` is black there and normal on Home. In addition, `Activity.ScreenCaptureCallback` (API 34,
  the project's `minSdk`, with the `DETECT_SCREEN_CAPTURE` permission) warns the merchant when a capture is attempted; the
  system still reports an attempt that `FLAG_SECURE` blocks, which was checked on an emulator.
* **Navigation** uses Navigation Compose with a bottom bar (Home / Payouts, as in the reference screenshots). The full
  activity list is a full-screen destination that slides over Home and is closed with "Done" or Back.
* **Accessibility.** Rows and balances are merged into single TalkBack items, titles are headings, error and loading text
  are live regions, the masked IBAN is announced as "starts with … ends with …", touch targets are at least 48 dp and
  text scales with the system font size. A dark theme is included.

## Assumptions on ambiguous requirements

* **Step 6 mixes two mechanisms.** The requirement and the reference screenshot (a capture is taken, followed by an alert)
  describe *detecting* a screenshot and warning, while the "Emulator testing" paragraph and the introduction describe
  *preventing* it with `FLAG_SECURE` (a black capture). Both are implemented, as they turn out to be compatible: the screen is
  protected, and the warning still fires when a capture is attempted.
* **Dates.** `DD MM YYYY` was read as `dd MMM yyyy` (`23 Jan 2026`), as in the screenshots, and the day groups
  (*Today / Yesterday / 18 May 2025*) from the brief were added while keeping the date on each row. The labels are
  recomputed when the screen comes back to the foreground, so "Today" does not go stale overnight.
* **IBAN.** Validated as ISO 13616: shape, exact length for SEPA countries and the UK, and the mod-97 checksum. The invalid
  example from the brief is rejected. An incomplete IBAN is only flagged once the field has been left, whereas one that is
  already wrong (forbidden character, too long, failed checksum) is flagged immediately.
* **"Confirm disabled when empty, zero or negative"** also covers an invalid IBAN. A negative amount can be typed or pasted:
  it is rejected with "Amount must be greater than zero", as are zero and anything unreadable. The disabled button also
  tells screen readers what is missing.
* **Device id** is read from the API rather than from `ANDROID_ID`, because the API documents it as the stable identity.
* **Step 5** mentions a "native bridge" and a "promise", which is wording from a React Native version of the challenge; the
  Android equivalent is `BiometricPrompt`.
* **Limits.** Amounts are capped at what the API's 32-bit integer can carry (about 21.4 million), and at two decimals.
* **The provided mock logs request bodies**, IBANs included, to Logcat: it must never be pointed at real data.

## Tests

Unit tests cover each layer, in the `should … when …` / Given-When-Then style:

* **Domain:** IBAN and amount validation, the biometric threshold at its boundaries, grouping by local day (time zone
  edges, merged pages, ordering), payout submission and status tracking (virtual time).
* **Data:** repositories against a real `MockWebServer` (request bodies and paths, DTO mapping, 503, insufficient funds,
  other 4xx, unreachable server, malformed bodies), device identity persistence, the base-URL interceptor.
* **UI:** every ViewModel (loading, errors, retry, pagination guards, the whole payout state machine including biometric
  outcomes, double confirmation, status polling and restoration from saved state), amount input filtering, formatters.

Instrumented Compose tests (`./gradlew connectedDebugAndroidTest`, on an emulator or device) cover what the merchant sees and
what each screen asks of its ViewModel: the stateless Home, activity list and payout screens in each of their states
(loading, error, empty, content, confirmation, submitted, completed, failed), inline validation messages and every button.
They need no network and no ViewModel, so they are fast and deterministic.

What is not covered automatically is system UI: the biometric prompt wrapper and the screenshot callback. These were
exercised by hand on emulators: the biometric flow (success, wrong finger, cancel, nothing enrolled, rotation during the
prompt), Back during submission, process death, dark mode, `FLAG_SECURE` and the screenshot warning.

## What would come next

* **Idempotency.** Send an idempotency key so a payout interrupted by process death or a lost response can be retried
  safely. Today an I/O failure after the request was written is reported as "no connection" and "Try Again" resends it, which
  could pay twice; a real client needs the key, or at least a distinct "outcome unknown, check before retrying" message.
* **Connectivity.** Observe network state to disable "Confirm" up front instead of failing after the attempt.
* **Caching.** Paging 3 with Room for an offline-capable activity list (both libraries are already declared).
* **More UI tests:** an end-to-end flow against the mock with a fake `BiometricAuthenticator` injected, and screenshot tests for
  the previews in `ui/preview`.
* **Biometric fallback.** Decide with the product team whether devices without a strong biometric may use the device credential.
* **Release build.** R8 is off in the starter; enabling it needs keep rules checked for Retrofit and kotlinx.serialization.
* **IBAN registry.** Per-country BBAN structure, and the full list of countries.
* **Localisation** of strings, and of amounts and dates according to the user's locale.

## AI assistance

This solution was built with an AI assistant (Claude Code). The `Co-Authored-By` trailers in the commit history say so, and
the decisions above are written down so that they can be discussed and challenged.
