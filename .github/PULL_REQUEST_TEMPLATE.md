## Summary
<!-- What does this PR do? Keep it short and focused. -->

## Type of Change
- [ ] Bug fix
- [ ] New feature / screen (UI only)
- [ ] New feature / screen (API wiring)
- [ ] Refactor / cleanup
- [ ] Assets only (images / GIF / WebP / drawable)
- [ ] Documentation
- [ ] Dependencies (build.gradle)

## How to Test
<!-- Steps to verify manually. Include every screen touched through a shared file, not just the main screen. -->
1.

---

## Developer Guidelines Agreement

- [ ] **I have read `CLAUDE.md` and confirm this PR complies with all guidelines. I take full responsibility for the changes introduced in this PR.**

---

## Self-Review Checklist

### Scope
- [ ] Every changed file relates to the ticket

### Architecture
- [ ] All Activities extend `BaseActivity` (never `AppCompatActivity` directly)
- [ ] State management and API calls live in ViewModel — navigation and UI binding live in Activity
- [ ] No business logic or network calls inside any Adapter
- [ ] No new Retrofit/OkHttpClient instance constructed outside `ApiClient`

### Lifecycle & Memory
- [ ] No `Activity` or `Context` stored as a field in a ViewModel or singleton
- [ ] No anonymous inner class capturing an Activity reference beyond its lifecycle
- [ ] No callbacks invoked after `onDestroy`

### Threading
- [ ] No heavy/blocking work on the main thread
- [ ] All UI updates happen on the main thread

### Error Handling
- [ ] `showProgress` (or equivalent) reset in **both** `onResponse` and `onFailure` — no spinner hangs forever
- [ ] User always receives feedback on failure — no silently swallowed errors
- [ ] `!!` not used on any value that can realistically be `null` at runtime

### Loading State
- [ ] Every button-triggered API call shows an in-button `ProgressBar`
- [ ] Button is guarded against re-tap while loading
- [ ] No full-screen overlay as the sole loading signal

### Build & Safety
- [ ] Builds cleanly — no new errors or warnings
- [ ] No `GlobalScope` used for coroutines
- [ ] No hardcoded credentials, tokens, or API keys in source
- [ ] No bearer token or user PII in `Log.d` / `Log.e`
- [ ] All unused imports, variables, and dead code removed
