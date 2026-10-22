package com.example.androidinterview.domain.repository

import com.example.androidinterview.domain.model.ActivityPage
import com.example.androidinterview.domain.model.MerchantOverview
import com.example.androidinterview.domain.model.Outcome

interface MerchantRepository {
    suspend fun getOverview(): Outcome<MerchantOverview>

    /** Returns the page following [cursor], or the first page when [cursor] is null. */
    suspend fun getActivityPage(cursor: String?, limit: Int): Outcome<ActivityPage>
}
