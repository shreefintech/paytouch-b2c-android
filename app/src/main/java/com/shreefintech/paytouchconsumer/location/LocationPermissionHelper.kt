package com.shreefintech.paytouchconsumer.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.DialogLocationPermissionBinding
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.visible

/**
 * Activity-scoped flow: disclosure dialog → runtime permission → "turn on location" → one current fix.
 *
 * Must be created as an Activity field (activity-result launchers must be registered before STARTED).
 * [onResult] is invoked exactly once per [start] — with the location, or null when the user declined,
 * location is off, the fix failed, or the location is mocked. Foreground only; never background.
 */
class LocationPermissionHelper(
    private val activity: ComponentActivity,
    private val onResult: (Location?) -> Unit
) : DefaultLifecycleObserver {

    private enum class DialogType { AWARENESS, RATIONALE, SETTINGS }

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { onPermissionResult() }

    private val resolutionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) fetchLocation() else onLocationOff()
    }

    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(activity) }

    private var cancellationSource: CancellationTokenSource? = null
    private var dialog: Dialog? = null
    private var dialogBinding: DialogLocationPermissionBinding? = null
    private var dialogType = DialogType.AWARENESS
    private var isRunning = false
    private var awaitingSettings = false

    init {
        activity.lifecycle.addObserver(this)
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        when {
            hasPermission() -> checkLocationSettings()
            shouldShowRationale() -> showDialog(DialogType.RATIONALE)
            !SharedPreferenceHelper.getSharedPreferenceBoolean(
                activity, Constant.KEY_LOCATION_ASKED, false
            ) -> showDialog(DialogType.AWARENESS)
            // Asked before and Android no longer shows its popup → "Don't ask again"
            else -> showDialog(DialogType.SETTINGS)
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onResume(owner: LifecycleOwner) {
        // Returning from the app-settings screen — Home is not recreated, so re-check here
        if (!awaitingSettings) return
        awaitingSettings = false
        if (hasPermission()) checkLocationSettings() else finish(null)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        cancellationSource?.cancel()
        cancellationSource = null
        dialog?.dismiss()
        owner.lifecycle.removeObserver(this)
    }

    // ── Permission ────────────────────────────────────────────────────────────

    private fun hasPermission(): Boolean = PERMISSIONS.any {
        ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun shouldShowRationale(): Boolean = PERMISSIONS.any {
        ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
    }

    private fun requestPermission() {
        SharedPreferenceHelper.setSharedPreferenceBoolean(activity, Constant.KEY_LOCATION_ASKED, true)
        permissionLauncher.launch(PERMISSIONS)
    }

    private fun onPermissionResult() {
        // Precise or approximate are both accepted — accuracy tells the backend how precise it is.
        // On denial we stop quietly; the next Home launch shows the rationale or settings dialog.
        if (hasPermission()) checkLocationSettings() else finish(null)
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null)
        )
        try {
            awaitingSettings = true
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            awaitingSettings = false
            finish(null)
        }
    }

    // ── Location ──────────────────────────────────────────────────────────────

    private fun checkLocationSettings() {
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_TIMEOUT_MS).build()
            )
            .build()
        LocationServices.getSettingsClient(activity)
            .checkLocationSettings(settingsRequest)
            .addOnSuccessListener { if (isAlive()) fetchLocation() }
            .addOnFailureListener { e ->
                if (!isAlive()) return@addOnFailureListener
                if (e is ResolvableApiException) {
                    // System "Turn on location?" popup
                    resolutionLauncher.launch(IntentSenderRequest.Builder(e.resolution).build())
                } else {
                    onLocationOff()
                }
            }
    }

    @SuppressLint("MissingPermission") // guarded by hasPermission()
    private fun fetchLocation() {
        if (!hasPermission()) {
            finish(null)
            return
        }
        val source = CancellationTokenSource().also { cancellationSource = it }
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(LOCATION_TIMEOUT_MS)
            .setMaxUpdateAgeMillis(MAX_LOCATION_AGE_MS)
            .build()
        fusedClient.getCurrentLocation(request, source.token)
            .addOnSuccessListener { location ->
                cancellationSource = null
                finish(location?.takeUnless { isMock(it) })
            }
            .addOnFailureListener {
                cancellationSource = null
                finish(null)
            }
    }

    @Suppress("DEPRECATION")
    private fun isMock(location: Location): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) location.isMock
        else location.isFromMockProvider

    private fun onLocationOff() {
        if (isAlive()) ToastUtil.showWarning(activity, activity.getString(R.string.msgTurnOnLocation))
        finish(null)
    }

    private fun finish(location: Location?) {
        isRunning = false
        if (isAlive()) onResult(location)
    }

    private fun isAlive(): Boolean =
        activity.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)

    // ── Dialog ────────────────────────────────────────────────────────────────

    private fun showDialog(type: DialogType) {
        dialogType = type
        val binding = DataBindingUtil.inflate<DialogLocationPermissionBinding>(
            activity.layoutInflater, R.layout.dialog_location_permission, null, false
        )
        dialogBinding = binding
        when (type) {
            DialogType.AWARENESS -> {
                binding.tvTitle.setText(R.string.titleLocationAccess)
                binding.tvMessage.setText(R.string.msgLocationAwareness)
                binding.tvPoints.visible()
                binding.tvPositive.setText(R.string.btnAllow)
            }
            DialogType.RATIONALE -> {
                binding.tvTitle.setText(R.string.titleLocationRationale)
                binding.tvMessage.setText(R.string.msgLocationRationale)
                binding.tvPositive.setText(R.string.btnAllow)
            }
            DialogType.SETTINGS -> {
                binding.tvTitle.setText(R.string.titleLocationSettings)
                binding.tvMessage.setText(R.string.msgLocationSettings)
                binding.tvPositive.setText(R.string.btnOpenSettings)
            }
        }
        binding.onClickListener = onClickListener()

        val newDialog = Dialog(activity)
        dialog = newDialog
        newDialog.setContentView(binding.root)
        newDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        newDialog.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        newDialog.setCancelable(true)
        // Back press / outside tap behaves like "Not Now"
        newDialog.setOnCancelListener { finish(null) }
        newDialog.setOnDismissListener {
            dialog = null
            dialogBinding = null
        }
        newDialog.show()
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                dialogBinding?.btnPositive -> {
                    if (Utility.stopClick()) return@OnClickListener
                    dialog?.dismiss()
                    if (dialogType == DialogType.SETTINGS) openAppSettings() else requestPermission()
                }

                dialogBinding?.btnNegative -> {
                    if (Utility.stopClick()) return@OnClickListener
                    dialog?.dismiss()
                    finish(null)
                }
            }
        }
    }

    companion object {
        private val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private const val LOCATION_TIMEOUT_MS = 10_000L
        private const val MAX_LOCATION_AGE_MS = 60_000L
    }
}
