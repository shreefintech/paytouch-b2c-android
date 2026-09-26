package com.shreefintech.paytouchconsumer.kyc.identity

import com.shreefintech.paytouchconsumer.kyc.identity.model.LivenessFrameItem
import com.shreefintech.paytouchconsumer.kyc.identity.model.LivenessInstruction
import com.shreefintech.paytouchconsumer.kyc.identity.model.LivenessStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class LivenessChallengeHelperTest {

    private lateinit var helper: LivenessChallengeHelper
    private val states = mutableListOf<Pair<LivenessStage, LivenessInstruction>>()
    private var passedCount = 0
    private var timeoutCount = 0
    private var clock = 0L

    @Before
    fun setUp() {
        helper = LivenessChallengeHelper()
        helper.onStateChanged = { stage, instruction -> states += stage to instruction }
        helper.onPassed = { passedCount++ }
        helper.onTimeout = { timeoutCount++ }
        helper.reset()
        clock = 0L
    }

    private fun frame(
        faceCount: Int = 1,
        trackingId: Int? = TRACKING_ID,
        centerX: Float = 0.5f,
        centerY: Float = 0.5f,
        width: Float = 0.4f,
        yaw: Float = 0f,
        eyesOpen: Float = OPEN,
        stepMs: Long = 100L
    ): LivenessFrameItem {
        clock += stepMs
        return LivenessFrameItem(
            faceCount = faceCount, trackingId = trackingId, centerXRatio = centerX, centerYRatio = centerY,
            widthRatio = width, yaw = yaw, roll = 0f, leftEyeOpen = eyesOpen, rightEyeOpen = eyesOpen,
            timestampMs = clock
        )
    }

    private fun feed(vararg frames: LivenessFrameItem) = frames.forEach { helper.onFrame(it) }

    private fun reachBlinkStage() = feed(frame(), frame(), frame())

    private fun blink() = feed(frame(eyesOpen = OPEN), frame(eyesOpen = CLOSED), frame(eyesOpen = OPEN))

    private fun lastState() = states.last()

    @Test
    fun reset_emitsAlignFace() {
        assertEquals(LivenessStage.POSITION to LivenessInstruction.ALIGN_FACE, lastState())
    }

    @Test
    fun fullChallenge_passesOnce() {
        reachBlinkStage()
        assertEquals(LivenessStage.BLINK to LivenessInstruction.BLINK, lastState())

        blink()
        assertEquals(LivenessStage.BLINK to LivenessInstruction.BLINK_AGAIN, lastState())

        feed(frame(eyesOpen = CLOSED), frame(eyesOpen = OPEN))
        assertEquals(LivenessStage.HOLD to LivenessInstruction.HOLD_STILL, lastState())

        feed(frame(), frame(), frame())
        assertEquals(LivenessStage.DONE to LivenessInstruction.CAPTURING, lastState())
        assertEquals(1, passedCount)

        // Frames after DONE are ignored.
        feed(frame(), frame())
        assertEquals(1, passedCount)
    }

    @Test
    fun eyesClosedTooLong_doesNotCountAsBlink() {
        reachBlinkStage()
        feed(
            frame(eyesOpen = OPEN),
            frame(eyesOpen = CLOSED),
            frame(eyesOpen = CLOSED, stepMs = 1_600L),
            frame(eyesOpen = OPEN)
        )
        assertEquals(LivenessStage.BLINK to LivenessInstruction.BLINK, lastState())
    }

    @Test
    fun closedWithoutPriorOpen_doesNotCountAsBlink() {
        reachBlinkStage()
        feed(frame(eyesOpen = CLOSED), frame(eyesOpen = OPEN))
        assertEquals(LivenessStage.BLINK to LivenessInstruction.BLINK, lastState())
    }

    @Test
    fun trackingIdChange_restartsChallenge() {
        reachBlinkStage()
        blink()
        feed(frame(trackingId = TRACKING_ID + 1))
        assertEquals(LivenessStage.POSITION to LivenessInstruction.ALIGN_FACE, lastState())
    }

    @Test
    fun secondFace_restartsChallenge() {
        reachBlinkStage()
        feed(frame(faceCount = 2))
        assertEquals(LivenessStage.POSITION to LivenessInstruction.SINGLE_FACE_ONLY, lastState())
    }

    @Test
    fun lostFace_restartsChallenge() {
        reachBlinkStage()
        feed(frame(faceCount = 0))
        assertEquals(LivenessStage.POSITION to LivenessInstruction.ALIGN_FACE, lastState())
    }

    @Test
    fun challengeTimeout_resetsAndNotifies() {
        reachBlinkStage()
        feed(frame(stepMs = 20_001L))
        assertEquals(1, timeoutCount)
        assertEquals(LivenessStage.POSITION to LivenessInstruction.ALIGN_FACE, lastState())
    }

    @Test
    fun positionHints_areEmitted() {
        feed(frame(width = 0.1f))
        assertEquals(LivenessInstruction.MOVE_CLOSER, lastState().second)
        feed(frame(width = 0.7f))
        assertEquals(LivenessInstruction.MOVE_BACK, lastState().second)
        feed(frame(centerX = 0.8f))
        assertEquals(LivenessInstruction.ALIGN_FACE, lastState().second)
        feed(frame(yaw = 30f))
        assertEquals(LivenessInstruction.LOOK_STRAIGHT, lastState().second)
    }

    @Test
    fun positionHint_resetsStableFrameCount() {
        feed(frame(), frame(), frame(centerX = 0.8f), frame(), frame())
        assertEquals(LivenessStage.POSITION, lastState().first)
    }

    @Test
    fun holdWithEyesClosed_doesNotPass() {
        reachBlinkStage()
        blink()
        feed(frame(eyesOpen = CLOSED), frame(eyesOpen = OPEN))
        feed(frame(), frame(), frame(eyesOpen = CLOSED), frame())
        assertFalse(passedCount > 0)
    }

    private companion object {
        const val TRACKING_ID = 7
        const val OPEN = 0.9f
        const val CLOSED = 0.1f
    }
}
