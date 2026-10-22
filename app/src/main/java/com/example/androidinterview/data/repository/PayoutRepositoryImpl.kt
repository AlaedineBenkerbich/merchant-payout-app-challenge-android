package com.example.androidinterview.data.repository

import com.example.androidinterview.data.mapper.toDomain
import com.example.androidinterview.data.mapper.toDto
import com.example.androidinterview.data.remote.BankingApi
import com.example.androidinterview.data.remote.apiCall
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.model.Payout
import com.example.androidinterview.domain.model.PayoutRequest
import com.example.androidinterview.domain.repository.PayoutRepository

class PayoutRepositoryImpl(private val api: BankingApi) : PayoutRepository {

    override suspend fun createPayout(request: PayoutRequest): Outcome<Payout> =
        apiCall { api.createPayout(request.toDto()).toDomain() }

    override suspend fun getPayout(id: String): Outcome<Payout> =
        apiCall { api.getPayout(id).toDomain() }
}
