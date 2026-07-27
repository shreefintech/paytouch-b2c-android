package com.shreefintech.paytouchconsumer.electricity.transactions

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivitySmsReceiptBinding
import com.shreefintech.paytouchconsumer.electricity.model.SmsReceiptItem
import com.shreefintech.paytouchconsumer.electricity.viewmodel.SmsReceiptViewModel
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityVerifyPaymentDataItem
import com.shreefintech.paytouchconsumer.utill.ToastType
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.visible
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class SmsReceiptActivity : BaseActivity() {

    private lateinit var binding: ActivitySmsReceiptBinding
    private var savedImageUri: Uri? = null

    private val viewModel: SmsReceiptViewModel by viewModels()

    private val writePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) performDownload()
        else ToastUtil.showDelete(mActivity, getString(R.string.msgStoragePermissionRequired))
    }

    private val receiptItem: SmsReceiptItem? by lazy {
        intent.getStringExtra(EXTRA_ITEM)?.let { Gson().fromJson(it, SmsReceiptItem::class.java) }
    }

    private val isFromPayment: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_FROM_PAYMENT, false)
    }

    companion object {
        private const val EXTRA_ITEM = "extra_item"
        private const val EXTRA_FROM_PAYMENT = "extra_from_payment"
        private const val TAB_RECEIPT = 0
        private const val TAB_DISPLAY = 1

        fun start(context: Context, item: SmsReceiptItem? = null, fromPayment: Boolean = false) {
            context.startActivity(
                Intent(context, SmsReceiptActivity::class.java).apply {
                    item?.let { putExtra(EXTRA_ITEM, Gson().toJson(it)) }
                    putExtra(EXTRA_FROM_PAYMENT, fromPayment)
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

        if (isFromPayment) {
            binding.llTitleRow.visibility = View.GONE
            binding.llReceiptContent.visibility = View.VISIBLE
            binding.llDisplayContent.visibility = View.GONE
            binding.llBtnContainer.visible()
        } else {
            binding.llTitleRow.visibility = View.VISIBLE
            selectTab(TAB_RECEIPT)
        }
        binding.onClickListener = onClickListener()
        onBack()

        populateData()
        loadLatestPayments()
    }

    // ── API Call ──────────────────────────────────────────────

    private fun loadLatestPayments() {
        viewModel.getLatestPayments(
            onLoading = { showReceiptLoading(true) },
            onSuccess = { item ->
                showReceiptLoading(false)
                populateReceiptFromApi(item)
            },
            onError = { msg ->
                showReceiptLoading(false)
                if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    // ── Populate ──────────────────────────────────────────────

    private fun populateData() {
        val item = receiptItem ?: return
        binding.tvConsumerNo.text = item.accountNo ?: "--"
        binding.tvCustomerName.text = item.username ?: "--"
        binding.tvCompanyName.text = item.companyName ?: "--"
        binding.tvReceiptDate.text = item.date ?: "--"
        binding.tvAmountPaid.text = item.amount ?: "--"
        binding.tvPaytouchTxnId.text = item.txnId ?: "--"
        binding.tvBConnectTxnId.text = item.refId ?: "--"
        binding.tvCcf.text = item.platformFee ?: "--"
        binding.tvReceiptStatus.text = getString(R.string.labelStatusBullet, item.status ?: "--")
        applyStatusStyle(item.status)
    }

    private fun populateReceiptFromApi(item: ElectricityVerifyPaymentDataItem) {
        val amount = "₹${item.totalPayable ?: "--"}"
        val consumerNo = item.subscriberNo ?: "--"
        val txnId = item.transactionId ?: "--"
        val date = formatDate(item.createdAt)
        val status = item.status ?: "Pending"

        binding.tvConsumerNo.text = consumerNo
        binding.tvCustomerName.text = item.customerName ?: "--"
        binding.tvCompanyName.text = item.operatorName ?: "--"
        binding.tvReceiptDate.text = date
        binding.tvAmountPaid.text = amount
        binding.tvPaytouchTxnId.text = txnId
        binding.tvBConnectTxnId.text = item.ccf ?: "--"
        binding.tvCcf.text = item.platformFee ?: "--"
        binding.tvReceiptStatus.text = getString(R.string.labelStatusBullet, status)
        applyStatusStyle(status)

        val smsBodyText = getString(R.string.msgSmsBody, amount, consumerNo)
        val spannable = SpannableString(smsBodyText)
        val amountStart = smsBodyText.indexOf(amount)
        if (amountStart >= 0) {
            val amountEnd = amountStart + amount.length
            spannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(mActivity, R.color.primary)), amountStart, amountEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(Typeface.BOLD), amountStart, amountEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.tvSmsBody.text = spannable
        binding.tvSmsBConnectTxn.text = txnId
        binding.tvSmsDate.text = date
    }

    private fun applyStatusStyle(status: String?) {
        val (bgColor, textColor) = when (status) {
            "success" -> Pair(R.color.toast_bg_success, R.color.toast_text_success)
            "failed"  -> Pair(R.color.toast_bg_delete, R.color.form_wizard_reject)
            else      -> Pair(R.color.toast_bg_warning, R.color.toast_text_warning)
        }
        binding.cvReceiptStatusBadge.setCardBackgroundColor(ContextCompat.getColor(mActivity, bgColor))
        binding.tvReceiptStatus.setTextColor(ContextCompat.getColor(mActivity, textColor))
    }

    private fun formatDate(createdAt: String?): String {
        if (createdAt.isNullOrBlank()) return "--"
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
            val date = input.parse(createdAt.substringBefore("."))
            if (date != null) output.format(date) else createdAt
        } catch (e: Exception) {
            createdAt
        }
    }

    // ── Tab Switching ─────────────────────────────────────────

    private fun selectTab(tab: Int) {
        val isReceipt = tab == TAB_RECEIPT
        binding.llReceiptContent.visibility = if (isReceipt) View.VISIBLE else View.GONE
        binding.llDisplayContent.visibility = if (isReceipt) View.GONE else View.VISIBLE
        binding.llBtnContainer.visibility = if (isReceipt) View.VISIBLE else View.GONE

        val activeColor = ContextCompat.getColor(mActivity, R.color.primary)
        val inactiveColor = android.graphics.Color.TRANSPARENT
        val activeTextColor = ContextCompat.getColor(mActivity, R.color.white)
        val inactiveTextColor = ContextCompat.getColor(mActivity, R.color.primary)

        binding.cvTabReceipt.setCardBackgroundColor(if (isReceipt) activeColor else inactiveColor)
        binding.cvTabDisplay.setCardBackgroundColor(if (isReceipt) inactiveColor else activeColor)
        binding.tvTabReceipt.setTextColor(if (isReceipt) activeTextColor else inactiveTextColor)
        binding.tvTabDisplay.setTextColor(if (isReceipt) inactiveTextColor else activeTextColor)
    }

    // ── Loading State ─────────────────────────────────────────

    private fun showReceiptLoading(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        binding.pbReceiptLoading.visibility = visibility
        binding.pbDisplayLoading.visibility = visibility
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
        val bitmap = captureViewAsBitmap(binding.cvReceiptCard)
        val uri = saveBitmapAndGetUri(bitmap)
        if (uri != null) {
            savedImageUri = uri
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
        val bitmap = captureViewAsBitmap(binding.cvReceiptCard)
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
            e.printStackTrace()
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
                    selectTab(TAB_RECEIPT)
                }
                binding.cvTabDisplay -> {
                    if (Utility.stopClick()) return@OnClickListener
                    selectTab(TAB_DISPLAY)
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
