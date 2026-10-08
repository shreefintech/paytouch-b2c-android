package com.shreefintech.paytouchconsumer.rewards

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityLevelUpBinding
import com.shreefintech.paytouchconsumer.enums.RewardsTier
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.RewardsLevelItem
import android.media.AudioAttributes
import android.media.MediaPlayer

import com.shreefintech.paytouchconsumer.rewards.viewmodel.RewardsViewModel
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import java.util.Locale

class LevelUpActivity : BaseActivity() {

    companion object {
        private const val EXTRA_LEVEL_JSON = "extra_level_json"
        private const val EXTRA_PREV_CASHBACK = "extra_prev_cashback"

        fun start(context: Context, data: RewardsLevelItem?, previousCashback: Double? = null) {
            val i = Intent(context, LevelUpActivity::class.java)
            data?.let { i.putExtra(EXTRA_LEVEL_JSON, Gson().toJson(it)) }
            previousCashback?.let { i.putExtra(EXTRA_PREV_CASHBACK, it) }
            context.startActivity(i)
        }
    }

    private enum class Palette(
        @ColorRes val lightRes: Int,
        @ColorRes val midRes: Int,
        @ColorRes val darkRes: Int,
        @ColorRes val glowRes: Int
    ) {
        BRONZE(R.color.lu_bronze_light, R.color.lu_bronze_mid, R.color.lu_bronze_dark, R.color.lu_bronze_glow),
        SILVER(R.color.lu_silver_light, R.color.lu_silver_mid, R.color.lu_silver_dark, R.color.lu_silver_glow),
        GOLD(R.color.lu_gold_light, R.color.lu_gold_mid, R.color.lu_gold_dark, R.color.lu_gold_glow),
        PLATINUM(R.color.lu_platinum_light, R.color.lu_platinum_mid, R.color.lu_platinum_dark, R.color.lu_platinum_glow)
    }

    /** [Palette] resolved to color ints for the current tier. */
    private class Colors(val light: Int, val mid: Int, val dark: Int, val glow: Int)

    private lateinit var binding: ActivityLevelUpBinding
    private val viewModel: RewardsViewModel by viewModels()
    private val running = mutableListOf<Animator>()
    private var levelUpPlayer: MediaPlayer? = null
    private var data: RewardsLevelItem? = null
    private var prevCashback: Double? = null
    private lateinit var palette: Colors
    private var rankIndex = 0
    private var isPlatinum = false
    private var statusBarInset = 0

    private val dp get() = resources.displayMetrics.density

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLevelUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.onClickListener = onClickListener()
        palette = resolve(Palette.BRONZE)
        applyInsets()

        prevCashback = intent.getDoubleExtra(EXTRA_PREV_CASHBACK, -1.0).takeIf { it >= 0 }
        val fromIntent = intent.getStringExtra(EXTRA_LEVEL_JSON)?.let {
            runCatching { Gson().fromJson(it, RewardsLevelItem::class.java) }.getOrNull()
        }
        if (fromIntent != null) show(fromIntent) else fetchAndShow()

