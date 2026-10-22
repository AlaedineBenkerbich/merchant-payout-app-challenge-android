package com.example.androidinterview.ui.payout.biometric

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/** What the system prompt says; supplied by the UI so that it stays translatable. */
data class BiometricPromptTexts(
    val title: String,
    val subtitle: String,
    val negativeButton: String,
)

/**
 * Shows the system biometric prompt. Only a strong biometric is accepted: the weaker class
 * (e.g. 2D face unlock) is not secure enough to authorise a transfer of money.
 */
class AndroidBiometricAuthenticator(
    private val activity: FragmentActivity,
    private val texts: BiometricPromptTexts,
) : BiometricAuthenticator {

    override suspend fun authenticate(): BiometricResult {
        when (BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Unit
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> return BiometricResult.NotEnrolled
            else -> return BiometricResult.Unavailable
        }

        return suspendCancellableCoroutine { continuation ->
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                ResultCallback(continuation),
            )
            // If the flow is left (e.g. the payout screen is closed) the prompt must not stay on screen.
            // A rotation also cancels this coroutine, but must not dismiss the prompt: the recreated
            // activity attaches a new callback to the same prompt, which then reports the result.
            continuation.invokeOnCancellation {
                if (!activity.isChangingConfigurations) prompt.cancelAuthentication()
            }
            prompt.authenticate(promptInfo())
        }
    }

    private fun promptInfo(): BiometricPrompt.PromptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(texts.title)
        .setSubtitle(texts.subtitle)
        .setNegativeButtonText(texts.negativeButton)
        .setAllowedAuthenticators(BIOMETRIC_STRONG)
        .build()

    /**
     * A nested class on purpose: the library keeps the last callback it was given even after the
     * prompt is over, so an anonymous object here would pin this authenticator, and through it
     * the activity, after a rotation. This one holds only the continuation, and lets go of it
     * as soon as the prompt has reported.
     */
    private class ResultCallback(
        continuation: CancellableContinuation<BiometricResult>,
    ) : BiometricPrompt.AuthenticationCallback() {

        private var pending: CancellableContinuation<BiometricResult>? = continuation

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            report(BiometricResult.Success)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            report(errorCode.toBiometricResult())
        }

        // A fingerprint that did not match: the prompt stays open so the user can retry.
        override fun onAuthenticationFailed() = Unit

        private fun report(result: BiometricResult) {
            pending?.takeIf { it.isActive }?.resume(result)
            pending = null
        }
    }
}

private fun Int.toBiometricResult(): BiometricResult = when (this) {
    BiometricPrompt.ERROR_USER_CANCELED,
    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
    BiometricPrompt.ERROR_CANCELED,
    BiometricPrompt.ERROR_TIMEOUT,
    -> BiometricResult.Cancelled

    BiometricPrompt.ERROR_LOCKOUT,
    BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
    -> BiometricResult.LockedOut

    BiometricPrompt.ERROR_NO_BIOMETRICS -> BiometricResult.NotEnrolled

    else -> BiometricResult.Unavailable
}
