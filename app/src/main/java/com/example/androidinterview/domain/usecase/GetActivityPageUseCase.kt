package com.example.androidinterview.domain.usecase

import com.example.androidinterview.domain.model.ActivityPage
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.repository.MerchantRepository

class GetActivityPageUseCase(private val merchants: MerchantRepository) {

    suspend operator fun invoke(cursor: String?): Outcome<ActivityPage> =
        merchants.getActivityPage(cursor, PAGE_SIZE)

    companion object {
        const val PAGE_SIZE = 15
    }
}
