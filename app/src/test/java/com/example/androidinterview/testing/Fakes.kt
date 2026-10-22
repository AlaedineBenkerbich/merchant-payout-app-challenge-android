package com.example.androidinterview.testing

import com.example.androidinterview.data.local.DeviceIdStore
import com.example.androidinterview.domain.model.ActivityPage
import com.example.androidinterview.domain.model.DomainError
import com.example.androidinterview.domain.model.MerchantOverview
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.model.Payout
import com.example.androidinterview.domain.model.PayoutRequest
import com.example.androidinterview.domain.repository.DeviceIdentityRepository
import com.example.androidinterview.domain.repository.MerchantRepository
import com.example.androidinterview.domain.repository.PayoutRepository
import kotlinx.coroutines.CompletableDeferred

class FakeMerchantRepository : MerchantRepository {

    private val overviews = ArrayDeque<Outcome<MerchantOverview>>()
    private val pages = mutableMapOf<String?, ArrayDeque<Outcome<ActivityPage>>>()

    val requestedCursors = mutableListOf<String?>()
    var overviewRequests = 0
        private set

    /** While set, overview requests suspend until it is completed. */
    var overviewGate: CompletableDeferred<Unit>? = null

    /** While set, activity requests suspend until it is completed. */
    var activityGate: CompletableDeferred<Unit>? = null

    fun enqueueOverview(vararg outcomes: Outcome<MerchantOverview>) {
        overviews += outcomes
    }

    fun enqueuePage(cursor: String?, vararg outcomes: Outcome<ActivityPage>) {
        pages.getOrPut(cursor) { ArrayDeque() } += outcomes
    }

    override suspend fun getOverview(): Outcome<MerchantOverview> {
        overviewRequests++
        overviewGate?.await()
        return overviews.removeFirstOrNull() ?: Outcome.Failure(DomainError.Unexpected)
    }

    override suspend fun getActivityPage(cursor: String?, limit: Int): Outcome<ActivityPage> {
        requestedCursors += cursor
        activityGate?.await()
        return pages[cursor]?.removeFirstOrNull() ?: Outcome.Failure(DomainError.Unexpected)
    }
}

class FakePayoutRepository : PayoutRepository {

    var createOutcome: Outcome<Payout> = Outcome.Success(payout())
    val createdRequests = mutableListOf<PayoutRequest>()

    /** While set, `createPayout` suspends until it is completed. */
    var createGate: CompletableDeferred<Unit>? = null

    /** Answers of successive `getPayout` calls; the last one is repeated once the queue is exhausted. */
    val getOutcomes = ArrayDeque<Outcome<Payout>>()
    var getRequests = 0
        private set

    override suspend fun createPayout(request: PayoutRequest): Outcome<Payout> {
        createdRequests += request
        createGate?.await()
        return createOutcome
    }

    override suspend fun getPayout(id: String): Outcome<Payout> {
        getRequests++
        return if (getOutcomes.size > 1) getOutcomes.removeFirst() else getOutcomes.first()
    }
}

class FakeDeviceIdentityRepository(
    var outcome: Outcome<String> = Outcome.Success("device_test"),
) : DeviceIdentityRepository {

    override suspend fun getDeviceId(): Outcome<String> = outcome
}

class FakeDeviceIdStore(var stored: String? = null) : DeviceIdStore {

    override fun read(): String? = stored

    override fun write(deviceId: String) {
        stored = deviceId
    }
}
