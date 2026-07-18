package com.shreefintech.paytouchconsumer.utill

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.shreefintech.paytouchconsumer.R

/**
 * FilePickerUtil — Pick & validate PDF/JPG/JPEG files (max 2MB)
 *
 * STEP 1 — Register launchers in onCreate / onViewCreated (before fragment/activity starts)
 *   filePickerUtil = FilePickerUtil(this)           // Activity
 *
 * STEP 2 — Set callbacks
 *   filePickerUtil.onSuccess = { file -> binding.tvFileName.text = file.fileName }
 *   filePickerUtil.onError   = { error -> ToastUtil.showDelete(this, filePickerUtil.getErrorMessage(error)) }
 *
 * STEP 3 — Open picker
 *   binding.btnPick.setOnClickListener { filePickerUtil.openPicker() }
 */
class FilePickerUtil {

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
        object PermissionDenied : FilePickerError()
        object PermissionPermanentlyDenied : FilePickerError()
    }

    // ─── Config ───────────────────────────────────────────────────────────────

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024 // 2MB
        private val ALLOWED_EXTENSIONS = listOf("pdf", "jpg", "jpeg")
    }

    // ─── Callbacks ────────────────────────────────────────────────────────────

    var onSuccess: ((FileResult) -> Unit)? = null
    var onError: ((FilePickerError) -> Unit)? = null

    // ─── Internals ────────────────────────────────────────────────────────────

    private var context: Context
    private var activity: Activity? = null
    private var fragment: Fragment? = null

    private val fileLauncher: ActivityResultLauncher<String>
    private val permissionLauncher: ActivityResultLauncher<Array<String>>

    // ─── Constructors ─────────────────────────────────────────────────────────

    constructor(activity: AppCompatActivity) {
        this.context = activity
        this.activity = activity

        fileLauncher = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { validate(it) }
        }

        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResult(permissions)
        }
    }



    // ─── Open picker ──────────────────────────────────────────────────────────

    fun openPicker() {
        fileLauncher.launch("*/*")
    }

    // ─── Permission helpers ───────────────────────────────────────────────────

    private fun requiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            else -> emptyArray()
        }
    }


    private fun shouldShowRationale(): Boolean {
        val act = activity ?: (fragment?.requireActivity()) ?: return false
        return requiredPermissions().any {
            ActivityCompat.shouldShowRequestPermissionRationale(act, it)
        }
    }

    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        when {
            permissions.values.all { it } -> {
                // All granted — open picker
                fileLauncher.launch("*/*")
            }
            shouldShowRationale() -> {
                // Denied but can ask again
                onError?.invoke(FilePickerError.PermissionDenied)
            }
            else -> {
                // Permanently denied
                onError?.invoke(FilePickerError.PermissionPermanentlyDenied)
            }
        }
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

        val fileSizeBytes = getFileSize(uri)
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

    private fun getFileSize(uri: Uri): Long {
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
            } catch (e: Exception) { e.printStackTrace() }
        }
        return size
    }

    // ─── Error messages ───────────────────────────────────────────────────────

    fun getErrorMessage(error: FilePickerError): String = when (error) {
        is FilePickerError.InvalidExtension -> context.getString(R.string.msfOnlyPdfJpgJpegAllowed)
        is FilePickerError.FileTooLarge -> context.getString(R.string.msgFileExceeds2mbLimit)
        is FilePickerError.UnableToReadFile -> context.getString(R.string.msgUnableToReadFileTryAgain)
        is FilePickerError.PermissionDenied -> context.getString(R.string.msgStoragePermissionDeniedPleaseAllow)
        is FilePickerError.PermissionPermanentlyDenied -> context.getString(R.string.msgPermissionPermanentlyDenied)
    }

}