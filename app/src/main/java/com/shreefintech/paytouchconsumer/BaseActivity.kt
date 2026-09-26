package com.shreefintech.paytouchconsumer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.shreefintech.paytouchconsumer.auth.LoginActivity
import com.shreefintech.paytouchconsumer.databinding.LytNoInternetBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.BetterActivityResult
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.ToastUtil


open class BaseActivity : AppCompatActivity() {

    lateinit var mActivity: Activity
    lateinit var betterActivityResult: BetterActivityResult<Intent, ActivityResult>
    private var noInternetView: LytNoInternetBinding? = null
    private var noInternetRoot: FrameLayout? = null
    private var glassAttached = false

    protected var retryCallback: (() -> Unit)? = null

    private val sessionHandler = Handler(Looper.getMainLooper())
    private val sessionCheckRunnable = object : Runnable {
        override fun run() {
            checkSessionTimeout()
            if (SharedPreferenceHelper.isLoggedIn(this@BaseActivity)) {
                sessionHandler.postDelayed(this, SESSION_CHECK_INTERVAL_MS)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        mActivity = this
        betterActivityResult = BetterActivityResult.registerActivityForResult(this)

    }

    override fun setContentView(view: View?) {
        val root = FrameLayout(this)
        noInternetRoot = root
        view?.let { root.addView(it) }

        noInternetView = LytNoInternetBinding.inflate(layoutInflater)
        noInternetView?.btnRetry?.setOnClickListener { retryCallback?.invoke() }
        noInternetView?.root?.visibility = View.GONE
        noInternetView?.root?.let { root.addView(it) }

        super.setContentView(root)
    }

    fun showNoInternet() {
        val binding = noInternetView ?: return
        binding.root.visibility = View.VISIBLE
        if (!glassAttached) {
            val root = noInternetRoot ?: return
            LiquidGlassEffect.attach(
                targetView = binding.flNoInternet,
                rootView = root,
                cornerRadius = resources.getDimensionPixelSize(R.dimen.no_internet_bg_radius),
                distortion = 0f,
                strokeWidth = 1,
                strokeColor = ContextCompat.getColor(mActivity, R.color.white),
                blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
            )
            glassAttached = true
        }
        Glide.with(this)
            .asGif()
            .load(R.drawable.gif_no_internet)
            .placeholder(R.drawable.ic_file_not_found)
            .error(R.drawable.ic_file_not_found)
            .listener(object : RequestListener<GifDrawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<GifDrawable?>, isFirstResource: Boolean) = false
                override fun onResourceReady(resource: GifDrawable, model: Any, target: Target<GifDrawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                    resource.setLoopCount(1)
                    return false
                }
            })
            .into(binding.ivNoInternet)
    }

    fun hideNoInternet() {
        noInternetView?.root?.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        checkSessionTimeout()
        sessionHandler.postDelayed(sessionCheckRunnable, SESSION_CHECK_INTERVAL_MS)
    }

    override fun onPause() {
        super.onPause()
        sessionHandler.removeCallbacks(sessionCheckRunnable)
    }


    private fun checkSessionTimeout() {
        if (!SharedPreferenceHelper.isLoggedIn(this)) return
        val last = SharedPreferenceHelper.getSharedPreferenceString(
            this, Constant.KEY_LAST_INTERACTION, null
        )?.toLongOrNull() ?: return
        if (System.currentTimeMillis() - last > Constant.SESSION_TIMEOUT_MS) {
            SharedPreferenceHelper.clearSharedPreference(this)
            ToastUtil.showExpired(this, getString(R.string.errUnauthorized))
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    companion object {
        private const val SESSION_CHECK_INTERVAL_MS = 30_000L
    }

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