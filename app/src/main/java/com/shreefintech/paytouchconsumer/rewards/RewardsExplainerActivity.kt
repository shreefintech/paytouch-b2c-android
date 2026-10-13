package com.shreefintech.paytouchconsumer.rewards

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityRewardsExplainerBinding
import com.shreefintech.paytouchconsumer.enums.RewardsTier
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.RewardsLevelItem
import com.shreefintech.paytouchconsumer.rewards.viewmodel.RewardsViewModel
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RewardsExplainerActivity : BaseActivity() {

    private lateinit var binding: ActivityRewardsExplainerBinding
    private val viewModel: RewardsViewModel by viewModels()
    private val loopAnimators = mutableListOf<Animator>()
    private var progressAnimator: ValueAnimator? = null

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, RewardsExplainerActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsExplainerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        val listener = onClickListener()
        binding.onClickListener = listener

        setupHeroHeadline()
        onBack()
        retryCallback = { loadLevel() }
        loadLevel()
    }

    override fun onResume() {
        super.onResume()
        startHeroLoops()
    }

    override fun onPause() {
        stopHeroLoops()
        super.onPause()
    }

    override fun onDestroy() {
        progressAnimator?.cancel()
        super.onDestroy()
    }

    private fun loadLevel() {
        if (!Utility.isInternetAvailable(mActivity)) {
            showNoInternet()
            return
        }
        hideNoInternet()
        viewModel.fetchLevel(
            onSuccess = { data -> populateHero(data) },
            onError   = { msg -> ToastUtil.showDelete(mActivity, msg) }
        )
    }

    private fun setupHeroHeadline() {
        val full      = getString(R.string.titleRewardsHero)
        val highlight = getString(R.string.labelRewardsHeroHighlight)
        val start     = full.indexOf(highlight)

        binding.tvHeroHeadline.text = if (start >= 0) {
            SpannableString(full).apply {
                setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(mActivity, R.color.secondary)),
                    start, start + highlight.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        } else {
            full   // text changed and highlight not found: show plain, don't crash
        }
    }

    private fun populateHero(data: RewardsLevelItem) {
        // Late response after onDestroy would restart the progress animator on a dead binding
        if (isFinishing || isDestroyed) return
        val tier = RewardsTier.from(data.stage)

        binding.ivYourBadge.setImageResource(tier.badgeRes)
        binding.tvYourTierName.text = data.label ?: getString(tier.labelRes)
        binding.tvYourCashback.text = getString(R.string.fmtPercent, data.cashbackPercent ?: 0.0)

        val next = data.next
        if (next == null) {
            binding.tvProgressNext.text = getString(R.string.msgHighestLevelPermanent)
            animateLevelProgress(1f)
        } else {
            val current = next.current ?: 0.0
            val target  = next.target  ?: 0.0
            val metric  = next.metric?.lowercase() ?: ""

            val amountPart = getString(
                R.string.labelProgressAmountFormat,
                progressValueText(current, metric),
                progressValueWithUnitText(target, metric)
            )
            binding.tvProgressNext.text = next.label?.let {
                getString(R.string.labelProgressToNextFormat, amountPart, it)
            } ?: amountPart

            val progress = if (target > 0) (current / target).toFloat().coerceIn(0f, 1f) else 1f
            animateLevelProgress(progress)
        }

        selectTierTab(tier)
        markYouBadge(data.label)
    }

    private fun progressValueText(value: Double, metric: String): String =
        if (metric == "referrals" || metric.endsWith("days")) value.toLong().toString()
        else Utility.formatAmount(value.toString(), trimZeros = true)

    private fun progressValueWithUnitText(value: Double, metric: String): String {
        val n = value.toLong().toInt()
        return when {
            metric == "referrals" -> resources.getQuantityString(R.plurals.fmtReferralsCount, n, n)
            metric.endsWith("days") -> resources.getQuantityString(R.plurals.fmtDaysCount, n, n)
            else -> Utility.formatAmount(value.toString(), trimZeros = true)
        }
    }

    /** Bar grows 0 → value: 1.2 s, after 0.6 s, CSS ease-out. Static when animations are off. */
    private fun animateLevelProgress(progress: Float) {
        progressAnimator?.cancel()
        val value = (progress * 100).toInt()
        if (!animationsEnabled()) {
            binding.viewProgressFill.progress = value
            return
        }
        progressAnimator = ValueAnimator.ofInt(0, value).apply {
            duration = 1_200L
            startDelay = 600L
            interpolator = PathInterpolator(0f, 0f, 0.58f, 1f)
            addUpdateListener { anim -> binding.viewProgressFill.progress = anim.animatedValue as Int }
            start()
        }
    }

    // ── Hero motion ─────────────────────────────────────────────────────────

    private fun heroBadges() = listOf(
        binding.llHeroBadgeBronze, binding.llHeroBadgeSilver,
        binding.llHeroBadgeGold, binding.llHeroBadgePlatinum
    )

    /** Rays turn once every 50 s; shields float 9dp on a 3.2 s cycle, staggered 0.3 s. */
    private fun startHeroLoops() {
        stopHeroLoops()
        if (!animationsEnabled()) return

        loopAnimators += ObjectAnimator.ofFloat(binding.ivHeroRays, View.ROTATION, 0f, 360f).apply {
            duration = 50_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
        val lift = -9f * resources.displayMetrics.density
        val easeInOut = PathInterpolator(0.42f, 0f, 0.58f, 1f)
        heroBadges().forEachIndexed { i, badge ->
            loopAnimators += ObjectAnimator.ofFloat(badge, View.TRANSLATION_Y, 0f, lift).apply {
                duration = 1_600L
                startDelay = i * 300L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = easeInOut
                start()
            }
        }
    }

    private fun stopHeroLoops() {
        loopAnimators.forEach { it.cancel() }
        loopAnimators.clear()
        binding.ivHeroRays.rotation = 0f
        heroBadges().forEach { it.translationY = 0f }
    }

    private fun animationsEnabled(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ValueAnimator.areAnimatorsEnabled()
        else Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

    // ── All Levels tabs ─────────────────────────────────────────────────────

    private fun selectTierTab(tier: RewardsTier) {
        val tabs = mapOf(
            RewardsTier.BRONZE   to Pair(binding.tabBronze,   binding.panelBronze),
            RewardsTier.SILVER   to Pair(binding.tabSilver,   binding.panelSilver),
            RewardsTier.GOLD     to Pair(binding.tabGold,     binding.panelGold),
            RewardsTier.PLATINUM to Pair(binding.tabPlatinum, binding.panelPlatinum)
        )
        val selectedBg   = ContextCompat.getDrawable(mActivity, R.drawable.bg_toggle_selected)
        val unselectedBg = ContextCompat.getDrawable(mActivity, R.drawable.bg_toggle_unselected)
        val white   = ContextCompat.getColor(mActivity, R.color.white)
        val primary = ContextCompat.getColor(mActivity, R.color.primary)

        tabs.forEach { (t, pair) ->
            val (tab, panel) = pair
            val isSelected = t == tier
            tab.background = if (isSelected) selectedBg else unselectedBg
            tab.setTextColor(if (isSelected) white else primary)
            panel.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }

    private fun markYouBadge(label: String?) {
        val safeLabel = label ?: return
        val chips = mapOf(
            "Bronze III" to binding.chipBronzeIII,
            "Bronze II"  to binding.chipBronzeII,
            "Bronze I"   to binding.chipBronzeI,
            "Silver III" to binding.chipSilverIII,
            "Silver II"  to binding.chipSilverII,
            "Silver I"   to binding.chipSilverI,
            "Gold III"   to binding.chipGoldIII,
            "Gold II"    to binding.chipGoldII,
            "Gold I"     to binding.chipGoldI,
            "Platinum"   to binding.chipPlatinum
        )
        chips.forEach { (lvl, chip) -> chip.visibility = if (lvl == safeLabel) View.VISIBLE else View.GONE }
    }

    // ── FAQ accordion ───────────────────────────────────────────────────────

    private fun toggleFaq(index: Int) {
        val answers   = listOf(binding.tvFaq1Answer,   binding.tvFaq2Answer,   binding.tvFaq3Answer,   binding.tvFaq4Answer)
        val arrows    = listOf(binding.ivFaq1Arrow,    binding.ivFaq2Arrow,    binding.ivFaq3Arrow,    binding.ivFaq4Arrow)
        val questions = listOf(binding.tvFaq1Question, binding.tvFaq2Question, binding.tvFaq3Question, binding.tvFaq4Question)
        val primaryColor = ContextCompat.getColor(mActivity, R.color.primary)
        val defaultColor = ContextCompat.getColor(mActivity, R.color.black)

        val open = answers[index].visibility == View.VISIBLE
        answers[index].visibility = if (open) View.GONE else View.VISIBLE
        arrows[index].rotation    = if (open) 0f else 180f
        questions[index].setTextColor(if (open) defaultColor else primaryColor)
    }

    // ── Navigation ──────────────────────────────────────────────────────────

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.lytToolbar.ivBack -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onBackPressedDispatcher.onBackPressed()
                }
                binding.tabBronze   -> { if (Utility.stopClick()) return@OnClickListener; selectTierTab(RewardsTier.BRONZE) }
                binding.tabSilver   -> { if (Utility.stopClick()) return@OnClickListener; selectTierTab(RewardsTier.SILVER) }
                binding.tabGold     -> { if (Utility.stopClick()) return@OnClickListener; selectTierTab(RewardsTier.GOLD) }
                binding.tabPlatinum -> { if (Utility.stopClick()) return@OnClickListener; selectTierTab(RewardsTier.PLATINUM) }
                binding.rowFaq1     -> { if (Utility.stopClick()) return@OnClickListener; toggleFaq(0) }
                binding.rowFaq2     -> { if (Utility.stopClick()) return@OnClickListener; toggleFaq(1) }
                binding.rowFaq3     -> { if (Utility.stopClick()) return@OnClickListener; toggleFaq(2) }
                binding.rowFaq4     -> { if (Utility.stopClick()) return@OnClickListener; toggleFaq(3) }
                binding.tvMyRank    -> {
                    if (Utility.stopClick()) return@OnClickListener
                    startActivity(Intent(mActivity, MyRankActivity::class.java))
                }
                binding.cardYourLevel    -> {
                    if (Utility.stopClick()) return@OnClickListener
                    startActivity(Intent(mActivity, MyRankActivity::class.java))
                }
            }
        }
    }
}
