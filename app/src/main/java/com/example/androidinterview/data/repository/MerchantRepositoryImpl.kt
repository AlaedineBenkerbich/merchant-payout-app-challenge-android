package com.example.androidinterview.data.repository

import com.example.androidinterview.data.mapper.toDomain
import com.example.androidinterview.data.remote.BankingApi
import com.example.androidinterview.data.remote.apiCall
import com.example.androidinterview.domain.model.ActivityPage
import com.example.androidinterview.domain.model.MerchantOverview
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.repository.MerchantRepository

class MerchantRepositoryImpl(private val api: BankingApi) : MerchantRepository {

    override suspend fun getOverview(): Outcome<MerchantOverview> =
        apiCall { api.getMerchant().toDomain() }

    override suspend fun getActivityPage(cursor: String?, limit: Int): Outcome<ActivityPage> =
        apiCall { api.getActivity(cursor = cursor, limit = limit).toDomain() }
}
