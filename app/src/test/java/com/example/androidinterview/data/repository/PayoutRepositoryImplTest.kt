package com.example.androidinterview.data.repository

import com.example.androidinterview.data.remote.createBankingApi
import com.example.androidinterview.domain.model.DomainError
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.model.Payout
import com.example.androidinterview.domain.model.PayoutRequest
import com.example.androidinterview.domain.model.PayoutStatus
import com.example.androidinterview.testing.VALID_IBAN
import com.example.androidinterview.testing.eur
import com.example.androidinterview.testing.gbp
import com.example.androidinterview.testing.validIban
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class PayoutRepositoryImplTest {

    private val server = MockWebServer()
    private lateinit var repository: PayoutRepositoryImpl

    @BeforeTest
    fun setUp() {
        server.start()
        repository = PayoutRepositoryImpl(createBankingApi(OkHttpClient(), server.url("/")))
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    private fun request(amount: Long = 40_000) =
        PayoutRequest(amount = gbp(amount), iban = validIban(), deviceId = "device_123")

    private fun enqueueJson(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body).addHeader("Content-Type", "application/json"))
    }

    @Test
    fun `should map the created payout when the api accepts the request`() = runTest {
        // Given
        enqueueJson(
            200,
            """{"id":"pay_001","status":"pending","amount":40000,"currency":"GBP","iban":"$VALID_IBAN","created_at":"2026-10-20T20:16:10.151764Z"}""",
        )

        // When
        val outcome = repository.createPayout(request())

        // Then
        assertEquals(
            Outcome.Success(
                Payout(
                    id = "pay_001",
                    status = PayoutStatus.PENDING,
                    amount = gbp(40_000),
                    iban = VALID_IBAN,
                    createdAt = Instant.parse("2026-10-20T20:16:10.151764Z"),
                ),
            ),
            outcome,
        )
    }

    @Test
    fun `should send amount in minor units with currency iban and device id when creating a payout`() = runTest {
        // Given
        enqueueJson(
            200,
            """{"id":"pay_002","status":"pending","amount":2500,"currency":"EUR","iban":"$VALID_IBAN","created_at":"2026-10-20T20:16:10Z"}""",
        )

        // When
        repository.createPayout(PayoutRequest(eur(2_500), validIban(), "device_123"))

        // Then
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/payouts", recorded.path)
        assertEquals(
            Json.parseToJsonElement("""{"amount":2500,"currency":"EUR","iban":"$VALID_IBAN","device_id":"device_123"}"""),
            Json.parseToJsonElement(recorded.body.readUtf8()),
        )
    }

    @Test
    fun `should report service unavailable when the api answers 503`() = runTest {
        // Given
        enqueueJson(503, """{"error":"Service unavailable","code":"SERVICE_UNAVAILABLE"}""")

        // When
        val outcome = repository.createPayout(request(amount = 99_999))

        // Then
        assertEquals(Outcome.Failure(DomainError.ServiceUnavailable), outcome)
    }

    @Test
    fun `should report insufficient funds when the api answers 400 with the insufficient funds code`() = runTest {
        // Given
        enqueueJson(400, """{"error":"Insufficient funds","code":"INSUFFICIENT_FUNDS"}""")

        // When
        val outcome = repository.createPayout(request(amount = 88_888))

        // Then
        assertEquals(Outcome.Failure(DomainError.InsufficientFunds), outcome)
    }

    @Test
    fun `should report the api message when the request is rejected for another reason`() = runTest {
        // Given
        enqueueJson(422, """{"error":"Unsupported IBAN","code":"UNSUPPORTED_IBAN"}""")

        // When
        val outcome = repository.createPayout(request())

        // Then
        assertEquals(Outcome.Failure(DomainError.Rejected("Unsupported IBAN")), outcome)
    }

    @Test
    fun `should report a rejection without message when the error body is empty`() = runTest {
        // Given
        server.enqueue(MockResponse().setResponseCode(404))

        // When
        val outcome = repository.getPayout("unknown")

        // Then
        assertEquals(Outcome.Failure(DomainError.Rejected(null)), outcome)
    }

    @Test
    fun `should report no connection when the server cannot be reached`() = runTest {
        // Given
        server.shutdown()

        // When
        val outcome = repository.createPayout(request())

        // Then
        assertEquals(Outcome.Failure(DomainError.NoConnection), outcome)
    }

    @Test
    fun `should report an unexpected failure when the response body is not valid`() = runTest {
        // Given
        enqueueJson(200, """{"unexpected":"shape"}""")

        // When
        val outcome = repository.createPayout(request())

        // Then
        assertEquals(Outcome.Failure(DomainError.Unexpected), outcome)
    }

    @Test
    fun `should report an unexpected failure when the creation date is malformed`() = runTest {
        // Given
        enqueueJson(
            200,
            """{"id":"pay_003","status":"pending","amount":100,"currency":"GBP","iban":"$VALID_IBAN","created_at":"yesterday"}""",
        )

        // When
        val outcome = repository.createPayout(request())

        // Then
        assertEquals(Outcome.Failure(DomainError.Unexpected), outcome)
    }

    @Test
    fun `should map a payout status the app does not know to unknown instead of failing`() = runTest {
        // Given
        enqueueJson(
            200,
            """{"id":"pay_004","status":"on_hold","amount":100,"currency":"GBP","iban":"$VALID_IBAN","created_at":"2026-10-20T20:16:10Z"}""",
        )

        // When
        val outcome = repository.getPayout("pay_004")

        // Then
        assertEquals(PayoutStatus.UNKNOWN, (outcome as Outcome.Success).value.status)
    }

    @Test
    fun `should fetch the payout status when asked for an existing payout`() = runTest {
        // Given
        enqueueJson(
            200,
            """{"id":"pay_001","status":"completed","amount":40000,"currency":"GBP","iban":"$VALID_IBAN","created_at":"2026-10-20T20:16:10Z"}""",
        )

        // When
        val outcome = repository.getPayout("pay_001")

        // Then
        assertEquals("/api/payouts/pay_001", server.takeRequest().path)
        assertEquals(PayoutStatus.COMPLETED, (outcome as Outcome.Success).value.status)
    }
}