        playSound()
    }

    override fun onPause() {
        stopAll()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (data == null) return
        if (running.isEmpty() && animationsEnabled()) startLoops()
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener {
            when (it) {
                binding.btnClose -> {
                    if (Utility.stopClick()) return@OnClickListener
                    finish()
                }
                binding.btnReplay -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (data == null) return@OnClickListener
                    stopAll(); resetForEntrance(); playAll(); playSound()
                }
                binding.btnContinue -> {
                    if (Utility.stopClick()) return@OnClickListener
                    finish()
                }
            }
        }
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.flRoot) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            statusBarInset = bars.top
            binding.flStage.setPadding(0, bars.top, 0, 0)
            binding.llTopBar.setPadding(
                (16 * dp).toInt(), bars.top + (14 * dp).toInt(), (16 * dp).toInt(), 0
            )
            binding.llSheet.setPadding(
                (22 * dp).toInt(), (14 * dp).toInt(), (22 * dp).toInt(), bars.bottom + (14 * dp).toInt()
            )
            insets
        }
    }

    private fun capSheetHeight() {
        binding.llSheet.post {
            val pipsBottom = statusBarInset + (412 * dp).toInt() + (44 * dp).toInt()
            val screenH = resources.displayMetrics.heightPixels
            val maxSheetH = (screenH - pipsBottom - (4 * dp).toInt()).coerceAtLeast((200 * dp).toInt())
            if (binding.llSheet.measuredHeight > maxSheetH) {
                val continuePlusMargin = binding.btnContinue.measuredHeight + (12 * dp).toInt()
                val svMaxH = (maxSheetH - binding.llSheet.paddingTop - binding.llSheet.paddingBottom - continuePlusMargin)
                    .coerceAtLeast((80 * dp).toInt())
                binding.svSheet.layoutParams = binding.svSheet.layoutParams.apply { height = svMaxH }
                binding.svSheet.requestLayout()
            }
        }
    }

    private fun fetchAndShow() {
        if (!Utility.isInternetAvailable(mActivity)) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
            finish()
            return
        }
        viewModel.fetchLevel(
            onSuccess = { show(it) },
            onError = { msg -> ToastUtil.showDelete(mActivity, msg); finish() }
        )
    }

    private fun show(d: RewardsLevelItem) {
        data = d
        bind(d)
        capSheetHeight()
        resetForEntrance()
        playAll()
    }

    private fun bind(d: RewardsLevelItem) {
        val tier = RewardsTier.from(d.stage)
        isPlatinum = tier == RewardsTier.PLATINUM
        palette = resolve(
            when (tier) {
                RewardsTier.BRONZE -> Palette.BRONZE
                RewardsTier.SILVER -> Palette.SILVER
                RewardsTier.GOLD -> Palette.GOLD
                RewardsTier.PLATINUM -> Palette.PLATINUM
            }
        )
        rankIndex = when (d.level?.substringAfterLast('_')) {
            "3" -> 0; "2" -> 1; "1" -> 2; else -> 2
        }
        val label = d.label.orEmpty()

        binding.flRoot.setBackgroundResource(
            if (isPlatinum) R.drawable.bg_lu_screen_platinum else R.drawable.bg_lu_screen
        )

        binding.ivRays.imageTintList = ColorStateList.valueOf(palette.glow)
        binding.viewGlow.backgroundTintList = ColorStateList.valueOf(palette.glow)
        binding.viewRing1.backgroundTintList = ColorStateList.valueOf(palette.glow)
        binding.viewRing2.backgroundTintList = ColorStateList.valueOf(palette.glow)
        sparkles().forEach { it.imageTintList = ColorStateList.valueOf(palette.light) }

        binding.tvLevelPill.setText(if (isPlatinum) R.string.labelLevelUpHighest else R.string.labelLevelUpPill)
        binding.tvLevelPill.setTextColor(palette.light)
        binding.tvLevelPill.background = GradientDrawable().apply {
            cornerRadius = 14 * dp
            setColor(withAlpha(palette.glow, 0.18f))
            setStroke((1 * dp).toInt(), withAlpha(palette.glow, 0.6f))
        }

        Glide.with(this).load(tier.rankShieldRes).into(binding.ivShield)

        binding.tvTitle.text = getString(R.string.titleLevelUpYouAre, label)
        if (isPlatinum) {
            binding.tvTitle.post {
                val w = binding.tvTitle.paint.measureText(binding.tvTitle.text.toString())
                binding.tvTitle.paint.shader = LinearGradient(
                    0f, 0f, w, 0f,
                    intArrayOf(
                        color(R.color.lu_platinum_title_start),
                        color(R.color.lu_platinum_title_mid),
                        color(R.color.lu_platinum_title_end)
                    ),
                    null, Shader.TileMode.CLAMP
                )
                binding.tvTitle.invalidate()
            }
        } else {
            binding.tvTitle.paint.shader = null
        }
        binding.tvSubtitle.text = getString(subtitleFor(d.level))

        binding.llPips.isVisible = !isPlatinum
        binding.llMedals.isVisible = isPlatinum
        if (!isPlatinum) bindPips()

        binding.tvSheetLabel.setText(
            if (isPlatinum) R.string.labelLevelUpPermanentCashback else R.string.labelLevelUpNewCashback
        )
        binding.tvCash.text = getString(R.string.fmtPercent, d.cashbackPercent ?: 0.0)
        val old = prevCashback ?: previousLevelCashback(d)
        binding.tvCashOld.isVisible = old != null
        old?.let {
            binding.tvCashOld.text = getString(R.string.fmtPercent, it)
            binding.tvCashOld.paintFlags = binding.tvCashOld.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }
        binding.tvCashNote.setText(
            when {
                isPlatinum -> R.string.msgLevelUpCashNotePlatinum
                d.cashbackActive == true -> R.string.msgLevelUpCashNoteActive
                else -> R.string.msgLevelUpCashNoteFestive
            }
        )

        binding.llProgress.isVisible = !isPlatinum
        binding.llPermanent.isVisible = isPlatinum
        if (!isPlatinum) bindProgress(d)
    }

    private fun bindPips() {
        listOf(binding.tvPip0, binding.tvPip1, binding.tvPip2).forEachIndexed { i, tv ->
            if (i == rankIndex) {
                tv.background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR, intArrayOf(palette.light, palette.mid)
                ).apply { cornerRadius = 22 * dp }
                tv.setTextColor(palette.dark)
                tv.typeface = Typeface.create(tv.typeface, Typeface.BOLD)
            } else {
                tv.setBackgroundResource(R.drawable.bg_lu_pip_off)
                tv.setTextColor(color(R.color.lu_pip_off_text))
            }
        }
    }

    private fun bindProgress(d: RewardsLevelItem) {
        val tierName = d.stage.orEmpty().replaceFirstChar { it.titlecase(Locale.ROOT) }
        binding.tvTierProgress.text = getString(R.string.labelLevelUpTierProgress, tierName)
        binding.tvTierCount.text = getString(R.string.labelLevelUpOfThree, rankIndex + 1)

        segments().forEachIndexed { i, seg ->
            seg.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(palette.mid, palette.dark)
            ).apply { cornerRadius = 4 * dp }
            seg.tag = i <= rankIndex
        }

        val next = d.next
        binding.llNext.isVisible = next != null
        if (next != null) {
            val head = getString(R.string.labelLevelUpNext, next.label.orEmpty())
            val target = next.target ?: 0.0
            val req = when (next.metric) {
                "txn_amount" -> getString(R.string.msgLevelUpReqTxn, rupees(target))
                "referrals" -> getString(R.string.msgLevelUpReqReferrals, target.toLong().toString())
                "locked_amount" -> getString(R.string.msgLevelUpReqLocked, rupees(target))
                "gold_1_days" -> getString(
                    R.string.msgLevelUpReqGoldDays,
                    (target.takeIf { it > 0 } ?: 90.0).toLong().toString()
                )
                else -> ""
            }
            binding.tvNext.text = SpannableStringBuilder(head).apply {
                setSpan(StyleSpan(Typeface.BOLD), 0, head.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (req.isNotEmpty()) append(" · ").append(req)
            }
        }
    }

    private fun previousLevelCashback(d: RewardsLevelItem): Double? {
        val h = d.history.orEmpty()
        if (h.size < 2) return null
        return RankCashbackTable.cashbackFor(h[h.size - 2]?.level)
    }

    private fun subtitleFor(level: String?): Int = when (level?.lowercase(Locale.ROOT)) {
        "bronze_3" -> R.string.msgLevelUpSubBronze3
        "bronze_2" -> R.string.msgLevelUpSubBronze2
        "bronze_1" -> R.string.msgLevelUpSubBronze1
        "silver_3" -> R.string.msgLevelUpSubSilver3
        "silver_2" -> R.string.msgLevelUpSubSilver2
        "silver_1" -> R.string.msgLevelUpSubSilver1
        "gold_3" -> R.string.msgLevelUpSubGold3
        "gold_2" -> R.string.msgLevelUpSubGold2
        "gold_1" -> R.string.msgLevelUpSubGold1
        else -> R.string.msgLevelUpSubPlatinum
    }

    // ─── MOTION ───

    private fun animationsEnabled(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ValueAnimator.areAnimatorsEnabled()
        else Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

    private fun resetForEntrance() {
        val fadeUps = listOf(
            binding.tvLevelPill, binding.tvTitle, binding.tvSubtitle,
            binding.llPips, binding.llCashBlock, binding.llProgress, binding.llPermanent
        )
        fadeUps.forEach { it.alpha = 0f; it.translationY = 26 * dp }
        binding.flBadgePop.scaleX = 0f; binding.flBadgePop.scaleY = 0f; binding.flBadgePop.rotation = -25f
        binding.llSheet.translationY = 600 * dp
        segments().forEach { it.scaleX = 0f }
        medals().forEach { it.scaleX = 0f; it.scaleY = 0f }
        sparkles().forEach { it.alpha = 0f }
        binding.viewRing1.alpha = 0f; binding.viewRing2.alpha = 0f
    }

    private fun showEndState() {
        listOf(
            binding.tvLevelPill, binding.tvTitle, binding.tvSubtitle,
            binding.llPips, binding.llCashBlock, binding.llProgress, binding.llPermanent
        ).forEach { it.alpha = 1f; it.translationY = 0f }
        binding.flBadgePop.scaleX = 1f; binding.flBadgePop.scaleY = 1f; binding.flBadgePop.rotation = 0f
        binding.llSheet.translationY = 0f
        segments().forEach { it.scaleX = if (it.tag == true) 1f else 0f }
        medals().forEach { it.scaleX = 1f; it.scaleY = 1f }
    }

    private fun playSound() {
        stopSound()
        try {
            val afd = resources.openRawResourceFd(R.raw.level_up) ?: return

            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { it.release(); levelUpPlayer = null }
            mp.setOnErrorListener { m, _, _ -> m.release(); levelUpPlayer = null; true }
            mp.prepareAsync()
            levelUpPlayer = mp
        } catch (e: Exception) {
            Utility.logError(e)
        }
    }

    private fun stopSound() {
        levelUpPlayer?.release()
        levelUpPlayer = null
    }

    private fun playAll() {
        if (!animationsEnabled()) { showEndState(); return }
        playEntrance()
        startLoops()
        binding.confetti.burst(
            intArrayOf(palette.light, palette.mid, color(R.color.primary), color(R.color.secondary), Color.WHITE)
        )
    }

    private fun fadeUp(v: View, delay: Long, dur: Long = 500L): Animator =
        ObjectAnimator.ofPropertyValuesHolder(
            v,
            PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 26 * dp, 0f)
        ).apply { startDelay = delay; duration = dur; interpolator = DecelerateInterpolator() }

    private fun playEntrance() {
        val set = AnimatorSet()
        val parts = mutableListOf<Animator>()

        parts += fadeUp(binding.tvLevelPill, 150)
        parts += ObjectAnimator.ofPropertyValuesHolder(
            binding.flBadgePop,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.ROTATION, -25f, 0f)
        ).apply { startDelay = 300; duration = 900; interpolator = OvershootInterpolator(2.2f) }
        parts += fadeUp(binding.tvTitle, 550, 550)
        parts += fadeUp(binding.tvSubtitle, 680, 550)
        if (!isPlatinum) parts += fadeUp(binding.llPips, 800)

        binding.llSheet.post { binding.llSheet.translationY = binding.llSheet.height.toFloat() }
        parts += ObjectAnimator.ofFloat(binding.llSheet, View.TRANSLATION_Y, 600 * dp, 0f)
            .apply { startDelay = 900; duration = 700; interpolator = DecelerateInterpolator(1.6f) }
        parts += fadeUp(binding.llCashBlock, 1250)
        parts += fadeUp(if (isPlatinum) binding.llPermanent else binding.llProgress, 1450)

        if (!isPlatinum) {
            segments().filter { it.tag == true }.forEach { seg ->
                parts += ObjectAnimator.ofFloat(seg, View.SCALE_X, 0f, 1f)
                    .apply { startDelay = 1650; duration = 700; interpolator = DecelerateInterpolator() }
            }
        } else {
            medals().forEachIndexed { i, m ->
                parts += ObjectAnimator.ofPropertyValuesHolder(
                    m,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f)
                ).apply { startDelay = 1000L + i * 180L; duration = 600; interpolator = OvershootInterpolator(2.2f) }
            }
        }

        set.playTogether(parts)
        set.start()
        running += set
    }

    private fun startLoops() {
        binding.ivRays.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        running += ObjectAnimator.ofFloat(binding.ivRays, View.ROTATION, 0f, 360f).apply {
            duration = 22_000L; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator(); start()
        }
        running += ObjectAnimator.ofFloat(binding.flBadge, View.TRANSLATION_Y, 0f, -9 * dp).apply {
            duration = 1_600L; startDelay = 1_300L
            repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator(); start()
        }
        listOf(binding.viewRing1 to 1_000L, binding.viewRing2 to 2_200L).forEach { (ring, delay) ->
            ring.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            running += ObjectAnimator.ofPropertyValuesHolder(
                ring,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.55f, 1.9f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.55f, 1.9f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.9f, 0f)
            ).apply {
                duration = 2_400L; startDelay = delay
                repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.RESTART
                interpolator = DecelerateInterpolator(); start()
            }
        }
        sparkles().forEachIndexed { i, s ->
            running += ObjectAnimator.ofPropertyValuesHolder(
                s,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f, 0f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.3f, 1f, 0.3f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.3f, 1f, 0.3f),
                PropertyValuesHolder.ofFloat(View.ROTATION, 0f, 90f, 0f)
            ).apply {
                duration = 2_200L; startDelay = 900L + i * 350L
                repeatCount = ValueAnimator.INFINITE; start()
            }
        }
    }

    private fun stopAll() {
        running.forEach { it.cancel() }
        running.clear()
        stopSound()
        binding.confetti.stop()
        binding.ivRays.setLayerType(View.LAYER_TYPE_NONE, null)
        binding.ivRays.rotation = 0f
        binding.flBadge.translationY = 0f
        listOf(binding.viewRing1, binding.viewRing2).forEach { ring ->
            ring.setLayerType(View.LAYER_TYPE_NONE, null)
            ring.alpha = 0f
            ring.scaleX = 1f
            ring.scaleY = 1f
        }
        showEndState()
    }

    private fun sparkles() = listOf(
        binding.ivSparkle0, binding.ivSparkle1, binding.ivSparkle2,
        binding.ivSparkle3, binding.ivSparkle4, binding.ivSparkle5
    )

    private fun segments() = listOf(binding.viewSeg0, binding.viewSeg1, binding.viewSeg2)

    private fun medals() = listOf(binding.llMedalBronze, binding.llMedalSilver, binding.llMedalGold)

    private fun color(@ColorRes res: Int): Int = ContextCompat.getColor(mActivity, res)

    private fun resolve(p: Palette) = Colors(color(p.lightRes), color(p.midRes), color(p.darkRes), color(p.glowRes))

    private fun rupees(value: Double): String = Utility.formatAmount(value.toString(), trimZeros = true)

    private fun withAlpha(color: Int, a: Float): Int =
        Color.argb((a * 255).toInt(), Color.red(color), Color.green(color), Color.blue(color))
}
