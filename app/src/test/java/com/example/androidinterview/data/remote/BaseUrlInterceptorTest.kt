package com.example.androidinterview.data.remote

import java.io.IOException
import java.util.concurrent.CompletableFuture
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class BaseUrlInterceptorTest {

    private val server = MockWebServer()

    @BeforeTest
    fun setUp() {
        server.start()
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    private fun clientWith(interceptor: BaseUrlInterceptor) =
        OkHttpClient.Builder().addInterceptor(interceptor).build()

    private val placeholderRequest = Request.Builder().url("http://placeholder.invalid/api/merchant?limit=15").build()

    @Test
    fun `should retarget the request to the mock server when its url is known`() {
        // Given
        server.enqueue(MockResponse().setBody("ok"))
        val baseUrl = CompletableFuture<HttpUrl>().apply { complete(server.url("/")) }
        val client = clientWith(BaseUrlInterceptor(baseUrl))

        // When
        val body = client.newCall(placeholderRequest).execute().use { it.body?.string() }

        // Then
        assertEquals("ok", body)
        assertEquals("/api/merchant?limit=15", server.takeRequest().path)
    }

    @Test
    fun `should wait for the mock server when its url is published after the request starts`() {
        // Given
        server.enqueue(MockResponse().setBody("late"))
        val baseUrl = CompletableFuture<HttpUrl>()
        val client = clientWith(BaseUrlInterceptor(baseUrl))
        Thread { Thread.sleep(100); baseUrl.complete(server.url("/")) }.start()

        // When
        val body = client.newCall(placeholderRequest).execute().use { it.body?.string() }

        // Then
        assertEquals("late", body)
    }

    @Test
    fun `should fail with an io exception when the mock server never starts`() {
        // Given
        val client = clientWith(BaseUrlInterceptor(CompletableFuture(), timeoutMillis = 50))

        // When / Then
        assertFailsWith<IOException> { client.newCall(placeholderRequest).execute() }
    }
}
