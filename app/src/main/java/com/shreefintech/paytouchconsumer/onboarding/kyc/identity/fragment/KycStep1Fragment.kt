package com.shreefintech.paytouchconsumer.onboarding.kyc.identity.fragment

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.FragmentKycStep1Binding
import com.shreefintech.paytouchconsumer.onboarding.kyc.identity.IdentityVerificationViewModel
import com.shreefintech.paytouchconsumer.utill.ToastUtil

class KycStep1Fragment : BaseKycStepFragment() {

    private var _binding: FragmentKycStep1Binding? = null
    private val binding get() = _binding!!
    private val viewModel: IdentityVerificationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.etMobile.setText(viewModel.mobile)
        binding.etEmail.setText(viewModel.email)
    }

    override fun validate(): Boolean {
        val mobile = binding.etMobile.text?.toString()?.trim() ?: ""
        val email  = binding.etEmail.text?.toString()?.trim()  ?: ""

        val msg = when {
            mobile.isEmpty()    -> { binding.etMobile.requestFocus(); getString(R.string.msgMobileEmpty) }
            mobile.length != 10 -> { binding.etMobile.requestFocus(); getString(R.string.msgMobileInvalid) }
            email.isEmpty()     -> { binding.etEmail.requestFocus(); getString(R.string.msgEmailEmpty) }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.etEmail.requestFocus(); getString(R.string.msgEmailInvalid)
            }
            else -> null
        }
        if (msg != null) { ToastUtil.showDelete(requireActivity(), msg); return false }

        viewModel.mobile = mobile
        viewModel.email  = email
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
