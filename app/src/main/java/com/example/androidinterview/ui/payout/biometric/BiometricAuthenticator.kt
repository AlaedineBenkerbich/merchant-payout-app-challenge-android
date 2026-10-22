package com.example.androidinterview.ui.payout.biometric

/** Asks the user to prove who they are with a strong biometric (fingerprint, face). */
interface BiometricAuthenticator {
    suspend fun authenticate(): BiometricResult
}

sealed interface BiometricResult {
    data object Success : BiometricResult

    /** The user dismissed the prompt, or the system cancelled it (e.g. the app went to the background). */
    data object Cancelled : BiometricResult

    /** The device supports biometrics but none is enrolled: the user must set one up in Settings. */
    data object NotEnrolled : BiometricResult

    data object LockedOut : BiometricResult

    /** No suitable sensor, or it cannot be used right now. */
    data object Unavailable : BiometricResult
}
