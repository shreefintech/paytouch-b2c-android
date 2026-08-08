package com.shreefintech.paytouchconsumer.onboarding.kyc.identity.fragment

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
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
import com.shreefintech.paytouchconsumer.databinding.FragmentKycStep4Binding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.onboarding.kyc.identity.IdentityVerificationActivity
import com.shreefintech.paytouchconsumer.onboarding.kyc.identity.IdentityVerificationViewModel
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class KycStep4Fragment : BaseKycStepFragment() {

    private var _binding: FragmentKycStep4Binding? = null
    private val binding get() = _binding!!
    private val viewModel: IdentityVerificationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycStep4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        attachEditDeleteGlass(binding.flEdit1)
        attachEditDeleteGlass(binding.flDelete1)
        viewModel.selfieUri?.let { showPreview(it) }

        binding.btnCapture.setOnClickListener { capture() }
        binding.ivEditProof1.setOnClickListener { capture() }
        binding.ivDeleteProof1.setOnClickListener { clearSelfie() }
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

    private fun capture() {
        if (Utility.stopClick()) return
        (requireActivity() as IdentityVerificationActivity).captureSelfie { uri ->
            viewModel.selfieUri = uri
            showPreview(uri)
        }
    }

    private fun clearSelfie() {
        if (Utility.stopClick()) return
        viewModel.selfieUri = null
        val ctx = context ?: return
        Glide.with(ctx).clear(binding.ivSelfiePreview)
        binding.mcvSelfiePreview.visibility = View.GONE
        binding.llSelfieActions.visibility  = View.GONE
        binding.ivSelfieGuide.visibility    = View.VISIBLE
    }

    private fun showPreview(uri: Uri) {
        val ctx = context ?: return
        binding.ivSelfieGuide.visibility = View.GONE

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
            .into(binding.ivSelfiePreview)

        binding.mcvSelfiePreview.visibility = View.VISIBLE
        binding.llSelfieActions.visibility  = View.VISIBLE
    }

    override fun validate(): Boolean {
        if (viewModel.selfieUri == null) {
            ToastUtil.showDelete(requireActivity(), getString(R.string.msgSelfieRequired))
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
