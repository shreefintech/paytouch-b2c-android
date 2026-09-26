# KYC (Onboarding) Module

Handles the mandatory KYC onboarding flow: initiation, 4-step identity verification (personal details, Aadhaar, PAN, selfie), bank account submission, and KYC status display. All screens in this module are part of the first-run onboarding sequence — a user cannot reach `HomeActivity` without completing them.

---

## Getting Oriented

**Package:** `com.shreefintech.paytouchconsumer.kyc`
Sub-packages: `kyc/identity/` (4-step wizard + live selfie), `kyc/bank/` (bank card submission)

**First files to open:**
1. `KycActivity.kt` — hub that fetches KYC status first on every launch and routes accordingly; never navigates directly on first open
2. `KycViewModel.kt` — all routing logic: initiate → section A → hub buttons → agree → status
3. `identity/IdentityVerificationActivity.kt` — 4-step wizard; fragments driven by `currentStep` LiveData
4. `bank/BankDetailsActivity.kt` — dynamic 1–4 bank card layout inflated into `llBankContainer`

**Launched from:** `SplashActivity` / `LoginActivity` when `requires_kyc = true` on the session response

**Enums used:** `KycSubmissionStatus`, `KycSectionStatus`, `ProofType`, `StatementPeriod` — all in `enums/`

**After KYC completes:** User is routed to `ResetMpinActivity` (create MPIN mode), then `HomeActivity`

**Viewing KYC after onboarding:** `myaccount/KycDetailsActivity` (not in this package) — calls `GET /api/dashboard-kyc/my-account`

**Key quirk:** HTTP 422 from `initiateKyc` is treated as success (KYC was already initiated). Do not change this — see `KycViewModel.callInitiate()`.

---

## Screens & ViewModels

| Activity | ViewModel | Purpose |
|---|---|---|
| `KycActivity` | `KycViewModel` | Hub: fetches KYC status, auto-initiates, routes to Identity or Bank |
| `IdentityVerificationActivity` | `IdentityVerificationViewModel` | 4-step wizard: personal details → Aadhaar upload → PAN upload → selfie capture |
| `SelfieCaptureActivity` | — (`LivenessChallengeHelper`) | CameraX front-camera preview + ML Kit face detection; auto-captures the selfie after position → blink twice → hold still |
| `BankDetailsActivity` | `BankDetailsViewModel` | Dynamic bank cards (1–4); account number, IFSC, proof document upload |
| `KycStatusActivity` | `KycStatusViewModel` | Status display: pending GIF / rejected GIF / approved (routes to MPIN) |

Sub-screens (Fragment-based, inside `IdentityVerificationActivity`):

| Fragment | Step | Contents |
|---|---|---|
| `KycStep1Fragment` | 0 | Personal details (name, DOB, gender, address, PAN, Aadhaar number) |
| `KycStep2Fragment` | 1 | Aadhaar card upload (front + back) |
| `KycStep3Fragment` | 2 | PAN card upload (front) |
| `KycStep4Fragment` | 3 | Live selfie via `SelfieCaptureActivity` (blink liveness) |

All Activities live in `kyc/`, identity screens in `kyc/identity/`, bank screens in `kyc/bank/`.

---

## Package

`com.shreefintech.paytouchconsumer.kyc`

---

## Onboarding Sequence

```
SplashActivity / LoginActivity
    └── requires_kyc = true
            │
            KycActivity.startKyc()
                    │
                    GET /api/dashboard-kyc/status
                            │
                    ┌───────┴────────────────────────────────────┐
                    │ No entityType yet                          │ entityType present
                    │                                            │
              POST /api/dashboard-kyc/initiate            ┌──────┴────────────────────┐
                    │                                     │ Section A PENDING/REJECTED │ Other
                    │ 200 or 422 → section A              │                            │
                    │ 403 → registration pending msg      POST /api/dashboard-kyc/sections/a │
                    │                                     │                            │
                    └──────────────────────────► KycActivity shows hub ◄──────────────┘
                                                         │
                                        ┌────────────────┴───────────────┐
                                        ▼                                ▼
                              mcIdentity (section B)            mcBank (section C)
                                        │                                │
                          IdentityVerificationActivity         BankDetailsActivity
                                        │                                │
                                setResult(1) ──────── triggers startKyc() refresh
                                                                         │
                                           Both UNDER_REVIEW ──► agreeAndFetchStatus()
                                                                POST /api/dashboard-kyc/agree
                                                                GET  /api/dashboard-kyc/status
                                                                         │
                                                                KycStatusActivity
                                                                         │
                                             ┌───────────────────────────┤
                                             ▼                           ▼
                                        APPROVED                PENDING / REJECTED
                                             │                           │
                                    ResetMpinActivity          wait / retry button
```

---

## KycActivity — Hub Logic

`KycActivity` never navigates directly to `IdentityVerificationActivity` or `BankDetailsActivity` on first launch. It always calls `GET /api/dashboard-kyc/status` first and derives what's needed from the response:

