package com.example.androidinterview.ui.payout

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.androidinterview.R
import com.example.androidinterview.ui.common.findFragmentActivity
import com.example.androidinterview.ui.payout.biometric.AndroidBiometricAuthenticator
import com.example.androidinterview.ui.payout.biometric.BiometricPromptTexts
import com.example.androidinterview.ui.payout.biometric.BiometricResult

/**
 * Shows the biometric prompt while the flow is in the [PayoutPhase.Authenticating] phase and reports
 * what happened. The view model decides what to do with the result; it never touches the activity.
 */
@Composable
internal fun BiometricPromptEffect(phase: PayoutPhase, onResult: (BiometricResult) -> Unit) {
    if (phase !is PayoutPhase.Authenticating) return

    val activity = LocalContext.current.findFragmentActivity()
    val texts = BiometricPromptTexts(
        title = stringResource(R.string.biometric_prompt_title),
        subtitle = stringResource(R.string.biometric_prompt_subtitle),
        negativeButton = stringResource(R.string.biometric_prompt_negative),
    )
    val latestOnResult by rememberUpdatedState(onResult)

    // Keyed on the phase: a new activity instance (rotation) starts a new composition and so
    // re-attaches to the prompt, while leaving the phase cancels the prompt.
    LaunchedEffect(phase) {
        latestOnResult(AndroidBiometricAuthenticator(activity, texts).authenticate())
    }
}

/** Sends the merchant to the system screen where a fingerprint can be enrolled. */
internal fun Context.openBiometricSettings() {
    val enrol = Intent(Settings.ACTION_BIOMETRIC_ENROLL)
        .putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG)
    try {
        startActivity(enrol)
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
    }
}
