package com.shreefintech.paytouchconsumer.utill

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.shreefintech.paytouchconsumer.R

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

    private val context: Context = activity

    private val fileLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { validate(it) }
        }

    // ─── Open picker ──────────────────────────────────────────────────────────

    fun openPicker() {
        fileLauncher.launch("*/*")
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
        is FilePickerError.InvalidExtension  -> context.getString(R.string.msgOnlyPdfJpgJpegAllowed)
        is FilePickerError.FileTooLarge      -> context.getString(R.string.msgFileExceeds2mbLimit)
        is FilePickerError.UnableToReadFile  -> context.getString(R.string.msgUnableToReadFileTryAgain)
    }
}
