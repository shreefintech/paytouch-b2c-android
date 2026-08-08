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
import com.shreefintech.paytouchconsumer.databinding.FragmentKycStep2Binding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.onboarding.kyc.identity.IdentityVerificationActivity
import com.shreefintech.paytouchconsumer.onboarding.kyc.identity.IdentityVerificationViewModel
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class KycStep2Fragment : BaseKycStepFragment() {

    private var _binding: FragmentKycStep2Binding? = null
    private val binding get() = _binding!!
    private val viewModel: IdentityVerificationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flUpload1.attach(binding.root as ViewGroup)
        binding.flUpload2.attach(binding.root as ViewGroup)
        attachEditDeleteGlass(binding.flEdit1)
        attachEditDeleteGlass(binding.flDelete1)
        attachEditDeleteGlass(binding.flEdit2)
        attachEditDeleteGlass(binding.flDelete2)

        binding.etAadhar.filters = arrayOf(InputFilter.LengthFilter(12), Utility.digitFilter(), Utility.EmojiExcludeFilter())
        binding.etAadhar.setText(viewModel.aadhaarNumber)

        viewModel.aadhaarFrontUri?.let { showPreview(it, isFront = true) }
        viewModel.aadhaarBackUri?.let { showPreview(it, isFront = false) }

        binding.flUpload1.setOnClickListener { pickFront() }
        binding.ivUpload1.setOnClickListener { pickFront() }
        binding.ivDeleteProof1.setOnClickListener { clearSlot(isFront = true) }

        binding.flUpload1.setOnClickListener { pickBack() }
        binding.ivUpload2.setOnClickListener { pickBack() }
        binding.ivDeleteProof2.setOnClickListener { clearSlot(isFront = false) }
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
    private fun hostActivity() = requireActivity() as IdentityVerificationActivity

    private fun pickFront() {
        if (Utility.stopClick()) return
        hostActivity().pickDocument { uri ->
            viewModel.aadhaarFrontUri = uri
            showPreview(uri, isFront = true)
        }
    }

    private fun pickBack() {
        if (Utility.stopClick()) return
        hostActivity().pickDocument { uri ->
            viewModel.aadhaarBackUri = uri
            showPreview(uri, isFront = false)
        }
    }

    private fun clearSlot(isFront: Boolean) {
        if (Utility.stopClick()) return
        if (isFront) viewModel.aadhaarFrontUri = null else viewModel.aadhaarBackUri = null

        val ctx = context ?: return
        val uploadPrompt  = if (isFront) binding.llUploadFront else binding.llUploadBack
        val preview       = if (isFront) binding.ivPreviewFront else binding.ivPreviewBack
        val editDeleteRow = if (isFront) binding.llEditDeleteFront else binding.llEditDeleteBack

        Glide.with(ctx).clear(preview)
        preview.visibility       = View.GONE
        editDeleteRow.visibility = View.GONE
        uploadPrompt.visibility  = View.VISIBLE
    }

    private fun showPreview(uri: Uri, isFront: Boolean) {
        val ctx = context ?: return
        val uploadPrompt  = if (isFront) binding.llUploadFront else binding.llUploadBack
        val preview       = if (isFront) binding.ivPreviewFront else binding.ivPreviewBack
        val editDeleteRow = if (isFront) binding.llEditDeleteFront else binding.llEditDeleteBack

        uploadPrompt.visibility = View.GONE

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
            .into(preview)

        preview.visibility       = View.VISIBLE
        editDeleteRow.visibility = View.VISIBLE
    }

    override fun validate(): Boolean {
        val aadhaar = binding.etAadhar.text?.toString()?.trim() ?: ""
        val msg = when {
            aadhaar.isEmpty()                  -> { binding.etAadhar.requestFocus(); getString(R.string.msgAadharEmpty) }
            aadhaar.length != 12                -> { binding.etAadhar.requestFocus(); getString(R.string.msgAadharInvalid) }
            viewModel.aadhaarFrontUri == null  -> getString(R.string.msgAadharFrontRequired)
            viewModel.aadhaarBackUri == null   -> getString(R.string.msgAadharBackRequired)
            else -> null
        }
        if (msg != null) { ToastUtil.showDelete(requireActivity(), msg); return false }

        viewModel.aadhaarNumber = aadhaar
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
