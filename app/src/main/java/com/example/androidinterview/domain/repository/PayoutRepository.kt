package com.example.androidinterview.domain.repository

import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.model.Payout
import com.example.androidinterview.domain.model.PayoutRequest

interface PayoutRepository {
    suspend fun createPayout(request: PayoutRequest): Outcome<Payout>

    suspend fun getPayout(id: String): Outcome<Payout>
}
