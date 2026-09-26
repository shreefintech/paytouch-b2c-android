package com.shreefintech.paytouchconsumer.kyc.identity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Size
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivitySelfieCaptureBinding
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Live selfie with active liveness check (position → blink → hold still).
 * The photo is auto-captured only after [LivenessChallengeHelper] passes, written as an upright
 * JPEG to the file path supplied by the caller, and the Activity finishes with RESULT_OK.
 * Face detection runs fully on-device (bundled ML Kit model) — no frame leaves the phone.
 */
class SelfieCaptureActivity : BaseActivity() {

    companion object {
        private const val EXTRA_OUTPUT_PATH = "extra_output_path"
        private const val MAX_CAPTURE_ATTEMPTS = 2
        private const val JPEG_QUALITY = 92

        fun buildIntent(context: Context, outputFile: File): Intent =
            Intent(context, SelfieCaptureActivity::class.java).apply {
                putExtra(EXTRA_OUTPUT_PATH, outputFile.absolutePath)
            }
    }

    private lateinit var binding: ActivitySelfieCaptureBinding

    private val outputPath: String? by lazy { intent.getStringExtra(EXTRA_OUTPUT_PATH) }

    private val liveness = LivenessChallengeHelper()
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val faceDetectorLazy = lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.2f)
                .enableTracking()
                .build()
        )
    }
    private val faceDetector: FaceDetector by faceDetectorLazy

    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null

    /** Set when ImageCapture keeps failing — the analyzer then saves the next live frame instead. */
    private val grabNextFrame = AtomicBoolean(false)

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            when {
                granted -> startCamera()
                // No rationale after a denial = "Don't ask again" / blocked — the dialog will never
                // show again, so send the user to app settings instead of a dead-end toast loop.
                !ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.CAMERA) -> {
                    ToastUtil.showDelete(mActivity, getString(R.string.msgCameraPermissionSettings))
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                    )
                    finish()
                }
                else -> {
                    ToastUtil.showDelete(mActivity, getString(R.string.msgCameraPermissionRequired))
                    finish()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelfieCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Camera runs edge-to-edge; only the toolbar is pushed below the status bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.lytToolbar.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top
            }
            insets
        }
        // Dark camera background → light status/nav bar icons.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        binding.lytToolbar.ivLogo.visibility = View.GONE

        binding.onClickListener = onClickListener()

        if (outputPath == null) {
            finish()
            return
        }

        setupLiveness()

        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else if (savedInstanceState == null) {
            // On recreation a pending request is re-delivered to the launcher — don't ask twice.
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupLiveness() {
        liveness.onStateChanged = { stage, instruction -> renderState(stage, instruction) }
        liveness.onPassed = { capturePhoto(attempt = 1) }
        liveness.onTimeout = {
            ToastUtil.showDelete(mActivity, getString(R.string.msgLivenessTimeout))
        }
        liveness.reset()
    }

    // ─── UI state ───────────────────────────────────────────────────────────────

    private fun renderState(stage: LivenessStage, instruction: LivenessInstruction) {
        binding.tvInstruction.text = getString(instructionText(instruction))
        // Circle turns green once the blink is verified (hold / capture).
        val passedBlink = stage == LivenessStage.HOLD || stage == LivenessStage.DONE
        binding.ovFaceCircle.setStrokeColor(
            ContextCompat.getColor(mActivity, if (passedBlink) R.color.fetch_bill_stroke else R.color.white)
        )
    }

    private fun instructionText(instruction: LivenessInstruction): Int = when (instruction) {
        LivenessInstruction.ALIGN_FACE -> R.string.msgLivenessAlignFace
        LivenessInstruction.SINGLE_FACE_ONLY -> R.string.msgLivenessSingleFace
        LivenessInstruction.MOVE_CLOSER -> R.string.msgLivenessMoveCloser
        LivenessInstruction.MOVE_BACK -> R.string.msgLivenessMoveBack
        LivenessInstruction.LOOK_STRAIGHT -> R.string.msgLivenessLookStraight
        LivenessInstruction.BLINK -> R.string.msgLivenessBlink
        LivenessInstruction.BLINK_AGAIN -> R.string.msgLivenessBlinkAgain
        LivenessInstruction.HOLD_STILL -> R.string.msgLivenessHoldStill
        LivenessInstruction.CAPTURING -> R.string.msgLivenessCapturing
    }

    // ─── Camera ─────────────────────────────────────────────────────────────────

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(mActivity)
        providerFuture.addListener({
            if (isFinishing || isDestroyed) return@addListener
            try {
                val provider = providerFuture.get()
                val selector = CameraSelector.DEFAULT_FRONT_CAMERA
                if (!provider.hasCamera(selector)) {
                    onCameraUnavailable()
                    return@addListener
                }

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.pvCamera.surfaceProvider)
                }
                // Capped resolution: many front cameras are 16–32 MP, and decoding that to a Bitmap
                // (plus the rotated copy) would OOM. ~2.7 MP is plenty for a KYC selfie.
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(1920, 1440),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                                )
                            )
                            .build()
                    )
                    .build()
                // Small analysis frames are enough for face detection and keep it fast.
                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(640, 480),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                )
                            )
                            .build()
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, ::analyzeFrame) }

                provider.unbindAll()
                imageCapture = try {
                    provider.bindToLifecycle(this, selector, preview, capture, analysis)
                    capture
                } catch (e: IllegalArgumentException) {
                    // Some LEGACY-level cameras can't run 3 streams — keep preview + analysis and
                    // let capturePhoto() save the live frame instead of blocking KYC.
                    e.printStackTrace()
                    provider.unbindAll()
                    provider.bindToLifecycle(this, selector, preview, analysis)
                    null
                }
                imageAnalysis = analysis
            } catch (e: Exception) {
                e.printStackTrace()
                onCameraUnavailable()
            }
        }, ContextCompat.getMainExecutor(mActivity))
    }

    private fun onCameraUnavailable() {
        ToastUtil.showDelete(mActivity, getString(R.string.msgCameraUnavailable))
        finish()
    }

    /** Runs on [analysisExecutor]; ML Kit delivers results on the main thread. */
    @OptIn(ExperimentalGetImage::class)
    private fun analyzeFrame(proxy: ImageProxy) {
        if (grabNextFrame.compareAndSet(true, false)) {
            saveFallbackFrame(proxy)
            return
        }

        val mediaImage = proxy.image
        if (mediaImage == null) {
            proxy.close()
            return
        }
        val rotation = proxy.imageInfo.rotationDegrees
        val uprightWidth = if (rotation % 180 == 0) proxy.width else proxy.height
        val uprightHeight = if (rotation % 180 == 0) proxy.height else proxy.width
        val timestampMs = SystemClock.elapsedRealtime()

        try {
            faceDetector.process(InputImage.fromMediaImage(mediaImage, rotation))
                // Activity-scoped listener is auto-removed on onStop — no callbacks after destroy.
                .addOnSuccessListener(this) { faces ->
                    liveness.onFrame(toFrame(faces, uprightWidth, uprightHeight, timestampMs))
                }
                .addOnCompleteListener { proxy.close() }
        } catch (_: IllegalStateException) {
            // Detector already closed (Activity destroying) — drop the frame.
            proxy.close()
        }
    }

    private fun toFrame(faces: List<Face>, width: Int, height: Int, timestampMs: Long): LivenessFrameItem {
        val face = faces.singleOrNull()
        if (face == null || width == 0 || height == 0) {
            return LivenessFrameItem(
                faceCount = faces.size, trackingId = null, centerXRatio = 0f, centerYRatio = 0f,
                widthRatio = 0f, yaw = 0f, roll = 0f, leftEyeOpen = null, rightEyeOpen = null,
                timestampMs = timestampMs
            )
        }
        val box = face.boundingBox
        return LivenessFrameItem(
            faceCount = 1,
            trackingId = face.trackingId,
            centerXRatio = box.exactCenterX() / width,
            centerYRatio = box.exactCenterY() / height,
            widthRatio = box.width().toFloat() / width,
            yaw = face.headEulerAngleY,
            roll = face.headEulerAngleZ,
            leftEyeOpen = face.leftEyeOpenProbability,
            rightEyeOpen = face.rightEyeOpenProbability,
            timestampMs = timestampMs
        )
    }

    // ─── Capture ────────────────────────────────────────────────────────────────

    /**
     * In-memory capture (we write the JPEG ourselves) — file-based OnImageSavedCallback fails on
     * some front cameras. Retries once, then falls back to saving the next live analysis frame.
     */
    private fun capturePhoto(attempt: Int) {
        val capture = imageCapture
        if (capture == null) {
            fallbackToFrame()
            return
        }
        imageAnalysis?.clearAnalyzer()

        capture.takePicture(ContextCompat.getMainExecutor(mActivity),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    lifecycleScope.launch {
                        val saved = withContext(Dispatchers.IO) {
                            try {
                                saveUpright(image.toBitmap(), image.imageInfo.rotationDegrees)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                false
                            } catch (e: OutOfMemoryError) {
                                e.printStackTrace()
                                false
                            } finally {
                                image.close()
                            }
                        }
                        if (saved) finishWithSelfie() else fallbackToFrame()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    if (isFinishing || isDestroyed) return
                    if (attempt < MAX_CAPTURE_ATTEMPTS) capturePhoto(attempt + 1) else fallbackToFrame()
                }
            })
    }

    private fun fallbackToFrame() {
        val analysis = imageAnalysis
        if (analysis == null) {
            onCaptureFailed()
            return
        }
        grabNextFrame.set(true)
        analysis.setAnalyzer(analysisExecutor, ::analyzeFrame)
    }

    /** Runs on [analysisExecutor]. */
    private fun saveFallbackFrame(proxy: ImageProxy) {
        imageAnalysis?.clearAnalyzer()
        val saved = try {
            saveUpright(proxy.toBitmap(), proxy.imageInfo.rotationDegrees)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            false
        } finally {
            proxy.close()
        }
        lifecycleScope.launch { if (saved) finishWithSelfie() else onCaptureFailed() }
    }

    /** Background thread only. Rotates to upright (no EXIF needed downstream) and writes the JPEG. */
    private fun saveUpright(source: Bitmap, rotation: Int): Boolean {
        val path = outputPath ?: return false
        val bitmap = if (rotation != 0) {
            Bitmap.createBitmap(
                source, 0, 0, source.width, source.height,
                Matrix().apply { postRotate(rotation.toFloat()) }, true
            ).also { if (it !== source) source.recycle() }
        } else source
        return try {
            File(path).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        } finally {
            bitmap.recycle()
        }
    }

    private fun finishWithSelfie() {
        if (isFinishing || isDestroyed) return
        setResult(RESULT_OK)
        finish()
    }

    private fun onCaptureFailed() {
        if (isFinishing || isDestroyed) return
        ToastUtil.showDelete(mActivity, getString(R.string.msgLivenessCaptureFailed))
        liveness.reset()
        imageAnalysis?.setAnalyzer(analysisExecutor, ::analyzeFrame)
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.lytToolbar.ivBack -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        imageAnalysis?.clearAnalyzer()
        analysisExecutor.shutdown()
        liveness.onStateChanged = null
        liveness.onPassed = null
        liveness.onTimeout = null
        // Only close if it was ever created (early finish skips camera setup entirely).
        if (faceDetectorLazy.isInitialized()) faceDetector.close()
    }
}
