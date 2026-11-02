package com.shreefintech.paytouchconsumer.kyc.identity.model

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
