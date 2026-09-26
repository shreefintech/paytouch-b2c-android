package com.shreefintech.paytouchconsumer.kyc.identity

import kotlin.math.abs

/**
 * One analysed camera frame, reduced to the values the liveness check needs.
 * Position/size are ratios of the upright image (0..1) so the helper is resolution-independent.
 * [yaw]/[roll] are ML Kit `headEulerAngleY`/`headEulerAngleZ` in degrees.
 */
data class LivenessFrameItem(
    val faceCount: Int,
    val trackingId: Int?,
    val centerXRatio: Float,
    val centerYRatio: Float,
    val widthRatio: Float,
    val yaw: Float,
    val roll: Float,
    val leftEyeOpen: Float?,
    val rightEyeOpen: Float?,
    val timestampMs: Long
)

enum class LivenessStage { POSITION, BLINK, HOLD, DONE }

enum class LivenessInstruction {
    ALIGN_FACE, SINGLE_FACE_ONLY, MOVE_CLOSER, MOVE_BACK, LOOK_STRAIGHT,
    BLINK, BLINK_AGAIN, HOLD_STILL, CAPTURING
}

/**
 * Active liveness challenge: position face → blink [REQUIRED_BLINKS] times → hold still with eyes open → pass.
 * Any second face, lost face or tracking-id change restarts the challenge (prevents face swap
 * mid-check). Pure Kotlin — no Android types — so it can be unit tested with synthetic frames.
 * Not thread-safe: feed frames from a single thread (ML Kit success listeners run on main).
 */
class LivenessChallengeHelper {

    var onStateChanged: ((LivenessStage, LivenessInstruction) -> Unit)? = null
    var onPassed: (() -> Unit)? = null
    var onTimeout: (() -> Unit)? = null

    private var stage = LivenessStage.POSITION
    private var lastStage: LivenessStage? = null
    private var lastInstruction: LivenessInstruction? = null
    private var lockedTrackingId: Int? = null
    private var challengeStartedAt = 0L
    private var stableFrames = 0
    private var sawEyesOpen = false
    private var eyesClosedAt: Long? = null
    private var blinkCount = 0

    fun reset() {
        resetState()
        lastStage = null
        lastInstruction = null
        emit(LivenessInstruction.ALIGN_FACE)
    }

    fun onFrame(frame: LivenessFrameItem) {
        if (stage == LivenessStage.DONE) return

        if (stage != LivenessStage.POSITION && frame.timestampMs - challengeStartedAt > CHALLENGE_TIMEOUT_MS) {
            reset()
            onTimeout?.invoke()
            return
        }

        when {
            frame.faceCount == 0 -> { restartIfInProgress(); emit(LivenessInstruction.ALIGN_FACE); return }
            frame.faceCount > 1 -> { restartIfInProgress(); emit(LivenessInstruction.SINGLE_FACE_ONLY); return }
            lockedTrackingId != null && frame.trackingId != lockedTrackingId -> {
                restartIfInProgress(); emit(LivenessInstruction.ALIGN_FACE); return
            }
        }

        when (stage) {
            LivenessStage.POSITION -> handlePosition(frame)
            LivenessStage.BLINK -> handleBlink(frame)
            LivenessStage.HOLD -> handleHold(frame)
            LivenessStage.DONE -> Unit
        }
    }

    private fun handlePosition(frame: LivenessFrameItem) {
        val hint = positionHint(frame)
        if (hint != null) {
            stableFrames = 0
            emit(hint)
            return
        }
        if (++stableFrames < STABLE_FRAMES) return
        lockedTrackingId = frame.trackingId
        challengeStartedAt = frame.timestampMs
        moveTo(LivenessStage.BLINK)
        emit(LivenessInstruction.BLINK)
    }

