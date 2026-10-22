package com.example.androidinterview

import android.app.Application
import com.example.androidinterview.di.AppContainer
import com.example.androidinterview.mock.MockServerManager
import kotlin.concurrent.thread

class InterviewApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        thread(name = "mock-server-init") {
            MockServerManager.start()
            // The server is only safe to address once start() has returned; see BaseUrlInterceptor.
            container.onMockServerStarted(MockServerManager.baseUrl)
        }
    }
}
