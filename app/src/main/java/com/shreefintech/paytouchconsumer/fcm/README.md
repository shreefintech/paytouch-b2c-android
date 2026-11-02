# fcm/

Firebase Cloud Messaging integration: receive push notifications and keep the device's FCM token
registered with the backend for the logged-in user.

## Files

| File                            | Role                                                                    |
|---------------------------------|-------------------------------------------------------------------------|
| `MyFirebaseMessagingService.kt` | FCM service — handles token rotation and incoming messages              |
| `NotificationHelper.kt`         | Channel, notification display, token register                           |

Related: request DTO `retrofit/model/notification/DeviceTokenRequest.kt`,
keys `KEY_FCM_TOKEN` / `KEY_FCM_TOKEN_AUTHED` / `FCM_PLATFORM_ANDROID` / `FCM_LANGUAGE_DEFAULT` in `Constant.kt`, channel strings
`labelNotificationChannelId` / `labelNotificationChannel`, and the service + default channel/icon
meta-data in `AndroidManifest.xml`.

## APIs

| Method   | Endpoint            | Body                                                        | Response      |
|----------|---------------------|-------------------------------------------------------------|---------------|
| `POST`   | `/api/device-token` | `DeviceTokenRequest` (`fcm_token`, `platform`, `device_id`, `language`) | `MessageItem` |

Bearer token is optional — `null` registers the device without linking to a user. After login,
`syncToken()` is called again with a bearer token so the server links the device to the account.

---

## Lifecycle

| When                                  | Call                                  | Where                           |
|---------------------------------------|---------------------------------------|---------------------------------|
| App start (Splash)                    | `createChannel()`                     | `MyApp.onCreate()`              |
| Splash redirect (logged out or in)    | `syncToken()`                         | `SplashActivity.redirect()`     |
| Login success (any route)             | `syncToken()`                         | `LoginViewModel.saveSession()`  |
| Home opened (covers already-logged-in users) | `syncToken()`                  | `HomeActivity.onCreate()`       |
| POST_NOTIFICATIONS permission         | `ActivityResultLauncher`              | `HomeActivity` (after location flow) |
| FCM rotates the token                 | `syncToken(token)`                    | `onNewToken()`                  |
| Logout                                | Logout API sends `fcm_token`; backend unlinks the device | `HomeViewModel.logout()` |

### `syncToken(context, token?)`

Skips when offline or already in flight. If the same token was registered while logged out
(`KEY_FCM_TOKEN_AUTHED = false`), it is re-sent after login with a bearer token so the server
links the device to the account. `registerToken()` is `@Synchronized` because `onNewToken` runs
on an FCM worker thread while Login/Home sync on main. Both `KEY_FCM_TOKEN` and
`KEY_FCM_TOKEN_AUTHED` are wiped by `clearSharedPreference()` on session timeout, and explicitly
cleared on logout, so the next login always registers as authenticated.

### `device_id`

`Settings.Secure.ANDROID_ID` — stable per signing key + device user and survives
`clearSharedPreference()`, unlike a generated UUID.

---

## Incoming messages

`onMessageReceived` reads `data["title"]` → `notification.title` → app name, and
`data["message"]` → `data["body"]` → `notification.body`. Tapping a notification resumes the app
(or starts it from `SplashActivity`). Screen-specific deep links are not implemented yet — they
depend on payload fields the backend has not defined.

| Payload type                 | App in foreground            | App in background / killed                         |
|------------------------------|------------------------------|----------------------------------------------------|
| Data-only                    | `onMessageReceived`          | `onMessageReceived`                                |
| Notification (± data)        | `onMessageReceived`          | Shown by the FCM SDK using the manifest default channel/icon — `onMessageReceived` is **not** called |

---

## Gotchas

- Session timeout and HTTP 401 clear SharedPreferences without calling the logout API (the bearer
  token is already invalid). The next login re-registers the same `device_id`.
- The small icon is `img_paytouch` (intentional, verified working) — used in `NotificationHelper` and the
  manifest meta-data.
