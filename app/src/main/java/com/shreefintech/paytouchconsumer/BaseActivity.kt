package com.shreefintech.paytouchconsumer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.shreefintech.paytouchconsumer.utill.BetterActivityResult
import com.shreefintech.paytouchconsumer.utill.ToastType
import com.shreefintech.paytouchconsumer.utill.ToastUtil


open class BaseActivity : AppCompatActivity() {

    lateinit var mActivity: Activity
    lateinit var betterActivityResult: BetterActivityResult<Intent, ActivityResult>
//    private var noInternetView: LytNoInternetBinding? = null

    var retryCallback: (() -> Unit)? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mActivity = this
        betterActivityResult = BetterActivityResult.registerActivityForResult(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Transparent system bars
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Dark icons for light purple background
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

    }


/*    override fun setContentView(view: View?) {

        val root = FrameLayout(this)

        view?.let { root.addView(it) }


        noInternetView = LytNoInternetBinding.inflate(layoutInflater)
        LiquidGlassEffect.attach(
            targetView = noInternetView!!.frameBg,
            rootView = noInternetView!!.root as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion = 0f,
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )

        noInternetView?.let { it.btnRetry.attach(it.root as ViewGroup) }

        noInternetView?.btnRetry?.setOnClickListener {
            retryCallback?.invoke()
        }

        noInternetView?.root?.gone()

        root.addView(noInternetView!!.root)

        super.setContentView(root)
    }

    fun showNoInternet() {
        noInternetView?.root?.visible()
    }

    fun hideNoInternet() {
        noInternetView?.root?.gone()
    }*/

    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration)

        // Force font scale to default
        configuration.fontScale = 1.0f

        // Force display density to default (ignore display size/zoom setting)
        configuration.densityDpi = DisplayMetrics.DENSITY_DEVICE_STABLE

        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }

}