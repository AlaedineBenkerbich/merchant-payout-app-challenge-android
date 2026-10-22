package com.example.androidinterview.ui.payout

import android.app.Activity
import android.view.WindowManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleStartEffect
import com.example.androidinterview.R
import com.example.androidinterview.ui.common.findActivity

/**
 * Calls [onScreenshotTaken] whenever the user takes a screenshot while this is on screen.
 *
 * The callback only fires while the activity is visible, so it is registered when the activity
 * starts and removed when it stops (or when the payout screen leaves the composition): the
 * warning is scoped to the payout flow and never shows on the home screen.
 */
@Composable
internal fun ScreenshotDetectionEffect(onScreenshotTaken: () -> Unit) {
    val activity = LocalContext.current.findActivity()
    val latestOnScreenshotTaken by rememberUpdatedState(onScreenshotTaken)

    LifecycleStartEffect(activity) {
        val callback = Activity.ScreenCaptureCallback { latestOnScreenshotTaken() }
        activity.registerScreenCaptureCallback(activity.mainExecutor, callback)
        onStopOrDispose { activity.unregisterScreenCaptureCallback(callback) }
    }
}

/**
 * Marks the window as secure while the payout screen is shown, so that the system refuses to
 * capture it: screenshots and screen recordings come out black, and so does the Recents thumbnail.
 * The flag is scoped to this screen and removed as soon as it leaves the composition.
 */
@Composable
internal fun SecureWindowEffect() {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

@Composable
internal fun ScreenshotWarningDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.screenshot_warning_title)) },
        text = { Text(text = stringResource(R.string.screenshot_warning_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.screenshot_warning_ok))
            }
        },
    )
}
