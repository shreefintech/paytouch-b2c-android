# KYC (Onboarding) Module

Handles the mandatory KYC onboarding flow: initiation, 4-step identity verification (personal details, Aadhaar, PAN, selfie), bank account submission, and KYC status display. All screens in this module are part of the first-run onboarding sequence — a user cannot reach `HomeActivity` without completing them.

---

## Screens & ViewModels

| Activity | ViewModel | Purpose |
|---|---|---|
| `KycActivity` | `KycViewModel` | Hub: fetches KYC status, auto-initiates, routes to Identity or Bank |
| `IdentityVerificationActivity` | `IdentityVerificationViewModel` | 4-step wizard: personal details → Aadhaar upload → PAN upload → selfie capture |
| `BankDetailsActivity` | `BankDetailsViewModel` | Dynamic bank cards (1–4); account number, IFSC, proof document upload |
| `KycStatusActivity` | `KycStatusViewModel` | Status display: pending GIF / rejected GIF / approved (routes to MPIN) |

Sub-screens (Fragment-based, inside `IdentityVerificationActivity`):

| Fragment | Step | Contents |
|---|---|---|
| `KycStep1Fragment` | 0 | Personal details (name, DOB, gender, address, PAN, Aadhaar number) |
| `KycStep2Fragment` | 1 | Aadhaar card upload (front + back) |
| `KycStep3Fragment` | 2 | PAN card upload (front) |
| `KycStep4Fragment` | 3 | Selfie capture via system camera |

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

**Document upload** — the Activity hosts a `FilePickerUtil` (gallery/camera picker) and a `cameraLauncher` (system camera). Fragments call `pickDocument(callback)` or `captureSelfie(callback)` on the Activity to avoid multiple launcher registrations.

**Selfie compression** — captured JPEG is compressed on `Dispatchers.IO` before the callback fires: quality steps down from 85 → 70 → 55 → 40 until the file is ≤2 MB. EXIF rotation is corrected before compression.

**Submit** — on step 3 continue, `submitIdentity()` reads all four byte arrays on `Dispatchers.IO` and calls `viewModel.submitIdentity()` which POSTs a multipart form to `api/dashboard-kyc/sections/b/signatory` (PAN, Aadhaar front/back, selfie). Success → `setResult(1); finish()`.

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
