# API Call Guide — PayTouch Dashboard

How networking works in this project end-to-end.

---

## 1. Stack Overview

| Component | Role |
|---|---|
| `Retrofit 2.11` | HTTP client framework |
| `OkHttp 4` | Underlying HTTP engine + interceptor chain |
| `Gson 2.13` | JSON ↔ Kotlin model serialization (`Strictness.LENIENT`) |
| `ApiClient` | Singleton that builds and owns the `Retrofit` instance |
| `ApiService` | Retrofit interface — all endpoints declared here |
| `ApiHelper` | Error parsing utility |
| `General<T>` | Universal response wrapper |
| `CurlInterceptor` | Debug-only — logs every request as a curl command |

---

## 2. Mandatory Rules

These are hard rules — no exceptions.

| Rule | Detail |
|---|---|
| **Base URL in `Constants`** | All URLs and keys must live in `Constants.kt`. No inline strings anywhere. |
| **Only `ApiClient.apiService`** | Never construct `Retrofit` or `ApiService` directly. |
| **Only `SharedPreferenceHelper`** | Never call `SharedPreferences` directly — always via this helper. |
| **Only `ToastUtil.show()`** | Never use `Toast.makeText()` — always `ToastUtil`. |
| **Only `ApiHelper.parseErrorMessage()`** | Never write custom error string logic — always use this helper for non-2xx. |
| **Check internet first** | `Utility.isInternetAvailable()` must be called before every network request. |
| **No logic in adapters** | Adapters must not make network calls or hold business logic. |
| **No Context in ViewModel** | ViewModels must not store `Activity` or `Context` references. |

---

## 3. `ApiClient` — The Single Source of Truth

**File:** `retrofit/ApiClient.kt`

```kotlin
object ApiClient {

    private var baseUrl: String = Constants.BASE_URL   // always from Constants

    fun init(context: Context)           // call once in Application.onCreate()
    fun resetWithNewUrl(context: Context) // call after base URL is changed in settings

    val retrofit: Retrofit       // lazy-built, cached
    val apiService: ApiService   // lazy-built, cached
}
```

`resetWithNewUrl()` nulls both `_retrofit` and `_apiService`, forcing a full rebuild on next access.

---

## 4. OkHttp Interceptor Chain

### 4a. Header Interceptor (always active)

Automatically appends `Accept` and `Content-Type` to every outgoing request:

```kotlin
.addInterceptor { chain ->
    val request = chain.request().newBuilder()
        .addHeader("Accept", "application/json")
        .addHeader("Content-Type", "application/json")
        .build()
    chain.proceed(request)
}
```

> Auth token is **not** injected globally — it is passed per-endpoint via `@Header("Authorization")`.

### 4b. `CurlInterceptor` (DEBUG builds only)

```kotlin
if (BuildConfig.DEBUG) {
    clientBuilder.addInterceptor(CurlInterceptor())
}
```

Prints every request as a copy-pasteable `curl` command to Logcat tag `CURL`:

```
curl -X POST -H "Accept: application/json" -H "Authorization: Bearer <token>" \
  --data '{"field":"value"}' "https://base-url/api/endpoint"
```

**Timeouts:** connect / read / write all set to **60 seconds**.

---

## 5. `ApiService` — Endpoint Interface

**File:** `retrofit/ApiService.kt`

All endpoints are declared in one Retrofit `interface`. Route prefixes are constants defined inside a companion object — never inline strings:

```kotlin
interface ApiService {
    companion object {
        const val CLIENT = "api/mobile/client/"
        const val ADMIN  = "api/mobile/admin/"
    }
}
```

### 5a. JSON body — `@Body RequestBody`

Use when sending a JSON object in the request body.

```kotlin
// Declaration
@POST("${CLIENT}resource/action")
fun doSomething(
    @Header("Authorization") auth: String,
    @Body body: RequestBody
): Call<General<ResponseItem?>?>

// Usage — build body with Gson
val body = Gson().toJson(mapOf("paramOne" to value1, "paramTwo" to value2))
    .toRequestBody("application/json".toMediaTypeOrNull())
ApiClient.apiService.doSomething("Bearer $token", body).enqueue(...)
```

### 5b. Form fields — `@FormUrlEncoded` + `@Field`

Use when the endpoint expects `application/x-www-form-urlencoded`.

```kotlin
// Declaration
@FormUrlEncoded
@POST("${CLIENT}resource/submit")
fun submitForm(
    @Field("fieldOne") fieldOne: String,
    @Field("fieldTwo") fieldTwo: String
): Call<General<ResponseItem?>?>

// Usage
ApiClient.apiService.submitForm(value1, value2).enqueue(...)
```

### 5c. Auth header — `@Header("Authorization")`

Pass the token explicitly on every authenticated endpoint.

```kotlin
// Declaration
@GET("${CLIENT}resource/list")
fun getList(
    @Header("Authorization") auth: String
): Call<General<ArrayList<ResponseItem?>?>?>

// Usage
val auth = "Bearer ${SharedPreferenceHelper.getSharedPreferenceString(context, Constants.KEY_TOKEN, "")}"
ApiClient.apiService.getList(auth).enqueue(...)
```

### 5d. Path parameter — `@Path`

Use for resource identifiers embedded in the URL.

```kotlin
// Declaration
@GET("${CLIENT}resource/{id}/detail")
fun getDetail(
    @Header("Authorization") auth: String,
    @Path("id") id: Int
): Call<General<ResponseItem?>?>

// Usage
ApiClient.apiService.getDetail(auth, resourceId).enqueue(...)
```

