package com.shreefintech.paytouchconsumer.rewards

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityMyRankBinding
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.RewardsLevelItem
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.RewardsLevelNextItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import java.util.Locale

class MyRankActivity : BaseActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMyRankBinding
    private val viewModel: RewardsViewModel by viewModels()
    private val loopAnimators = mutableListOf<Animator>()
    private var levelData: RewardsLevelItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyRankBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.onClickListener = this
        retryCallback = { loadLevel() }
        loadLevel()
    }

    override fun onResume() {
        super.onResume()
        if (levelData != null) startLoops()
    }

    override fun onPause() {
        stopLoops()
        super.onPause()
    }

    override fun onClick(v: View) {
        if (Utility.stopClick()) return
        when (v.id) {
            R.id.ivBack   -> onBackPressedDispatcher.onBackPressed()
            R.id.btnReplay -> openCelebration()
        }
    }

    // ───────────────────────── API ─────────────────────────

    private fun loadLevel() {
        if (!Utility.isInternetAvailable(mActivity)) {
            showNoInternet()
            return
        }
        hideNoInternet()
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.shimmerLayout.startShimmer()
        binding.llContent.visibility = View.INVISIBLE

        viewModel.fetchLevel(
            onSuccess = { data ->
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
                levelData = data
                bind(data)
                binding.llContent.visibility = View.VISIBLE
                startLoops()
                playEntrance()
            },
            onError = { msg ->
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    // ───────────────────────── BIND ─────────────────────────

    private fun bind(d: RewardsLevelItem) {
        val tier = RankTier.from(d.stage)
        val tint = ContextCompat.getColor(this, tier.tint)

        Glide.with(this).load(tier.shield).into(binding.ivShield)
        binding.ivRays.imageTintList = ColorStateList.valueOf(tint)
        binding.viewGlow.backgroundTintList = ColorStateList.valueOf(tint)
        binding.tvRankName.text = d.label.orEmpty()
        binding.tvCashPill.text = cashPillText(d.cashbackPercent ?: 0.0)

        val next = d.next
        if (next == null) {
            binding.cardRoad.isVisible = false
            binding.llPlatinum.isVisible = true
            return
        }
        binding.cardRoad.isVisible = true
        binding.llPlatinum.isVisible = false

        binding.tvRoadTitle.text = getString(R.string.rank_road_to, next.label.orEmpty())
        val nextCash = RankFormat.cashbackFor(next.level)
        binding.tvNextCashChip.isVisible = nextCash != null
        nextCash?.let { binding.tvNextCashChip.text = getString(R.string.rank_percent, RankFormat.percent(it)) }

        if (next.metric == "gold_1_days") {
            bindCountdown(next, d.platinumDays)
        } else {
            bindBar(next)
        }
    }

    private fun cashPillText(percent: Double): CharSequence {
        val value = "${RankFormat.percent(percent)}%"
        val full = value + getString(R.string.rank_cash_suffix)
        return SpannableString(full).apply {
            setSpan(ForegroundColorSpan(ContextCompat.getColor(mActivity, R.color.secondary)), 0, value.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(AbsoluteSizeSpan(16, true), 0, value.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), 0, value.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun bindBar(next: RewardsLevelNextItem) {
        binding.llBar.isVisible = true
        binding.llCountdown.isVisible = false

        val cur = next.current ?: 0.0
        val tgt = next.target ?: 0.0
        binding.tvProgress.text = when (next.metric) {
            "txn_amount"    -> getString(R.string.rank_progress_txn, RankFormat.rupees(cur), RankFormat.rupees(tgt))
            "locked_amount" -> getString(R.string.rank_progress_locked, RankFormat.rupees(cur), RankFormat.rupees(tgt))
            else            -> getString(R.string.rank_progress_referrals, RankFormat.count(cur), RankFormat.count(tgt))
        }
        binding.tvRule.text = getString(
            when (next.metric) {
                "txn_amount"    -> R.string.rank_rule_txn
                "locked_amount" -> R.string.rank_rule_locked
                else            -> R.string.rank_rule_referrals
            }
        )
        val ratio = if (tgt > 0) (cur / tgt).toFloat().coerceIn(0f, 1f) else 0f
        binding.tvProgressPct.text = getString(R.string.rank_percent, (ratio * 100).toInt().toString())
        binding.viewBarFill.tag = ratio
    }

    private fun bindCountdown(next: RewardsLevelNextItem, platinumDays: Int?) {
        binding.llBar.isVisible = false
        binding.llCountdown.isVisible = true

        val target  = if ((next.target ?: 0.0) > 0) next.target!!.toInt() else 90
        val current = if ((next.current ?: 0.0) > 0) next.current!!.toInt() else (platinumDays ?: 0)
        val remaining = (target - current).coerceAtLeast(0)

        binding.tvRingValue.text = current.toString()
        binding.tvRingTotal.text = getString(R.string.rank_days_of, target)
        binding.tvDaysToGo.text  = getString(R.string.rank_days_to_go, remaining)
    }

    // ───────────────────────── MOTION ─────────────────────────

    private fun animationsEnabled(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ValueAnimator.areAnimatorsEnabled()
        else Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

    private fun playEntrance() {
        val anim = animationsEnabled()
        (binding.viewBarFill.tag as? Float)?.let { ratio ->
            if (anim) {
                binding.viewBarFill.scaleX = 0f
                binding.viewBarFill.animate().scaleX(ratio).setStartDelay(300).setDuration(1000)
                    .setInterpolator(DecelerateInterpolator()).start()
            } else binding.viewBarFill.scaleX = ratio
        }
    }

    private fun startLoops() {
        stopLoops()
        if (!animationsEnabled()) return

        binding.ivRays.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        loopAnimators += ObjectAnimator.ofFloat(binding.ivRays, View.ROTATION, 0f, 360f).apply {
            duration = 40_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
        val lift = -9f * resources.displayMetrics.density
        loopAnimators += ObjectAnimator.ofFloat(binding.flBadge, View.TRANSLATION_Y, 0f, lift).apply {
            duration = 2_000L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopLoops() {
        loopAnimators.forEach { it.cancel() }
        loopAnimators.clear()
        binding.ivRays.setLayerType(View.LAYER_TYPE_NONE, null)
        binding.ivRays.rotation = 0f
        binding.flBadge.translationY = 0f
    }

    // ───────────────────────── NAV ─────────────────────────

    private fun openCelebration() {
        val d = levelData ?: return
        // TODO(PAYTOUCH-207): navigate to level-up celebration Activity when implemented
    }
}
