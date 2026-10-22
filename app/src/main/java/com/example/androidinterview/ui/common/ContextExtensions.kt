package com.example.androidinterview.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/** Compose hands out wrapped contexts; this walks back to the hosting activity. */
tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("No activity found in the context chain of $this")
}

/** `BiometricPrompt` can only be hosted by a [FragmentActivity]. */
fun Context.findFragmentActivity(): FragmentActivity = findActivity() as FragmentActivity
