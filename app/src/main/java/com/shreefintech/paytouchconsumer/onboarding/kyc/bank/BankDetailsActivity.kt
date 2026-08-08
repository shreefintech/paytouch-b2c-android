package com.shreefintech.paytouchconsumer.onboarding.kyc.bank

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityBankDetailsBinding
import com.shreefintech.paytouchconsumer.databinding.ItemBankAccountBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.FilePickerUtil
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.widget.CustomDropdown

class BankDetailsActivity : BaseActivity() {

    companion object {
        private const val MAX_ACCOUNTS = 4
        private val IFSC_REGEX = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")
    }

    private lateinit var binding: ActivityBankDetailsBinding
    private val viewModel: BankDetailsViewModel by viewModels()
    private var showProgressSubmit = ObservableBoolean(false)

    private lateinit var filePickerUtil: FilePickerUtil
    private var activeCardIndex = -1

    private val bankCardBindings = mutableListOf<ItemBankAccountBinding>()
    private val proofTypes = mutableListOf<String?>()
    private val proofUris = mutableListOf<Uri?>()

    private val proofTypeList = listOf("Cancelled Cheque", "Bank Statement")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBankDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets  = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(imeInsets.bottom, systemBars.bottom)
            )
            insets
        }

        LiquidGlassEffect.attach(
            targetView   = binding.flCard,
            rootView     = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion   = 0f,
            strokeWidth = 1,
            strokeColor = ContextCompat.getColor(mActivity, R.color.primary),
            solidStroke = true,
            blur         = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )

        binding.onClickListener    = onClickListener()
        binding.showProgressSubmit = showProgressSubmit

        onBack()
        setupFilePicker()
        addBankCard()
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    // ─── Dynamic bank cards ───────────────────────────────────────────────────

    private fun addBankCard() {
        if (bankCardBindings.size >= MAX_ACCOUNTS) return

        val card = ItemBankAccountBinding.inflate(layoutInflater, binding.llBankContainer, true)
        val index = bankCardBindings.size
        bankCardBindings.add(card)
        proofTypes.add(null)
        proofUris.add(null)

        card.flUpload1.attach(card.root as ViewGroup)
        attachEditDeleteGlass(card.flEdit1)
        attachEditDeleteGlass(card.flDelete1)
        card.tvCardTitle.text = getString(R.string.fmtBankAccountTitle, index + 1)
        setupCardFilters(card)

        card.ivDelete.setOnClickListener {
            if (Utility.stopClick()) return@setOnClickListener
            removeBankCard(card)
        }

        card.flProofTypeAnchor.setOnClickListener {
            if (Utility.stopClick()) return@setOnClickListener
            showProofTypeDropdown(card)
        }

        val openPicker = View.OnClickListener {
            if (Utility.stopClick()) return@OnClickListener
            activeCardIndex = bankCardBindings.indexOf(card)
            filePickerUtil.openPicker()
        }
        card.flUpload1.setOnClickListener(openPicker)
        card.ivEditProof1.setOnClickListener(openPicker)

        card.ivDeleteProof1.setOnClickListener {
            if (Utility.stopClick()) return@setOnClickListener
            clearProof(card, bankCardBindings.indexOf(card))
        }

        syncCardState()
    }

    private fun attachEditDeleteGlass(targetView: View) {
        LiquidGlassEffect.attach(
            targetView   = targetView,
            rootView     = binding.root as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.filter_btn_radius),
            distortion   = 0f,
            blur         = resources.getDimensionPixelSize(R.dimen.filter_btn_blure),
            tintColor    = ContextCompat.getColor(mActivity, R.color.filter_bg)
        )
    }

    private fun removeBankCard(card: ItemBankAccountBinding) {
        if (bankCardBindings.size <= 1) return
        val index = bankCardBindings.indexOf(card)
        if (index == -1) return

        binding.llBankContainer.removeView(card.root)
        bankCardBindings.removeAt(index)
        proofTypes.removeAt(index)
        proofUris.removeAt(index)

        bankCardBindings.forEachIndexed { i, b -> b.tvCardTitle.text = getString(R.string.fmtBankAccountTitle, i + 1) }
        syncCardState()
    }

    private fun syncCardState() {
        val count = bankCardBindings.size
        bankCardBindings.forEach { b -> b.ivDelete.visibility = if (count > 1) View.VISIBLE else View.GONE }
        (binding.llAddAccount.parent as? View)?.visibility = if (count >= MAX_ACCOUNTS) View.GONE else View.VISIBLE
    }

    private fun setupCardFilters(card: ItemBankAccountBinding) {
        val emojiFilter     = Utility.EmojiExcludeFilter()
        val upperCaseFilter = InputFilter { source, start, end, _, _, _ ->
            source.subSequence(start, end).toString().uppercase()
        }

        card.etAccountNumber.filters = arrayOf(InputFilter.LengthFilter(18), Utility.digitFilter(), emojiFilter)
        card.etBankName.filters      = arrayOf(InputFilter.LengthFilter(50), emojiFilter)
        card.etIfscCode.filters      = arrayOf(InputFilter.LengthFilter(11), upperCaseFilter, emojiFilter)
        card.etBranchName.filters    = arrayOf(InputFilter.LengthFilter(50), emojiFilter)
    }

    private fun showProofTypeDropdown(card: ItemBankAccountBinding) {
        Utility.hideKeyboard(binding.clRoot)
        CustomDropdown.showDropdown(
            activity   = mActivity,
            anchorView = card.flProofTypeAnchor,
            arrowView  = card.ivProofTypeArrow,
            textView   = card.tvProofType,
            items      = proofTypeList
        ) { selected, _ -> proofTypes[bankCardBindings.indexOf(card)] = selected }
    }

    // ─── Bank proof upload ────────────────────────────────────────────────────

    private fun setupFilePicker() {
        filePickerUtil = FilePickerUtil(this)
        filePickerUtil.onSuccess = { result -> applyProofToActiveCard(result.uri) }
        filePickerUtil.onError   = { error ->
            ToastUtil.showDelete(mActivity, filePickerUtil.getErrorMessage(error))
        }
    }

    private fun applyProofToActiveCard(uri: Uri) {
        val index = activeCardIndex
        if (index !in bankCardBindings.indices) return
        proofUris[index] = uri

        val card = bankCardBindings[index]
        card.llUploadProof.visibility = View.GONE

        Glide.with(mActivity)
            .load(uri)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean
                ): Boolean = false

                override fun onResourceReady(
                    resource: Drawable, model: Any, target: Target<Drawable>?,
                    dataSource: DataSource, isFirstResource: Boolean
                ): Boolean = false
            })
            .into(card.ivPreviewProof)

        card.ivPreviewProof.visibility     = View.VISIBLE
        card.llEditDeleteProof.visibility  = View.VISIBLE
    }

    private fun clearProof(card: ItemBankAccountBinding, index: Int) {
        if (index !in proofUris.indices) return
        proofUris[index] = null
        Glide.with(mActivity).clear(card.ivPreviewProof)
        card.ivPreviewProof.visibility    = View.GONE
        card.llEditDeleteProof.visibility = View.GONE
        card.llUploadProof.visibility     = View.VISIBLE
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    private fun validate(): Boolean {
        Utility.hideKeyboard(binding.clRoot)

        bankCardBindings.forEachIndexed { index, card ->
            val msg = validateCard(card, index) ?: return@forEachIndexed
            ToastUtil.showDelete(mActivity, msg)
            return false
        }

        if (!binding.cbTermsConditions.isChecked) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgAgreeTAndCRequired))
            return false
        }
        return true
    }

    private fun validateCard(card: ItemBankAccountBinding, index: Int): String? {
        val accountNumber = card.etAccountNumber.text?.toString()?.trim() ?: ""
        val bankName       = card.etBankName.text?.toString()?.trim()     ?: ""
        val ifsc           = card.etIfscCode.text?.toString()?.trim()     ?: ""
        val branchName     = card.etBranchName.text?.toString()?.trim()   ?: ""

        return when {
            accountNumber.isEmpty()           -> { card.etAccountNumber.requestFocus(); getString(R.string.msgAccountNumberEmpty) }
            accountNumber.length !in 9..18     -> { card.etAccountNumber.requestFocus(); getString(R.string.msgAccountNumberInvalid) }
            bankName.isEmpty()                -> { card.etBankName.requestFocus(); getString(R.string.msgBankNameEmpty) }
            ifsc.isEmpty()                     -> { card.etIfscCode.requestFocus(); getString(R.string.msgIfscEmpty) }
            !IFSC_REGEX.matches(ifsc)          -> { card.etIfscCode.requestFocus(); getString(R.string.msgIfscInvalid) }
            branchName.isEmpty()               -> { card.etBranchName.requestFocus(); getString(R.string.msgBranchNameEmpty) }
            proofTypes[index].isNullOrEmpty()  -> getString(R.string.msgProofTypeEmpty, index + 1)
            proofUris[index] == null           -> getString(R.string.msgBankProofRequired, index + 1)
            else -> null
        }
    }

    private fun onSubmit() {
        if (!validate()) return

        val accounts = bankCardBindings.mapIndexed { index, card ->
            BankAccountInput(
                accountNumber = card.etAccountNumber.text?.toString()?.trim() ?: "",
                bankName      = card.etBankName.text?.toString()?.trim()      ?: "",
                ifscCode      = card.etIfscCode.text?.toString()?.trim()      ?: "",
                branchName    = card.etBranchName.text?.toString()?.trim()   ?: "",
                proofType     = proofTypes[index] ?: "",
                proofUri      = proofUris[index] ?: return
            )
        }

        viewModel.submit(
            accounts  = accounts,
            onLoading = { showProgressSubmit.set(true) },
            onSuccess = {
                showProgressSubmit.set(false)
                ToastUtil.showSuccess(mActivity, getString(R.string.msgBankDetailsSubmitSuccess))
                setResult(1)
                finish()
            },
            onError   = { msg -> showProgressSubmit.set(false); ToastUtil.showDelete(mActivity, msg) }
        )
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.lytToolbar.ivBack -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onBackPressedDispatcher.onBackPressed()
                }

                binding.llAddAccount -> {
                    if (Utility.stopClick()) return@OnClickListener
                    addBankCard()
                }

                binding.llSubmit -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressSubmit.get()) return@OnClickListener
                    onSubmit()
                }
            }
        }
    }
}
