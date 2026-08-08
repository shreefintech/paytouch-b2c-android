package com.shreefintech.paytouchconsumer.onboarding.kyc.identity.fragment

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.FragmentKycStep3Binding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.onboarding.kyc.identity.IdentityVerificationActivity
import com.shreefintech.paytouchconsumer.onboarding.kyc.identity.IdentityVerificationViewModel
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class KycStep3Fragment : BaseKycStepFragment() {

    companion object {
        private val PAN_REGEX = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")
    }

    private var _binding: FragmentKycStep3Binding? = null
    private val binding get() = _binding!!
    private val viewModel: IdentityVerificationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycStep3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flUpload1.attach(binding.root as ViewGroup)
        attachEditDeleteGlass(binding.flEdit1)
        attachEditDeleteGlass(binding.flDelete1)

        val upperCaseFilter = InputFilter { source, start, end, _, _, _ ->
            source.subSequence(start, end).toString().uppercase()
        }
        binding.etPan.filters = arrayOf(InputFilter.LengthFilter(10), upperCaseFilter, Utility.EmojiExcludeFilter())
        binding.etPan.setText(viewModel.panNumber)

        viewModel.panFrontUri?.let { showPreview(it) }

        binding.flUpload1.setOnClickListener { pickFront() }
        binding.ivEditProof1.setOnClickListener { pickFront() }
        binding.ivDeleteProof1.setOnClickListener { clearFront() }
    }

    private fun attachEditDeleteGlass(targetView: View) {
        LiquidGlassEffect.attach(
            targetView   = targetView,
            rootView     = binding.root as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.filter_btn_radius),
            distortion   = 0f,
            blur         = resources.getDimensionPixelSize(R.dimen.filter_btn_blure),
            tintColor    = ContextCompat.getColor(requireContext(), R.color.filter_bg)
        )
    }

    private fun pickFront() {
        if (Utility.stopClick()) return
        (requireActivity() as IdentityVerificationActivity).pickDocument { uri ->
            viewModel.panFrontUri = uri
            showPreview(uri)
        }
    }

    private fun clearFront() {
        if (Utility.stopClick()) return
        viewModel.panFrontUri = null
        val ctx = context ?: return
        Glide.with(ctx).clear(binding.ivPreviewPan)
        binding.ivPreviewPan.visibility    = View.GONE
        binding.llEditDeletePan.visibility = View.GONE
        binding.llUploadPan.visibility     = View.VISIBLE
    }

    private fun showPreview(uri: Uri) {
        val ctx = context ?: return
        binding.llUploadPan.visibility = View.GONE

        Glide.with(ctx)
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
            .into(binding.ivPreviewPan)

        binding.ivPreviewPan.visibility    = View.VISIBLE
        binding.llEditDeletePan.visibility = View.VISIBLE
    }

    override fun validate(): Boolean {
        val pan = binding.etPan.text?.toString()?.trim() ?: ""
        val msg = when {
            pan.isEmpty()                  -> { binding.etPan.requestFocus(); getString(R.string.msgPanEmpty) }
            !PAN_REGEX.matches(pan)        -> { binding.etPan.requestFocus(); getString(R.string.msgPanInvalid) }
            viewModel.panFrontUri == null  -> getString(R.string.msgPanFrontRequired)
            else -> null
        }
        if (msg != null) { ToastUtil.showDelete(requireActivity(), msg); return false }

        viewModel.panNumber = pan
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