| Condition | Action |
|---|---|
| No `entityType` | Call `POST /api/dashboard-kyc/initiate` (entity_type = "individual"), then proceed to section A |
| 422 from `initiateKyc` | Treat as success — KYC was already initiated; proceed to section A |
| 403 from `initiateKyc` | Show "pending registration" panel with server message |
| Section A PENDING or REJECTED | Call `POST /api/dashboard-kyc/sections/a` (has_gst = "0") |
| KYC already submitted | Go directly to `KycStatusActivity` |
| Both B + C are UNDER_REVIEW | Call `POST /api/dashboard-kyc/agree`, then `GET /api/dashboard-kyc/status` → `KycStatusActivity` |

`KycActivity` uses `ActivityResultLauncher` for both `IdentityVerificationActivity` and `BankDetailsActivity`. `resultCode == 1` from either child triggers a full `startKyc()` refresh.

The Bank card is only clickable after Identity is UNDER_REVIEW — disabled and dimmed (alpha 0.7) until then.

---

## IdentityVerificationActivity — 4-Step Wizard

4 steps driven by `IdentityVerificationViewModel.currentStep: LiveData<Int>`.

| Step | Fragment | ViewModel state |
|---|---|---|
| 0 | `KycStep1Fragment` | Personal details (name, DOB, gender, PAN, Aadhaar number, address) |
| 1 | `KycStep2Fragment` | Aadhaar front + back URIs |
| 2 | `KycStep3Fragment` | PAN front URI |
| 3 | `KycStep4Fragment` | Selfie URI (camera) |

Back press on step 0 → `setResult(resultCode); finish()` (resultCode = 0 = no change; resultCode = 1 = submitted).
Back press on steps 1–3 → `goToPreviousStep()`.

**Document upload** — the Activity hosts a `FilePickerUtil` (gallery/camera picker) and a `selfieLauncher` (launches `SelfieCaptureActivity` for result with the output file path). Fragments call `pickDocument(callback)` or `captureSelfie(callback)` on the Activity to avoid multiple launcher registrations.

**Selfie compression** — captured JPEG is compressed on `Dispatchers.IO` before the callback fires: quality steps down from 85 → 70 → 55 → 40 until the file is ≤2 MB. EXIF rotation is corrected before compression.

**Submit** — on step 3 continue, `submitIdentity()` reads all four byte arrays on `Dispatchers.IO` and calls `viewModel.submitIdentity()` which POSTs a multipart form to `api/dashboard-kyc/sections/b/signatory` (PAN, Aadhaar front/back, selfie). Success → `setResult(1); finish()`.

---

## SelfieCaptureActivity — Live Selfie (Liveness Check)

Launched for result by `IdentityVerificationActivity.captureSelfie()` with the output file path (`extra_output_path`). Returns `RESULT_OK` only after the liveness check passes and the JPEG is written; the host then runs its normal EXIF/2 MB compression.

| Piece | Role |
|---|---|
| `SelfieCaptureActivity` | CameraX front camera (preview + 640×480 analysis + capture capped at ~1920×1440), bundled ML Kit face detection (fully on-device), auto-capture |
| `LivenessChallengeHelper` | Pure-Kotlin state machine — no Android types, unit-testable with `LivenessFrameItem` |
| `widget/FaceCircleOverlayView` | Full-screen dim with a centred circle (0.75 × width) and 1dp outline; outline turns green after the blinks |

**Check sequence:** face centred + straight → **blink twice** (each: open → closed → open within 1.5 s) → hold still with eyes open → auto-capture. A lost face, second face or changed tracking ID restarts the check; 20 s timeout. Thresholds live in the helper's `companion object`.

**Capture resilience:** in-memory `takePicture` → one retry → fall back to saving the live analysis frame. If the device can't bind Preview + Capture + Analysis together, it binds Preview + Analysis only and uses the frame fallback.

**Permission:** `CAMERA` requested at runtime; if blocked ("Don't ask again") the user is sent to app settings. Both `android.hardware.camera` and `camera.front` are `required="false"` in the manifest so Play doesn't filter devices.

---

## BankDetailsActivity — Dynamic Bank Cards

Supports 1–4 bank accounts (controlled by `MAX_ACCOUNTS = 4`). Cards are dynamically inflated into `llBankContainer` from `item_bank_account.xml`. Each card has:
- Account number (9–18 digits), bank name, IFSC (regex-validated), branch name
- Proof type dropdown (`ProofType` enum: passbook, cancelled cheque, bank statement)
- Statement period dropdown (only if proof type = bank statement; `StatementPeriod` enum)
- Proof document upload (gallery/camera via `FilePickerUtil`)

**Validation** per card (in order): account number → bank name → IFSC format → branch name → proof type → statement period (if applicable) → proof URI. First failure shows a toast and returns false.

**Submit** — reads all proof URIs on `Dispatchers.IO` via `contentResolver`, then calls `viewModel.submit()` which POSTs a multipart form to `api/dashboard-kyc/sections/c`. Success → `setResult(1); finish()`.

