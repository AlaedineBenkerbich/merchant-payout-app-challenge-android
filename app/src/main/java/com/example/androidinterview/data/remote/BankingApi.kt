package com.example.androidinterview.data.remote

import com.example.androidinterview.data.remote.dto.CreatePayoutRequestDto
import com.example.androidinterview.data.remote.dto.DeviceResponseDto
import com.example.androidinterview.data.remote.dto.MerchantDataResponseDto
import com.example.androidinterview.data.remote.dto.PaginatedActivityResponseDto
import com.example.androidinterview.data.remote.dto.PayoutResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BankingApi {

    @GET("api/merchant")
    suspend fun getMerchant(): MerchantDataResponseDto

    @GET("api/merchant/activity")
    suspend fun getActivity(
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
    ): PaginatedActivityResponseDto

    @POST("api/payouts")
    suspend fun createPayout(@Body request: CreatePayoutRequestDto): PayoutResponseDto

    @GET("api/payouts/{id}")
    suspend fun getPayout(@Path("id") id: String): PayoutResponseDto

    @GET("api/devices")
    suspend fun getDevice(): DeviceResponseDto
}