    private fun handleBlink(frame: LivenessFrameItem) {
        if (frame.widthRatio < MIN_FACE_WIDTH * 0.8f) { restartIfInProgress(); emit(LivenessInstruction.MOVE_CLOSER); return }
        val left = frame.leftEyeOpen ?: return
        val right = frame.rightEyeOpen ?: return
        val open = left > EYE_OPEN && right > EYE_OPEN
        val closed = left < EYE_CLOSED && right < EYE_CLOSED
        val closedAt = eyesClosedAt

        when {
            closedAt == null && open -> sawEyesOpen = true
            closedAt == null && closed && sawEyesOpen -> eyesClosedAt = frame.timestampMs
            closedAt != null && frame.timestampMs - closedAt > BLINK_MAX_MS -> {
                // Eyes held shut too long — not a natural blink, wait for a fresh open→closed→open.
                sawEyesOpen = false
                eyesClosedAt = null
            }
            closedAt != null && open -> {
                // Blink completed — eyes are open again, so the next closed frame starts blink #2.
                eyesClosedAt = null
                sawEyesOpen = true
                if (++blinkCount < REQUIRED_BLINKS) {
                    emit(LivenessInstruction.BLINK_AGAIN)
                } else {
                    moveTo(LivenessStage.HOLD)
                    emit(LivenessInstruction.HOLD_STILL)
                }
            }
        }
    }

    /** Final frontal frame check so the captured selfie is centred, straight and eyes-open. */
    private fun handleHold(frame: LivenessFrameItem) {
        val hint = positionHint(frame)
        if (hint != null) {
            stableFrames = 0
            emit(hint)
            return
        }
        emit(LivenessInstruction.HOLD_STILL)
        val eyesOpen = (frame.leftEyeOpen ?: 0f) > EYE_OPEN && (frame.rightEyeOpen ?: 0f) > EYE_OPEN
        if (!eyesOpen) { stableFrames = 0; return }
        if (++stableFrames < STABLE_FRAMES) return
        moveTo(LivenessStage.DONE)
        emit(LivenessInstruction.CAPTURING)
        onPassed?.invoke()
    }

    /** Null when the face is centred, correctly sized and looking straight at the camera. */
    private fun positionHint(frame: LivenessFrameItem): LivenessInstruction? = when {
        frame.widthRatio < MIN_FACE_WIDTH -> LivenessInstruction.MOVE_CLOSER
        frame.widthRatio > MAX_FACE_WIDTH -> LivenessInstruction.MOVE_BACK
        abs(frame.centerXRatio - 0.5f) > CENTER_TOLERANCE ||
                abs(frame.centerYRatio - 0.5f) > CENTER_TOLERANCE -> LivenessInstruction.ALIGN_FACE
        abs(frame.yaw) > STRAIGHT_ANGLE || abs(frame.roll) > STRAIGHT_ANGLE -> LivenessInstruction.LOOK_STRAIGHT
        else -> null
    }

    private fun resetState() {
        stage = LivenessStage.POSITION
        lockedTrackingId = null
        challengeStartedAt = 0L
        stableFrames = 0
        sawEyesOpen = false
        eyesClosedAt = null
        blinkCount = 0
    }

    private fun moveTo(next: LivenessStage) {
        stage = next
        stableFrames = 0
    }

    /** Drops any challenge progress; the caller emits the hint explaining why. */
    private fun restartIfInProgress() {
        if (stage == LivenessStage.POSITION) stableFrames = 0 else resetState()
    }

    private fun emit(instruction: LivenessInstruction) {
        if (instruction == lastInstruction && stage == lastStage) return
        lastInstruction = instruction
        lastStage = stage
        onStateChanged?.invoke(stage, instruction)
    }

    companion object {
        // Ratios of the analysed image. The full-screen preview (fillCenter, portrait) shows
        // roughly the middle 60% of the image width, so the 0.75-screen-width circle ≈ 0.45
        // image width — a face filling the circle sits comfortably inside MIN..MAX.
        private const val MIN_FACE_WIDTH = 0.28f
        private const val MAX_FACE_WIDTH = 0.60f
        private const val CENTER_TOLERANCE = 0.12f
        private const val STRAIGHT_ANGLE = 12f
        private const val EYE_OPEN = 0.7f
        private const val EYE_CLOSED = 0.3f
        private const val BLINK_MAX_MS = 1_500L
        private const val REQUIRED_BLINKS = 2
        private const val STABLE_FRAMES = 3
        private const val CHALLENGE_TIMEOUT_MS = 20_000L
    }
}
