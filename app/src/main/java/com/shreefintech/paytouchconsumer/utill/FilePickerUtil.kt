package com.shreefintech.paytouchconsumer.utill

import android.Manifest
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.DialogSelectDocumentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * FilePickerUtil — Pick & validate PDF/JPG/JPEG files (max 2MB)
 *
 * STEP 1 — Register launchers in onCreate (before activity starts)
 *   filePickerUtil = FilePickerUtil(this)
 *
 * STEP 2 — Set callbacks
 *   filePickerUtil.onSuccess = { file -> binding.tvFileName.text = file.fileName }
 *   filePickerUtil.onError   = { error -> ToastUtil.showDelete(this, filePickerUtil.getErrorMessage(error)) }
 *
 * STEP 3 — Open picker
 *   binding.btnPick.setOnClickListener { filePickerUtil.openPicker() }
 *
 * Note: GetContent does not require explicit storage permissions — the system picker
 * grants URI read access automatically upon selection.
 */
class FilePickerUtil(activity: AppCompatActivity) {

    // ─── Models ───────────────────────────────────────────────────────────────

    data class FileResult(
        val uri: Uri,
        val fileName: String,
        val extension: String,
        val sizeMB: Double
    )

    sealed class FilePickerError {
        object InvalidExtension : FilePickerError()
        object FileTooLarge : FilePickerError()
        object UnableToReadFile : FilePickerError()
        object CameraPermissionDenied : FilePickerError()
        object CameraPermissionBlocked : FilePickerError()
        object CameraUnavailable : FilePickerError()
        object FilePickerUnavailable : FilePickerError()
    }

    // ─── Config ───────────────────────────────────────────────────────────────

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024 // 2MB
        private val ALLOWED_EXTENSIONS = listOf("pdf", "jpg", "jpeg")

