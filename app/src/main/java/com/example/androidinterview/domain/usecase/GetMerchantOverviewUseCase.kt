package com.example.androidinterview.domain.usecase

import com.example.androidinterview.domain.model.MerchantOverview
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.repository.MerchantRepository

class GetMerchantOverviewUseCase(private val merchants: MerchantRepository) {

    suspend operator fun invoke(): Outcome<MerchantOverview> = merchants.getOverview()
}