### 5e. Query parameter — `@Query`

Use for optional or filterable list endpoints.

```kotlin
// Declaration
@GET("${ADMIN}resource/list")
fun getFilteredList(
    @Header("Authorization") auth: String,
    @Query("page") page: Int,
    @Query("filter") filter: String?
): Call<General<ResponseItem?>?>

// Usage
ApiClient.apiService.getFilteredList(auth, page = 1, filter = "active").enqueue(...)
```

### 5f. Multipart file upload — `@Multipart` + `@PartMap` + `@Part`

Use when uploading files alongside text fields.

```kotlin
// Declaration
@Multipart
@POST("${CLIENT}resource/upload")
fun uploadFiles(
    @Header("Authorization") auth: String,
    @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,  // text fields
    @Part files: List<MultipartBody.Part>                              // file parts
): Call<General<Any?>?>

// Usage — build parts
val fields = mapOf(
    "fieldOne" to "value1".toRequestBody("text/plain".toMediaTypeOrNull())
)
val fileParts = listOf(
    MultipartBody.Part.createFormData("fileKey", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
)
ApiClient.apiService.uploadFiles(auth, fields, fileParts).enqueue(...)
```

### 5g. Suspend / Coroutine variant

Use inside a `viewModelScope` or `lifecycleScope` coroutine.

```kotlin
// Declaration
@GET("${CLIENT}resource/info")
suspend fun getInfo(): Response<ResponseItem>

// Usage — inside coroutine
viewModelScope.launch {
    val response = ApiClient.apiService.getInfo()
    if (response.isSuccessful) { /* handle */ }
}
```

---

## 6. `General<T>` — Universal Response Wrapper

**File:** `retrofit/model/General.kt`

Every endpoint is typed as `Call<General<T>>`. Never create a custom response wrapper.

```kotlin
data class General<T>(
    val data: T?,         // actual payload — null on error
    val success: Boolean?,
    val meta: Any?,       // pagination info or extra metadata
    val message: String?
)
```

Always check `response.isSuccessful` (HTTP 2xx) before accessing `body()?.data`.

---

## 7. Standard API Call Pattern

This is the **only approved way** to make a network call in this project.

```kotlin
// Step 1 — always guard with internet check
if (!Utility.isInternetAvailable(mActivity)) return

// Step 2 — read token from SharedPreferenceHelper, never hardcode
val auth = "Bearer ${SharedPreferenceHelper.getSharedPreferenceString(mActivity, Constants.KEY_TOKEN, "")}"

// Step 3 — enqueue via ApiClient.apiService
ApiClient.apiService.someEndpoint(auth, body).enqueue(object : Callback<General<SomeItem>> {

    override fun onResponse(call: Call<General<SomeItem>>, response: Response<General<SomeItem>>) {
        if (response.isSuccessful) {
            val result = response.body()?.data
            // handle success
        } else {
            val msg = ApiHelper.parseErrorMessage(mActivity, response.code(), response.errorBody()?.string())
            ToastUtil.show(mActivity, msg)
        }
    }

    override fun onFailure(call: Call<General<SomeItem>>, t: Throwable) {
        ToastUtil.show(mActivity, t.localizedMessage)
    }
})
```

---

## 8. `ApiHelper.parseErrorMessage()` — Error Parsing

**File:** `retrofit/ApiHelper.kt`

```kotlin
ApiHelper.parseErrorMessage(context, response.code(), response.errorBody()?.string())
```

Resolution order (same as iOS behavior):

1. Deserialize error body → return `message` field if non-empty.
2. Map HTTP status code to a localized string (400, 401, 403, 404, 422, 500, …).
3. Fall back to `R.string.err_generic`.

---

## 9. Auth Token Flow

Token is stored via `SharedPreferenceHelper` after login and read on every authenticated call. It is **never** hardcoded or injected by a global interceptor.

```kotlin
// Store after login
SharedPreferenceHelper.setSharedPreferenceString(context, Constants.KEY_TOKEN, token)

// Read before any authenticated call
val auth = "Bearer ${SharedPreferenceHelper.getSharedPreferenceString(context, Constants.KEY_TOKEN, "")}"
```

---

## 10. Constants File Rule

All URLs, keys, and configuration strings must be defined in `Constants.kt`. No inline strings anywhere in the codebase.

```kotlin
// Constants.kt
object Constants {
    const val BASE_URL    = "https://your-api-base-url/"
    const val KEY_TOKEN   = "auth_token"
    const val KEY_BASE_URL = "base_url"
    // all other keys here
}
```

```kotlin
// Correct — read from Constants
private var baseUrl: String = Constants.BASE_URL

// Wrong — never do this
private var baseUrl: String = "https://hardcoded-url.com/"
```

---

## 11. Quick Reference

```
ApiClient.apiService            → only way to make API calls
Constants.kt                    → all URLs and keys — no inline strings
SharedPreferenceHelper          → only way to read/write SharedPreferences
Utility.isInternetAvailable()   → call before every network request
ApiHelper.parseErrorMessage()   → call for every non-2xx response
ToastUtil.show()                → only way to show messages to the user
General<T>                      → all responses wrapped here — never custom wrappers
@Header("Authorization")        → "Bearer <token>" passed per-endpoint, not globally
CurlInterceptor (DEBUG only)    → logcat tag: CURL
Timeouts                        → 60s connect / read / write
```
