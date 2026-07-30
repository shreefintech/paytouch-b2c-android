package com.shreefintech.paytouchconsumer.electricity.transactions

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivitySmsReceiptBinding
import com.shreefintech.paytouchconsumer.electricity.model.SmsReceiptItem
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.ToastType
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.gone
import com.shreefintech.paytouchconsumer.utill.Utility.visible
import java.io.File
import java.io.FileOutputStream

class SmsReceiptActivity : BaseActivity() {

    private lateinit var binding: ActivitySmsReceiptBinding

    private var isReceiptTab = true

    private val writePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) performDownload()
        else ToastUtil.showDelete(mActivity, getString(R.string.msgStoragePermissionRequired))
    }

    private val receiptItem: SmsReceiptItem? by lazy {
        intent.getStringExtra(EXTRA_ITEM)?.let { Gson().fromJson(it, SmsReceiptItem::class.java) }
    }

    companion object {
        private const val EXTRA_ITEM = "extra_item"

        fun start(context: Context, item: SmsReceiptItem) {
            context.startActivity(
                Intent(context, SmsReceiptActivity::class.java).apply {
                    putExtra(EXTRA_ITEM, Gson().toJson(item))
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySmsReceiptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        LiquidGlassEffect.attach(
            targetView = binding.flCard,
            rootView = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion = 0f,
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur),
            strokeColor = Color.argb(180, 213, 38, 98),
            strokeWidth = 1,
            solidStroke = true,
        )

        populateData()
        selectReceiptTab()
        binding.onClickListener = onClickListener()
        onBack()
    }

    private fun populateData() {
        val item = receiptItem ?: return
        val txnId = item.txnId ?: "--"
        val amount = item.amount ?: "--"
        val status = item.status ?: "--"
        val username = item.username ?: "--"
        val date = item.date ?: "--"
        val platformFee = item.platformFee ?: "--"
        val refId = item.refId ?: "--"
        val accountNo = item.accountNo ?: "--"
        val companyName = item.companyName ?: "--"

        // Receipt tab fields
        binding.tvConsumerNo.text = accountNo
        binding.tvCustomerName.text = username
        binding.tvCompanyName.text = companyName
        binding.tvReceiptDate.text = date
        binding.tvAmountPaid.text = amount
        binding.tvPaytouchTxnId.text = txnId
        binding.tvBConnectTxnId.text = refId
        binding.tvCcf.text = platformFee
        binding.tvReceiptStatus.text = "● $status"

        val (bgColor, textColor) = when (status) {
            "Success" -> Pair(R.color.toast_bg_success, R.color.toast_text_success)
            "Failed" -> Pair(R.color.toast_bg_delete, R.color.form_wizard_reject)
            else -> Pair(R.color.toast_bg_warning, R.color.toast_text_warning)
        }
        binding.cvReceiptStatusBadge.setCardBackgroundColor(
            ContextCompat.getColor(mActivity, bgColor)
        )
        binding.tvReceiptStatus.setTextColor(ContextCompat.getColor(mActivity, textColor))

        // Display tab fields
        binding.tvSmsBody.text = getString(R.string.msgSmsBody, amount, accountNo)
        binding.tvSmsBConnectTxn.text = refId
        binding.tvSmsDate.text = date
    }

    // ── Tab switching ─────────────────────────────────────────

    private fun selectReceiptTab() {
        isReceiptTab = true
        val primary = ContextCompat.getColor(mActivity, R.color.primary)
        val transparent = Color.TRANSPARENT

        // Active pill: white bg, primary text
        binding.cvTabReceipt.setCardBackgroundColor(primary)
        binding.tvTabReceipt.setTextColor(Color.WHITE)
        binding.tvTabReceipt.text = getString(R.string.tabReceipt)

        // Inactive pill: primary bg (blends into container), white text
        binding.cvTabDisplay.setCardBackgroundColor(transparent)
        binding.tvTabDisplay.setTextColor(primary)

        binding.tvTabDisplay.text = getString(R.string.tabDisplay)

        binding.llReceiptContent.visibility = View.VISIBLE
        binding.llDisplayContent.visibility = View.GONE
        binding.llBtnContainer.visible()
    }

    private fun selectDisplayTab() {
        isReceiptTab = false
        val primary = ContextCompat.getColor(mActivity, R.color.primary)
        val transparent = Color.TRANSPARENT

        // Active pill: white bg, primary text
        binding.cvTabDisplay.setCardBackgroundColor(primary)
        binding.tvTabDisplay.setTextColor(Color.WHITE)
        binding.tvTabDisplay.text = getString(R.string.tabDisplay)

        // Inactive pill: primary bg (blends into container), white text
        binding.cvTabReceipt.setCardBackgroundColor(transparent)
        binding.tvTabReceipt.setTextColor(primary)
        binding.tvTabReceipt.text = getString(R.string.tabReceipt)

        binding.llReceiptContent.visibility = View.GONE
        binding.llDisplayContent.visibility = View.VISIBLE
        binding.llBtnContainer.gone()
    }

    // ── Download & Share ──────────────────────────────────────

    private fun downloadReceipt() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        performDownload()
    }

    private fun performDownload() {
        val target = if (isReceiptTab) binding.cvReceiptCard else binding.cvSmsCard
        val bitmap = captureViewAsBitmap(target)
        val uri = saveBitmapAndGetUri(bitmap)
        if (uri != null) {
            ToastUtil.showInActivityWithAction(
                activity = mActivity,
                message = getString(R.string.msgReceiptDownloaded),
                type = ToastType.SUCCESS,
                actionLabel = getString(R.string.btnOpen),
                onAction = { openImageInGallery(uri) }
            )
        } else {
            ToastUtil.showDelete(mActivity, getString(R.string.msgReceiptDownloadFailed))
        }
    }

    private fun shareReceipt() {
        val target = if (isReceiptTab) binding.cvReceiptCard else binding.cvSmsCard
        val bitmap = captureViewAsBitmap(target)
        try {
            val dir = File(cacheDir, "receipts").also { it.mkdirs() }
            val file = File(dir, "receipt_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.titleShareReceipt)))
        } catch (e: Exception) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgReceiptShareFailed))
        }
    }

    private fun captureViewAsBitmap(view: View): Bitmap {
        val scale = 2f
        val bitmap = Bitmap.createBitmap(
            (view.width * scale).toInt().coerceAtLeast(1),
            (view.height * scale).toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        // Direct pixel-level fill — guarantees fully opaque white in every corner,
        // including the transparent corner pixels left by the card's rounded clip.
        bitmap.eraseColor(Color.WHITE)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        view.draw(canvas)
        return bitmap
    }

    private fun saveBitmapAndGetUri(bitmap: Bitmap): Uri? {
        val filename = "PayTouch_Receipt_${System.currentTimeMillis()}.png"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/PayTouch"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: return null
                contentResolver.openOutputStream(uri)
                    ?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
                uri
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "PayTouch"
                ).also { it.mkdirs() }
                val file = File(dir, filename)
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                android.media.MediaScannerConnection.scanFile(
                    mActivity, arrayOf(file.absolutePath), null, null
                )
                FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun openImageInGallery(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/png")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent.createChooser(intent, getString(R.string.btnOpen)))
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.lytToolbar.ivBack -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onBackPressedDispatcher.onBackPressed()
                }
                binding.cvTabReceipt -> {
                    if (Utility.stopClick()) return@OnClickListener
                    selectReceiptTab()
                }
                binding.cvTabDisplay -> {
                    if (Utility.stopClick()) return@OnClickListener
                    selectDisplayTab()
                }
                binding.cvDownload -> {
                    if (Utility.stopClick()) return@OnClickListener
                    downloadReceipt()
                }
                binding.cvShare -> {
                    if (Utility.stopClick()) return@OnClickListener
                    shareReceipt()
                }
            }
        }
    }
}
