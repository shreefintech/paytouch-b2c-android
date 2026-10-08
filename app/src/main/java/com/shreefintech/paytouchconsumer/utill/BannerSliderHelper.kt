package com.shreefintech.paytouchconsumer.utill

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager2.widget.ViewPager2
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.BannerAdp

class BannerSliderHelper(
    private val context: Context,
    private val viewPager: ViewPager2
) : DefaultLifecycleObserver {

    private val handler = Handler(Looper.getMainLooper())

    private val banners = listOf(
        R.drawable.img_banner_1, R.drawable.img_banner_2, R.drawable.img_banner_3,
        R.drawable.img_banner_4, R.drawable.img_banner_5, R.drawable.img_banner_6,
        R.drawable.img_banner_8, R.drawable.img_banner_9
    )

    private val autoScrollRunnable = Runnable { scrollToNext() }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) = scheduleNext()
    }

    init {
        viewPager.adapter = BannerAdp(context, banners)
        viewPager.registerOnPageChangeCallback(pageChangeCallback)
    }

    fun attachToLifecycle(owner: LifecycleOwner) = owner.lifecycle.addObserver(this)

    override fun onResume(owner: LifecycleOwner) = scheduleNext()
    override fun onPause(owner: LifecycleOwner) = handler.removeCallbacks(autoScrollRunnable)
    override fun onDestroy(owner: LifecycleOwner) {
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        handler.removeCallbacks(autoScrollRunnable)
    }

    private fun scheduleNext() {
        handler.removeCallbacks(autoScrollRunnable)
        handler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY_MS)
    }

    private fun scrollToNext() {
        val next = (viewPager.currentItem + 1) % banners.size
        viewPager.setCurrentItem(next, next != 0)
    }

    companion object {
        private const val AUTO_SCROLL_DELAY_MS = 3000L
    }
}
