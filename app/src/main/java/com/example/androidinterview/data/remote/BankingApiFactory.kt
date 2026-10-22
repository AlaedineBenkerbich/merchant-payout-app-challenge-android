package com.example.androidinterview.data.remote

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

internal val ApiJson = Json {
    // The server may add fields, and new values to enums that have an UNKNOWN member, without breaking older clients.
    ignoreUnknownKeys = true
    coerceInputValues = true
}

fun createBankingApi(client: OkHttpClient, baseUrl: HttpUrl): BankingApi =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(BankingApi::class.java)
