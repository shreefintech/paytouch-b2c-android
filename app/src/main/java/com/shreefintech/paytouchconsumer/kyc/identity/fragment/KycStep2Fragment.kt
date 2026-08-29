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
import com.shreefintech.paytouchconsumer.databinding.FragmentKycStep2Binding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.kyc.identity.IdentityVerificationActivity
import com.shreefintech.paytouchconsumer.kyc.identity.IdentityVerificationViewModel
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        binding.etAadhar.filters = arrayOf(
            InputFilter.LengthFilter(12),
            Utility.digitFilter(),
            Utility.EmojiExcludeFilter()
        )
        binding.etAadhar.setText(viewModel.aadhaarNumber)

        viewModel.aadhaarFrontUri?.let { showPreview(it, isFront = true) }
        viewModel.aadhaarBackUri?.let { showPreview(it, isFront = false) }

        binding.flUpload1.setOnClickListener {
            Utility.hideKeyboard(requireActivity())
            pickFront()
        }
        binding.flEdit1.setOnClickListener {
            Utility.hideKeyboard(requireActivity())
            pickFront()
        }
        binding.ivDeleteProof1.setOnClickListener {
            Utility.hideKeyboard(requireActivity())
            clearSlot(isFront = true)
        }

        binding.flUpload2.setOnClickListener {
            Utility.hideKeyboard(requireActivity())
            pickBack()
        }
        binding.flEdit2.setOnClickListener {
            Utility.hideKeyboard(requireActivity())
            pickBack()
        }
        binding.ivDeleteProof2.setOnClickListener {
            Utility.hideKeyboard(requireActivity())
            clearSlot(isFront = false)
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

    private fun hostActivity() = requireActivity() as IdentityVerificationActivity

    private fun pickFront() {
        if (Utility.stopClick()) return
        hostActivity().pickDocument { uri ->
            viewModel.setAadhaarFrontUri(uri)
            showPreview(uri, isFront = true)
        }
    }

    private fun pickBack() {
        if (Utility.stopClick()) return
        hostActivity().pickDocument { uri ->
            viewModel.setAadhaarBackUri(uri)
            showPreview(uri, isFront = false)
        }
    }

    private fun clearSlot(isFront: Boolean) {
        if (Utility.stopClick()) return
        if (isFront) viewModel.setAadhaarFrontUri(null) else viewModel.setAadhaarBackUri(null)

        val ctx = context ?: return
        val uploadPrompt = if (isFront) binding.llUploadFront else binding.llUploadBack
        val preview = if (isFront) binding.ivPreviewFront else binding.ivPreviewBack
        val editDeleteRow = if (isFront) binding.llEditDeleteFront else binding.llEditDeleteBack

        Glide.with(ctx).clear(preview)
        preview.visibility = View.GONE
        editDeleteRow.visibility = View.GONE
        uploadPrompt.visibility = View.VISIBLE
    }

    private fun showPreview(uri: Uri, isFront: Boolean) {
        val ctx = context ?: return
        val uploadPrompt = if (isFront) binding.llUploadFront else binding.llUploadBack
        val preview = if (isFront) binding.ivPreviewFront else binding.ivPreviewBack
        val editDeleteRow = if (isFront) binding.llEditDeleteFront else binding.llEditDeleteBack

        uploadPrompt.visibility = View.GONE
        preview.visibility = View.VISIBLE
        editDeleteRow.visibility = View.VISIBLE

        if (ctx.contentResolver.getType(uri) == "application/pdf") {
            preview.setImageResource(R.drawable.ic_file_not_found)
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val bmp = Utility.renderPdfFirstPage(ctx, uri)
                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    if (bmp != null) Glide.with(ctx).load(bmp).into(preview)
                }
            }
        } else {
            Glide.with(ctx)
                .load(uri)
                .placeholder(R.drawable.ic_file_not_found)
                .error(R.drawable.ic_file_not_found)
                .into(preview)
        }
    }

    override fun validate(): Boolean {
        val aadhaar = binding.etAadhar.text?.toString()?.trim() ?: ""
        val msg = when {
            aadhaar.isEmpty() -> {
                binding.etAadhar.requestFocus(); getString(R.string.msgAadharEmpty)
            }

            aadhaar.length != 12 -> {
                binding.etAadhar.requestFocus(); getString(R.string.msgAadharInvalid)
            }

            viewModel.aadhaarFrontUri == null -> getString(R.string.msgAadharFrontRequired)
            viewModel.aadhaarBackUri == null -> getString(R.string.msgAadharBackRequired)
            else -> null
        }
        if (msg != null) {
            ToastUtil.showDelete(requireActivity(), msg); return false
        }

        viewModel.setAadhaarNumber(aadhaar)
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
