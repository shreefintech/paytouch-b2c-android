package com.shreefintech.paytouchconsumer.utill

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
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
class FilePickerUtil(private val activity: AppCompatActivity) {

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
    }

    // ─── Config ───────────────────────────────────────────────────────────────

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024 // 2MB
        private val ALLOWED_EXTENSIONS = listOf("pdf", "jpg", "jpeg")
        private const val STATE_KEY = "file_picker_util_state"
        private const val STATE_CAMERA_FILE = "camera_output_file"
    }

    // ─── Callbacks ────────────────────────────────────────────────────────────

    var onSuccess: ((FileResult) -> Unit)? = null
    var onError: ((FilePickerError) -> Unit)? = null

    /** Max bytes to compress camera-captured images to. Defaults to 2 MB; set lower for faster uploads. */
    var cameraMaxBytes: Int = 2 * 1024 * 1024

    // ─── Internals ────────────────────────────────────────────────────────────

    private val context: Context = activity

    private val fileLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { validate(it) }
        }

    private var cameraOutputFile: File? = null
    private var cameraOutputUri: Uri? = null
    private var pendingCameraStorageDir: File? = null

    // The camera app may outlive our process — persist the output path so the photo isn't dropped
    init {
        val registry = activity.savedStateRegistry
        registry.registerSavedStateProvider(STATE_KEY) {
            Bundle().apply { putString(STATE_CAMERA_FILE, cameraOutputFile?.absolutePath) }
        }
        registry.consumeRestoredStateForKey(STATE_KEY)?.getString(STATE_CAMERA_FILE)?.let { path ->
            try {
                val file = File(path)
                cameraOutputUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                cameraOutputFile = file
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val cameraPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val dir = pendingCameraStorageDir
            pendingCameraStorageDir = null
            if (granted && dir != null) launchCamera(dir)
            else if (!granted) onError?.invoke(FilePickerError.CameraPermissionDenied)
        }

    private val cameraLauncher: ActivityResultLauncher<Uri> =
        activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { _ ->
            val file = cameraOutputFile
            val uri = cameraOutputUri
            cameraOutputFile = null
            cameraOutputUri = null
            if (file != null && uri != null && file.exists() && file.length() > 0) {
                // lifecycleScope cancels on destroy, so onSuccess never reaches a dead Activity
                activity.lifecycleScope.launch(Dispatchers.IO) {
                    Utility.compressImageFile(file, cameraMaxBytes)
                    withContext(Dispatchers.Main) {
                        onSuccess?.invoke(FileResult(uri, file.name, "jpg", file.length() / (1024.0 * 1024.0)))
                    }
                }
            }
        }

    // ─── Open picker ──────────────────────────────────────────────────────────

    fun openPicker() {
        fileLauncher.launch("*/*")
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
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraOutputFile = file
        cameraOutputUri = uri
        cameraLauncher.launch(uri)
    }

    fun showSourceChooser(storageDir: File) {
        val dialogBinding = DialogSelectDocumentBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(context)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setWindowAnimations(R.style.DialogScaleFadeAnimation)
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        dialogBinding.cardCamera.setOnClickListener {
            dialog.dismiss()
            openCamera(storageDir)
        }
        dialogBinding.cardFiles.setOnClickListener {
            dialog.dismiss()
            openPicker()
        }
        dialog.show()
    }

    // ─── Validate file ────────────────────────────────────────────────────────

    private fun validate(uri: Uri) {
        val fileName = getFileName(uri) ?: run {
            onError?.invoke(FilePickerError.UnableToReadFile)
            return
        }

        val extension = fileName.substringAfterLast(".", "").lowercase()
        if (extension !in ALLOWED_EXTENSIONS) {
            onError?.invoke(FilePickerError.InvalidExtension)
            return
        }

        val fileSizeBytes = getFileSize(uri) ?: run {
            onError?.invoke(FilePickerError.UnableToReadFile)
            return
        }
        if (fileSizeBytes > MAX_FILE_SIZE_BYTES) {
            onError?.invoke(FilePickerError.FileTooLarge)
            return
        }

        onSuccess?.invoke(FileResult(uri, fileName, extension, fileSizeBytes / (1024.0 * 1024.0)))
    }

    // ─── Get file name ────────────────────────────────────────────────────────

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) name = it.getString(index)
                }
            }
        }
        return name ?: uri.path?.substringAfterLast("/")
    }

    // ─── Get file size ────────────────────────────────────────────────────────

    private fun getFileSize(uri: Uri): Long? {
        var size = 0L
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.SIZE)
                    if (index != -1) size = it.getLong(index)
                }
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
        is FilePickerError.InvalidExtension    -> context.getString(R.string.msgOnlyPdfJpgJpegAllowed)
        is FilePickerError.FileTooLarge        -> context.getString(R.string.msgFileExceeds2mbLimit)
        is FilePickerError.UnableToReadFile    -> context.getString(R.string.msgUnableToReadFileTryAgain)
        is FilePickerError.CameraPermissionDenied -> context.getString(R.string.msgCameraPermissionDenied)
    }
}