`LiquidGlassEffect.attach()` is called individually on the upload button (`flUpload1`), edit button (`flEdit1`), and delete button (`flDelete1`) for each card — call sites use the card binding's root, not `binding.root`.

---

## KycStatusActivity

Receives a `KycStatusItem` as JSON via `EXTRA_STATUS` intent extra. Swipe-to-refresh triggers `viewModel.fetchStatus()` which re-fetches `GET /api/dashboard-kyc/status`.

| Status | UI | Action |
|---|---|---|
| `KYC_APPROVED` | — | Immediately navigates to `ResetMpinActivity` (stack cleared) |
| `KYC_SUBMITTED` | `gif_kyc_pending` GIF (plays once) | Shows pending message; no retry button |
| `KYC_REJECTED` | `gif_kyc_rejected` GIF (plays once) | Shows rejected message + Retry button |
| `PENDING_KYC` | — | Navigates back to `KycActivity` |

GIF is loaded via Glide with `setLoopCount(1)`.

---

## API Endpoints

All declared in `ApiService.kt` under the `// ── KYC ──` section.

| Method | Path | Purpose | Response model |
|---|---|---|---|
| GET | `api/dashboard-kyc/status` | Fetch full KYC status | `KycStatusItem` (flat, not `General<T>`) |
| POST | `api/dashboard-kyc/initiate` | Start KYC for a new user (multipart: `entity_type`) | `General<KycSubmissionDataItem>` |
| POST | `api/dashboard-kyc/sections/a` | Submit section A placeholder (multipart: `has_gst = "0"`) | `General<KycSubmissionDataItem>` |
| POST | `api/dashboard-kyc/sections/b/signatory` | Submit identity documents (multipart: PAN, Aadhaar ×2, selfie) | `General<KycSubmissionDataItem>` |
| POST | `api/dashboard-kyc/sections/c` | Submit bank documents (multipart: per-account fields + proof) | `General<KycSubmissionDataItem>` |
| POST | `api/dashboard-kyc/agree` | Mark KYC agreement (empty body) | `General<KycAgreeDataItem>` |

`KycStatusItem` is a **flat** response (no `General<T>` wrapper) — success check is `response.isSuccessful && body?.success == true`.

All multipart submissions use `@Multipart @POST` with `@Part` for byte arrays and `@Part` for string fields via `RequestBody.create("text/plain", value)`.

---

## KYC Models (`retrofit/model/kyc/`)

| Class | Purpose |
|---|---|
| `KycStatusItem` | Root response for `GET /api/dashboard-kyc/status` |
| `KycSubmissionStatusItem` | Nested: overall submission record (status, entityType, dates) |
| `KycSectionsItem` | Nested: sections A / B / C review records |
| `KycSectionStatusItem` / `KycSectionReviewItem` | Per-section status and review result |
| `KycSubmissionDataItem` | Response data for initiate / submit-a/b/c |
| `KycAgreeDataItem` | Response data for agree endpoint |
| `KycMyAccountItem` | Flat response for `GET /api/dashboard-kyc/my-account` — used by `KycDetailsActivity` in `myaccount/` |
| `KycDocumentsInfoItem` / `KycDocumentItem` / `KycDocumentDetailItem` | Document list inside `KycMyAccountItem` |
| `KycIdentityItem` / `KycBankInfoItem` / `KycBankAccountDetailItem` | Identity and bank fields inside `KycMyAccountItem` |
| `KycSignatoryDataItem` | Signatory details inside `KycMyAccountItem` |
| `KycVirtualAccountItem` | Virtual account details inside `KycMyAccountItem` |

---

## Enums

| Enum | Values | Used by |
|---|---|---|
| `KycSubmissionStatus` | `KYC_SUBMITTED`, `KYC_APPROVED`, `KYC_REJECTED`, `PENDING_KYC` | `KycActivity`, `KycStatusActivity` |
| `KycSectionStatus` | `PENDING`, `UNDER_REVIEW`, `REJECTED` | `KycActivity`, `KycViewModel` |
| `ProofType` | `PASSBOOK`, `CANCELLED_CHEQUE`, `BANK_STATEMENT` | `BankDetailsActivity` |
| `StatementPeriod` | `LAST_3_MONTHS`, `LAST_6_MONTHS` | `BankDetailsActivity` |

---

## Post-KYC: Viewing KYC Details

After onboarding is complete, the user can view their submitted KYC data from **My Account**. This is handled by `KycDetailsActivity` in `myaccount/`, not in this package.

| Activity | Package | ViewModel | Entry point |
|---|---|---|---|
| `KycDetailsActivity` | `myaccount/` | `KycDetailsViewModel` | `MyAccountActivity` → "View KYC Details" |

`KycDetailsViewModel` calls `GET /api/dashboard-kyc/my-account` (flat `KycMyAccountItem`). Document taps launch `DocPreviewActivity` (also in `myaccount/`) with `extra_file_url` and `extra_file_title`.
