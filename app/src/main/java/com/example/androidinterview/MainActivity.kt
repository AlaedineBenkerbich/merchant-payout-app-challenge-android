package com.example.androidinterview

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.androidinterview.ui.AppRoot
import com.example.androidinterview.ui.theme.AndroidInterviewTheme

// BiometricPrompt can only be hosted by a FragmentActivity.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as InterviewApplication).container
        setContent {
            AndroidInterviewTheme {
                AppRoot(viewModelFactory = container.viewModelFactory)
            }
        }
    }
}
