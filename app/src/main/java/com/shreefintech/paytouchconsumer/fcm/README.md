# fcm/

Firebase Cloud Messaging integration: receive push notifications and keep the device's FCM token
registered with the backend for the logged-in user.

## Files

| File                            | Role                                                                    |
|---------------------------------|-------------------------------------------------------------------------|
| `MyFirebaseMessagingService.kt` | FCM service — handles token rotation and incoming messages              |
| `NotificationHelper.kt`         | Channel, permission, notification display, token register/remove calls |

Related: request DTOs in `retrofit/model/notification/` (`DeviceTokenRequest.kt`, `DeviceTokenRemoveRequest.kt`),
keys `KEY_FCM_TOKEN` / `FCM_PLATFORM_ANDROID` in `Constant.kt`, channel strings
`labelNotificationChannelId` / `labelNotificationChannel`, and the service + default channel/icon
meta-data in `AndroidManifest.xml`.

## APIs

| Method   | Endpoint            | Body                                      | Response      |
|----------|---------------------|-------------------------------------------|---------------|
| `POST`   | `/api/device-token` | `DeviceTokenRequest` (`fcm_token`, `platform`, `device_id`) | `MessageItem` |
| `DELETE` | `/api/device-token` | `DeviceTokenRemoveRequest` (`fcm_token`)  | `MessageItem` |

Both need the bearer token. `DELETE` carries a JSON body, so it is declared with
`@HTTP(method = "DELETE", hasBody = true)` — plain `@DELETE` cannot send a body.

---

## Lifecycle

| When                                  | Call                                  | Where                           |
|---------------------------------------|---------------------------------------|---------------------------------|
| App start                             | `createChannel()`                     | `MyApp.onCreate()`              |
| Login success (any route)             | `syncToken()`                         | `LoginViewModel.saveSession()`  |
| Home opened (covers already-logged-in users) | `syncToken()`                  | `HomeActivity.onCreate()`       |
| Location flow finished (Home)         | `requestPermission()` — after location so the two system popups never overlap | `LocationPermissionHelper` callback in `HomeActivity` |
| FCM rotates the token                 | `syncToken(token)`                    | `onNewToken()`                  |
| Logout                                | `removeToken()` → then logout API     | `HomeViewModel.logout()`        |

### `syncToken(context, token?)`

Skips when logged out, offline, already in flight, or when the token equals the one saved under
`Constant.KEY_FCM_TOKEN` (last token the backend accepted). The key is only written after a
`success == true` response, so a failed call retries on the next trigger. `registerToken()` is
`@Synchronized` because `onNewToken` runs on an FCM worker thread while Login/Home sync on main.
`KEY_FCM_TOKEN` is wiped by `clearSharedPreference()` on logout, so the next login registers again.

### `removeToken(context, onDone)`

Must run **before** the logout API — logout invalidates the bearer token the delete call needs.
`onDone` is always invoked (success, failure, offline) so logout is never blocked.

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

- Session timeout and HTTP 401 clear SharedPreferences without calling `removeToken()` (the bearer
  token is already invalid). The next login re-registers the same `device_id`.
- The small icon is `img_paytouch` (intentional, verified working) — used in `NotificationHelper` and the
  manifest meta-data.