        private const val STATE_PROVIDER_KEY = "file_picker_util"
        private const val STATE_CAMERA_OUTPUT_PATH = "camera_output_path"
        private const val STATE_PENDING_CAMERA_DIR = "pending_camera_dir"
    }

    // ─── Callbacks ────────────────────────────────────────────────────────────

    var onSuccess: ((FileResult) -> Unit)? = null
    var onError: ((FilePickerError) -> Unit)? = null

    /** Max bytes to compress camera-captured images to. Defaults to 2 MB; set lower for faster uploads. */
    var cameraMaxBytes: Int = 2 * 1024 * 1024

    // ─── Internals ────────────────────────────────────────────────────────────

    private val hostActivity: AppCompatActivity = activity
    private val context: Context = activity
    private val lifecycleOwner: LifecycleOwner = activity
    private var sourceDialog: Dialog? = null
    // Main-thread only — true from a picked/captured file until its validation or compression reports back
    private var isProcessing = false

    init {
        // Dismiss the source chooser with its Activity so a recreation never leaks its window.
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                sourceDialog?.dismiss()
                sourceDialog = null
            }
        })
    }

    private val fileLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { validate(it) }
        }

    // Persisted via SavedStateRegistry — the process is often killed while the camera app or the
    // permission dialog is in front, and the result must still find its output file / directory.
    private var cameraOutputFile: File? = null
    private var pendingCameraStorageDir: File? = null

    private val cameraPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val dir = pendingCameraStorageDir
            pendingCameraStorageDir = null
            when {
                granted -> {
                    // A later revoke from Settings puts the permission back in "ask" state.
                    SharedPreferenceHelper.setSharedPreferenceBoolean(context, Constant.KEY_CAMERA_DENIED, false)
                    if (dir != null) launchCamera(dir) else onError?.invoke(FilePickerError.UnableToReadFile)
                }
                // Rationale after a denial = explicit "Deny". Remember it so a later denial with no
                // rationale can be recognised as "Don't ask again" (same rule as SelfieCaptureActivity).
                ActivityCompat.shouldShowRequestPermissionRationale(hostActivity, Manifest.permission.CAMERA) -> {
                    SharedPreferenceHelper.setSharedPreferenceBoolean(context, Constant.KEY_CAMERA_DENIED, true)
                    onError?.invoke(FilePickerError.CameraPermissionDenied)
                }
                // No rationale after an earlier explicit denial = blocked — the system dialog will never
                // show again, so send the user to app settings instead of a dead-end toast loop.
                SharedPreferenceHelper.getSharedPreferenceBoolean(context, Constant.KEY_CAMERA_DENIED, false) -> {
                    onError?.invoke(FilePickerError.CameraPermissionBlocked)
                    openAppSettings()
                }
                // No rationale and never explicitly denied = dialog dismissed (Back / tap outside).
                else -> onError?.invoke(FilePickerError.CameraPermissionDenied)
            }
        }

    private val cameraLauncher: ActivityResultLauncher<Uri> =
        activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { _ ->
            val file = cameraOutputFile
            cameraOutputFile = null
            if (file == null) {
                // Capture state was lost — tell the user instead of silently dropping the photo.
                onError?.invoke(FilePickerError.UnableToReadFile)
                return@registerForActivityResult
            }
            if (!file.exists() || file.length() == 0L) {
                // Capture cancelled — drop the empty placeholder the camera app may have created.
                file.delete()
                return@registerForActivityResult
            }
            val uri = fileProviderUri(file)
            // lifecycleScope: if the Activity is destroyed mid-compression the callback is skipped.
            isProcessing = true
            lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                Utility.compressImageFile(file, cameraMaxBytes)
                val sizeBytes = file.length()
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    if (sizeBytes > MAX_FILE_SIZE_BYTES) {
                        onError?.invoke(FilePickerError.FileTooLarge)
                    } else {
                        onSuccess?.invoke(FileResult(uri, file.name, "jpg", sizeBytes / (1024.0 * 1024.0)))
                    }
                }
            }
        }

    init {
        // Constructed after super.onCreate(), so restored state is already available here.
        val registry = activity.savedStateRegistry
        registry.consumeRestoredStateForKey(STATE_PROVIDER_KEY)?.let { state ->
            cameraOutputFile = state.getString(STATE_CAMERA_OUTPUT_PATH)?.let(::File)
            pendingCameraStorageDir = state.getString(STATE_PENDING_CAMERA_DIR)?.let(::File)
        }
        registry.registerSavedStateProvider(STATE_PROVIDER_KEY) {
            bundleOf(
                STATE_CAMERA_OUTPUT_PATH to cameraOutputFile?.absolutePath,
                STATE_PENDING_CAMERA_DIR to pendingCameraStorageDir?.absolutePath
            )
        }
    }

    // ─── Open picker ──────────────────────────────────────────────────────────

    fun openPicker() {
        try {
            fileLauncher.launch("*/*")
        } catch (e: ActivityNotFoundException) {
            // Kiosk / MDM builds can disable the system document picker
            e.printStackTrace()
            onError?.invoke(FilePickerError.FilePickerUnavailable)
        }
    }

    fun openCamera(storageDir: File) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera(storageDir)
        } else {
            pendingCameraStorageDir = storageDir
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera(storageDir: File) {
        storageDir.mkdirs()
        val file = File(storageDir, "doc_${System.currentTimeMillis()}.jpg")
        cameraOutputFile = file
        try {
            cameraLauncher.launch(fileProviderUri(file))
        } catch (e: ActivityNotFoundException) {
            // Camera is optional in the manifest — camera-less or MDM-restricted devices have no capture app
            e.printStackTrace()
            cameraOutputFile = null
            file.delete()
            onError?.invoke(FilePickerError.CameraUnavailable)
        }
    }

    private fun fileProviderUri(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun openAppSettings() {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            )
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    /**
     * Returns false (and warns) while a previous file is still being validated / compressed — a second pick
     * would race the first and the caller's "active slot" could receive the wrong file.
     */
    fun showSourceChooser(storageDir: File): Boolean {
        if (isProcessing) {
            ToastUtil.showWarning(hostActivity, context.getString(R.string.msgFileStillProcessing))
            return false
        }
        val dialogBinding = DialogSelectDocumentBinding.inflate(LayoutInflater.from(context))
        sourceDialog?.dismiss()
        val dialog = Dialog(context)
        sourceDialog = dialog
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setWindowAnimations(R.style.DialogScaleFadeAnimation)
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        dialog.setOnDismissListener { if (sourceDialog === dialog) sourceDialog = null }
        // Per-dialog guard (not Utility.stopClick — the 800 ms global window was just consumed by
        // the tap that opened this dialog) so a double tap cannot launch two pickers.
        var isOptionChosen = false
        val onClickListener = View.OnClickListener {
            if (isOptionChosen) return@OnClickListener
            when (it) {
                dialogBinding.cardCamera -> {
                    isOptionChosen = true
                    dialog.dismiss()
                    openCamera(storageDir)
                }
                dialogBinding.cardFiles -> {
                    isOptionChosen = true
                    dialog.dismiss()
                    openPicker()
                }
            }
        }
        dialogBinding.cardCamera.setOnClickListener(onClickListener)
        dialogBinding.cardFiles.setOnClickListener(onClickListener)
        dialog.show()
        return true
    }

    // ─── Validate file ────────────────────────────────────────────────────────

    private fun validate(uri: Uri) {
        // Cloud providers (Drive / OneDrive) download the file on openFileDescriptor — keep lookups off Main.
        // lifecycleScope: if the Activity is destroyed mid-lookup the callback is skipped.
        isProcessing = true
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val fileName = getFileName(uri)
            val extension = fileName?.substringAfterLast(".", "")?.lowercase()
            // Size only for an allowed extension, so a rejected file is never downloaded
            val fileSizeBytes = if (extension in ALLOWED_EXTENSIONS) getFileSize(uri) else null
            withContext(Dispatchers.Main) {
                isProcessing = false
                when {
                    fileName == null || extension == null      -> onError?.invoke(FilePickerError.UnableToReadFile)
                    extension !in ALLOWED_EXTENSIONS           -> onError?.invoke(FilePickerError.InvalidExtension)
                    fileSizeBytes == null                      -> onError?.invoke(FilePickerError.UnableToReadFile)
                    fileSizeBytes > MAX_FILE_SIZE_BYTES        -> onError?.invoke(FilePickerError.FileTooLarge)
                    else -> onSuccess?.invoke(FileResult(uri, fileName, extension, fileSizeBytes / (1024.0 * 1024.0)))
                }
            }
        }
    }

    // ─── Get file name ────────────────────────────────────────────────────────

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) name = it.getString(index)
                    }
                }
            } catch (e: Exception) {
                // Third-party document providers can refuse the query (Security / IllegalArgument / Unsupported)
                e.printStackTrace()
            }
        }
        return name ?: uri.path?.substringAfterLast("/")
    }

    // ─── Get file size ────────────────────────────────────────────────────────

    private fun getFileSize(uri: Uri): Long? {
        var size = 0L
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(OpenableColumns.SIZE)
                        if (index != -1) size = it.getLong(index)
                    }
                }
            } catch (e: Exception) {
                // Falls through to the openFileDescriptor fallback below
                e.printStackTrace()
            }
        }
        if (size == 0L) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { size = it.statSize }
            } catch (e: Exception) {
                return null
            }
        }
        return size
    }

    // ─── Error messages ───────────────────────────────────────────────────────

    fun getErrorMessage(error: FilePickerError): String = when (error) {
        is FilePickerError.InvalidExtension        -> context.getString(R.string.msgOnlyPdfJpgJpegAllowed)
        is FilePickerError.FileTooLarge            -> context.getString(R.string.msgFileExceeds2mbLimit)
        is FilePickerError.UnableToReadFile        -> context.getString(R.string.msgUnableToReadFileTryAgain)
        is FilePickerError.CameraPermissionDenied  -> context.getString(R.string.msgCameraPermissionDenied)
        is FilePickerError.CameraPermissionBlocked -> context.getString(R.string.msgCameraPermissionBlockedDoc)
        is FilePickerError.CameraUnavailable       -> context.getString(R.string.msgNoCameraApp)
        is FilePickerError.FilePickerUnavailable   -> context.getString(R.string.msgNoFilePickerApp)
    }
}
