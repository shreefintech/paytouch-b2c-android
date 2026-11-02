package com.shreefintech.paytouchconsumer.auth

import android.content.Context
import android.content.Intent
import com.shreefintech.paytouchconsumer.HomeActivity
import com.shreefintech.paytouchconsumer.enums.NextStep
import com.shreefintech.paytouchconsumer.kyc.KycActivity

/** Single place that turns the server's `next_step` + `mpin_required` into the next screen (Splash, Login, MPIN setup). */
object NextStepRouter {

    fun intentFor(context: Context, nextStep: String?, mpinRequired: Boolean?): Intent = when {
        NextStep.from(nextStep)?.isKycStep == true -> Intent(context, KycActivity::class.java)
        mpinRequired == true                       -> ResetMpinActivity.buildCreateIntent(context)
        else                                       -> Intent(context, HomeActivity::class.java)
    }
}
