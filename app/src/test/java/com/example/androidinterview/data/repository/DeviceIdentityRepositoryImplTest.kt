package com.example.androidinterview.data.repository

import com.example.androidinterview.data.remote.createBankingApi
import com.example.androidinterview.domain.model.DomainError
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.testing.FakeDeviceIdStore
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class DeviceIdentityRepositoryImplTest {

    private val server = MockWebServer()
    private val store = FakeDeviceIdStore()
    private lateinit var repository: DeviceIdentityRepositoryImpl

    @BeforeTest
    fun setUp() {
        server.start()
        repository = DeviceIdentityRepositoryImpl(createBankingApi(OkHttpClient(), server.url("/")), store)
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `should fetch and persist the device id when none is stored yet`() = runTest {
        // Given
        server.enqueue(MockResponse().setBody("""{"device_id":"device_abc"}"""))

        // When
        val outcome = repository.getDeviceId()

        // Then
        assertEquals(Outcome.Success("device_abc"), outcome)
        assertEquals("device_abc", store.stored)
    }

    @Test
    fun `should reuse the stored device id without calling the api when one already exists`() = runTest {
        // Given
        store.stored = "device_saved"

        // When
        val outcome = repository.getDeviceId()

        // Then
        assertEquals(Outcome.Success("device_saved"), outcome)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `should keep returning the same device id when it is requested several times`() = runTest {
        // Given
        server.enqueue(MockResponse().setBody("""{"device_id":"device_abc"}"""))

        // When
        val first = repository.getDeviceId()
        val second = repository.getDeviceId()

        // Then
        assertEquals(first, second)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `should fetch the device id only once when it is requested concurrently for the first time`() = runTest {
        // Given: a slow answer, so that the second caller arrives while the first is still fetching.
        server.enqueue(
            MockResponse().setBody("""{"device_id":"device_abc"}""").setBodyDelay(300, TimeUnit.MILLISECONDS),
        )

        // When
        val first = async { repository.getDeviceId() }
        val second = async { repository.getDeviceId() }

        // Then
        assertEquals(Outcome.Success("device_abc"), first.await())
        assertEquals(Outcome.Success("device_abc"), second.await())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `should not persist anything when the device request fails`() = runTest {
        // Given
        server.enqueue(MockResponse().setResponseCode(503))

        // When
        val outcome = repository.getDeviceId()

        // Then
        assertEquals(Outcome.Failure(DomainError.ServiceUnavailable), outcome)
        assertNull(store.stored)
    }
}
