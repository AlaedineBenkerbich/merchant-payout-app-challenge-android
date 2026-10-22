package com.example.androidinterview.data.remote

import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Points every request at the local mock server once it is listening.
 *
 * The mock server is started on a background thread at launch, and reading its URL before it is
 * running would start it a second time with the wrong dispatcher. Requests are therefore built
 * against a placeholder host and re-targeted here, on OkHttp's own threads, after waiting for it.
 */
class BaseUrlInterceptor(
    private val baseUrl: CompletableFuture<HttpUrl>,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val target = awaitBaseUrl()
        val retargeted = chain.request().url.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .build()
        return chain.proceed(chain.request().newBuilder().url(retargeted).build())
    }

    private fun awaitBaseUrl(): HttpUrl = try {
        baseUrl.get(timeoutMillis, TimeUnit.MILLISECONDS)
    } catch (exception: TimeoutException) {
        throw IOException("The mock server did not start in time", exception)
    } catch (exception: ExecutionException) {
        throw IOException("The mock server failed to start", exception)
    }

    companion object {
        private const val DEFAULT_TIMEOUT_MILLIS = 5_000L
    }
}
