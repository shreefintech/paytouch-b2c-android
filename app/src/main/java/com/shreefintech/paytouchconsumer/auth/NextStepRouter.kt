package com.shreefintech.paytouchconsumer.auth

import android.content.Context
import android.content.Intent
import com.shreefintech.paytouchconsumer.HomeActivity
import com.shreefintech.paytouchconsumer.enums.NextStep
import com.shreefintech.paytouchconsumer.kyc.KycActivity
import com.shreefintech.paytouchconsumer.kyc.KycStatusActivity

/**
 * Single place that turns the server's `next_step` + `mpin_required` into the next screen (Splash, Login, MPIN setup, Home, 403).
 * `kyc_required` opens the full [KycActivity] flow; Aadhaar steps open Home, whose auth/me check shows the Aadhaar dialog;
 * review states open [KycStatusActivity].
 * Deliberate: a null or unrecognised `next_step` falls through to Home.
 */
object NextStepRouter {

    fun intentFor(context: Context, nextStep: String?, mpinRequired: Boolean?): Intent = when (NextStep.from(nextStep)) {
        NextStep.KYC_REQUIRED -> Intent(context, KycActivity::class.java)
        NextStep.PENDING_APPROVAL, NextStep.REGISTRATION_REJECTED -> KycStatusActivity.buildIntent(context, nextStep)
        NextStep.AADHAAR_REQUIRED, NextStep.KYC_RESUBMIT_REQUIRED -> Intent(context, HomeActivity::class.java)
        else -> if (mpinRequired == true) ResetMpinActivity.buildCreateIntent(context) else Intent(context, HomeActivity::class.java)
    }
}
