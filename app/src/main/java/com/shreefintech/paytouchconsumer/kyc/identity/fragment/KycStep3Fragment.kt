package com.shreefintech.paytouchconsumer.kyc.identity.fragment

import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.FragmentKycStep3Binding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.kyc.identity.IdentityVerificationActivity
import com.shreefintech.paytouchconsumer.kyc.identity.IdentityVerificationViewModel
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        binding.etPan.filters =
            arrayOf(InputFilter.LengthFilter(10), upperCaseFilter, Utility.EmojiExcludeFilter())
        binding.etPan.setText(viewModel.panNumber)

        viewModel.panFrontUri?.let { showPreview(it) }

        binding.flUpload1.setOnClickListener {
            Utility.hideKeyboard(requireActivity())
            pickFront()
        }
        binding.ivEditProof1.setOnClickListener {
            Utility.hideKeyboard(requireActivity())
            pickFront()
        }
        binding.ivDeleteProof1.setOnClickListener {
            Utility.hideKeyboard(requireActivity())
            clearFront()
        }
    }

    private fun attachEditDeleteGlass(targetView: View) {
        LiquidGlassEffect.attach(
            targetView = targetView,
            rootView = binding.root as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.filter_btn_radius),
            distortion = 0f,
            blur = resources.getDimensionPixelSize(R.dimen.filter_btn_blure),
            tintColor = ContextCompat.getColor(requireContext(), R.color.filter_bg)
        )
    }

    private fun pickFront() {
        if (Utility.stopClick()) return
        (requireActivity() as IdentityVerificationActivity).pickDocument { uri ->
            viewModel.setPanFrontUri(uri)
            showPreview(uri)
        }
    }

    private fun clearFront() {
        if (Utility.stopClick()) return
        viewModel.setPanFrontUri(null)
        val ctx = context ?: return
        Glide.with(ctx).clear(binding.ivPreviewPan)
        binding.ivPreviewPan.visibility = View.GONE
        binding.llEditDeletePan.visibility = View.GONE
        binding.llUploadPan.visibility = View.VISIBLE
    }

    private fun showPreview(uri: Uri) {
        val ctx = context ?: return
        binding.llUploadPan.visibility = View.GONE
        binding.ivPreviewPan.visibility = View.VISIBLE
        binding.llEditDeletePan.visibility = View.VISIBLE

        if (ctx.contentResolver.getType(uri) == "application/pdf") {
            binding.ivPreviewPan.setImageResource(R.drawable.ic_file_not_found)
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val bmp = Utility.renderPdfFirstPage(ctx, uri)
                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    if (bmp != null) Glide.with(ctx).load(bmp).into(binding.ivPreviewPan)
                }
            }
        } else {
            Glide.with(ctx)
                .load(uri)
                .placeholder(R.drawable.ic_file_not_found)
                .error(R.drawable.ic_file_not_found)
                .into(binding.ivPreviewPan)
        }
    }

    override fun validate(): Boolean {
        val pan = binding.etPan.text?.toString()?.trim() ?: ""
        val msg = when {
            pan.isEmpty() -> {
                binding.etPan.requestFocus(); getString(R.string.msgPanEmpty)
            }

            !PAN_REGEX.matches(pan) -> {
                binding.etPan.requestFocus(); getString(R.string.msgPanInvalid)
            }

            viewModel.panFrontUri == null -> getString(R.string.msgPanFrontRequired)
            else -> null
        }
        if (msg != null) {
            ToastUtil.showDelete(requireActivity(), msg); return false
        }

        viewModel.setPanNumber(pan)
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
