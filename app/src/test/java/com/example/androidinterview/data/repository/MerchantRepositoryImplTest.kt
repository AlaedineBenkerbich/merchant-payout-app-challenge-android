package com.example.androidinterview.data.repository

import com.example.androidinterview.data.remote.createBankingApi
import com.example.androidinterview.domain.model.ActivityItem
import com.example.androidinterview.domain.model.ActivityPage
import com.example.androidinterview.domain.model.ActivityStatus
import com.example.androidinterview.domain.model.ActivityType
import com.example.androidinterview.domain.model.DomainError
import com.example.androidinterview.domain.model.MerchantOverview
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.testing.gbp
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class MerchantRepositoryImplTest {

    private val server = MockWebServer()
    private lateinit var repository: MerchantRepositoryImpl

    @BeforeTest
    fun setUp() {
        server.start()
        repository = MerchantRepositoryImpl(createBankingApi(OkHttpClient(), server.url("/")))
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueueJson(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body).addHeader("Content-Type", "application/json"))
    }

    private val payoutActivityJson = """
        {"id":"act_001","type":"payout","amount":-12500,"currency":"GBP","date":"2026-10-20T20:16:10.151764Z","description":"Invoice payment","status":"processing"}
    """.trimIndent()

    private val payoutActivity = ActivityItem(
        id = "act_001",
        type = ActivityType.PAYOUT,
        amount = gbp(-12_500),
        occurredAt = Instant.parse("2026-10-20T20:16:10.151764Z"),
        description = "Invoice payment",
        status = ActivityStatus.PROCESSING,
    )

    @Test
    fun `should map balances and recent activity when the overview is requested`() = runTest {
        // Given
        enqueueJson(
            200,
            """{"available_balance":500000,"pending_balance":25000,"currency":"GBP","activity":[$payoutActivityJson]}""",
        )

        // When
        val outcome = repository.getOverview()

        // Then
        assertEquals(
            Outcome.Success(
                MerchantOverview(
                    availableBalance = gbp(500_000),
                    pendingBalance = gbp(25_000),
                    recentActivity = listOf(payoutActivity),
                ),
            ),
            outcome,
        )
        assertEquals("/api/merchant", server.takeRequest().path)
    }

    @Test
    fun `should request the first page without a cursor when none is given`() = runTest {
        // Given
        enqueueJson(200, """{"items":[$payoutActivityJson],"next_cursor":"act_001","has_more":true}""")

        // When
        val outcome = repository.getActivityPage(cursor = null, limit = 15)

        // Then
        assertEquals("/api/merchant/activity?limit=15", server.takeRequest().path)
        assertEquals(Outcome.Success(ActivityPage(listOf(payoutActivity), "act_001", true)), outcome)
    }

    @Test
    fun `should pass the cursor to the api when the next page is requested`() = runTest {
        // Given
        enqueueJson(200, """{"items":[],"next_cursor":null,"has_more":false}""")

        // When
        val outcome = repository.getActivityPage(cursor = "act_015", limit = 15)

        // Then
        assertEquals("/api/merchant/activity?cursor=act_015&limit=15", server.takeRequest().path)
        assertEquals(Outcome.Success(ActivityPage(emptyList(), null, false)), outcome)
    }

    @Test
    fun `should degrade a type and a status the app does not know instead of failing the page`() = runTest {
        // Given: the API starts sending values this version has never heard of.
        enqueueJson(
            200,
            """{"items":[{"id":"act_9","type":"chargeback","amount":-500,"currency":"GBP","date":"2026-10-20T10:00:00Z","description":"Disputed card payment","status":"under_review"}],"next_cursor":null,"has_more":false}""",
        )

        // When
        val outcome = repository.getActivityPage(cursor = null, limit = 15)

        // Then
        val item = (outcome as Outcome.Success).value.items.single()
        assertEquals(ActivityType.OTHER, item.type)
        assertEquals(ActivityStatus.UNKNOWN, item.status)
        assertEquals(gbp(-500), item.amount)
    }

    @Test
    fun `should still fail the page when an amount comes with a currency the app does not support`() = runTest {
        // Given: an amount cannot be shown or trusted without its currency.
        enqueueJson(
            200,
            """{"items":[{"id":"act_9","type":"deposit","amount":500,"currency":"USD","date":"2026-10-20T10:00:00Z","description":"Payment","status":"completed"}],"next_cursor":null,"has_more":false}""",
        )

        // When
        val outcome = repository.getActivityPage(cursor = null, limit = 15)

        // Then
        assertEquals(Outcome.Failure(DomainError.Unexpected), outcome)
    }

    @Test
    fun `should report service unavailable when the overview request fails on the server`() = runTest {
        // Given
        server.enqueue(MockResponse().setResponseCode(500))

        // When
        val outcome = repository.getOverview()

        // Then
        assertEquals(Outcome.Failure(DomainError.ServiceUnavailable), outcome)
    }
}
