package com.shreefintech.paytouchconsumer.onboarding.kyc.identity.fragment

import androidx.fragment.app.Fragment

/**
 * Contract for each identity-verification step. [IdentityVerificationActivity] calls
 * [validate] on Continue — the fragment validates its own fields, persists valid data
 * into the shared [IdentityVerificationViewModel], and reports whether the host may advance.
 */
abstract class BaseKycStepFragment : Fragment() {
    abstract fun validate(): Boolean
}
