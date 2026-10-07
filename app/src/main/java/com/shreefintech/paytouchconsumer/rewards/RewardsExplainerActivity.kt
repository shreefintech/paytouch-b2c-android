package com.shreefintech.paytouchconsumer.rewards

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityRewardsExplainerBinding
import com.shreefintech.paytouchconsumer.enums.RewardsTier
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.RewardsLevelItem
import com.shreefintech.paytouchconsumer.utill.Utility
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RewardsExplainerActivity : BaseActivity() {

    private lateinit var binding: ActivityRewardsExplainerBinding
    private val viewModel: RewardsViewModel by viewModels()
    private var selectedTier = RewardsTier.BRONZE

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
        binding.onClickListener = onClickListener()

        Glide.with(this).load(R.drawable.img_hero_background_with_levels).into(binding.ivHeroBg)
        setupHeroHeadline()
        setupFaq()
        onBack()
        loadLevel()
    }

    private fun loadLevel() {
        viewModel.fetchLevel(
            onSuccess = { data -> populateHero(data) },
            onError   = { /* non-critical — defaults already visible */ }
        )
    }

    private fun setupHeroHeadline() {
        val full = getString(R.string.titleRewardsHero)
        val highlight = "0.50% back"
        val start = full.indexOf(highlight)

        binding.tvHeroHeadline.text = if (start >= 0) {
            SpannableString(full).apply {
                setSpan(
                    ForegroundColorSpan("#FCCFA1".toColorInt()),
                    start, start + highlight.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        } else {
            full   // text changed and highlight not found: show plain, don't crash
        }
    }

    private fun populateHero(data: RewardsLevelItem) {
        val tier = RewardsTier.from(data.stage)

        binding.ivYourBadge.setImageResource(tier.badgeRes)
        binding.tvYourTierName.text = data.label ?: getString(tier.labelRes)
        binding.tvYourCashback.text = "%.2f%%".format(data.cashbackPercent ?: 0.0)

        val remaining = data.next?.remaining ?: 0.0
        binding.tvProgressNext.text = if (remaining > 0)
            getString(R.string.labelMoreToNextLevel, Utility.formatAmount(remaining.toString()))
        else
            getString(R.string.msgTopTier)

        val current  = data.next?.current ?: 0.0
        val target   = data.next?.target  ?: 1.0
        val progress = if (target > 0) (current / target).toFloat().coerceIn(0f, 1f) else 1f
        animateLevelProgress(progress)

        selectTierTab(tier)
        markYouBadge(data.label)
    }

    private fun animateLevelProgress(progress: Float) {
        binding.viewProgressFill.pivotX = 0f
        binding.viewProgressFill.animate()
            .scaleX(progress)
            .setDuration(1000)
            .setStartDelay(500)
            .start()
    }

    // ── All Levels tabs ─────────────────────────────────────────────────────

    private fun selectTierTab(tier: RewardsTier) {
        selectedTier = tier
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

    private fun setupFaq() {
        val rows      = listOf(binding.rowFaq1, binding.rowFaq2, binding.rowFaq3, binding.rowFaq4)
        val questions = listOf(binding.tvFaq1Question, binding.tvFaq2Question, binding.tvFaq3Question, binding.tvFaq4Question)
        val answers   = listOf(binding.tvFaq1Answer,   binding.tvFaq2Answer,   binding.tvFaq3Answer,   binding.tvFaq4Answer)
        val arrows    = listOf(binding.ivFaq1Arrow,    binding.ivFaq2Arrow,    binding.ivFaq3Arrow,    binding.ivFaq4Arrow)

        val primaryColor = ContextCompat.getColor(mActivity, R.color.primary)
        val defaultColor = ContextCompat.getColor(mActivity, R.color.black)

        rows.forEachIndexed { i, row ->
            row.setOnClickListener {
                val open = answers[i].visibility == View.VISIBLE
                answers[i].visibility = if (open) View.GONE else View.VISIBLE
                arrows[i].rotation    = if (open) 0f else 180f
                questions[i].setTextColor(if (open) defaultColor else primaryColor)
            }
        }
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
                binding.cardYourLevel    -> {
                    if (Utility.stopClick()) return@OnClickListener
                    startActivity(Intent(mActivity, MyRankActivity::class.java))
                }
            }
        }
    }
}
