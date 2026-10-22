package com.example.androidinterview.di

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.androidinterview.data.local.SharedPreferencesDeviceIdStore
import com.example.androidinterview.data.remote.BankingApi
import com.example.androidinterview.data.remote.BaseUrlInterceptor
import com.example.androidinterview.data.remote.createBankingApi
import com.example.androidinterview.data.repository.DeviceIdentityRepositoryImpl
import com.example.androidinterview.data.repository.MerchantRepositoryImpl
import com.example.androidinterview.data.repository.PayoutRepositoryImpl
import com.example.androidinterview.domain.repository.DeviceIdentityRepository
import com.example.androidinterview.domain.repository.MerchantRepository
import com.example.androidinterview.domain.repository.PayoutRepository
import com.example.androidinterview.domain.security.BiometricPolicy
import com.example.androidinterview.domain.usecase.GetActivityPageUseCase
import com.example.androidinterview.domain.usecase.GetMerchantOverviewUseCase
import com.example.androidinterview.domain.usecase.GroupActivityByDateUseCase
import com.example.androidinterview.domain.usecase.SubmitPayoutUseCase
import com.example.androidinterview.domain.usecase.TrackPayoutStatusUseCase
import com.example.androidinterview.domain.usecase.ValidatePayoutDraftUseCase
import com.example.androidinterview.ui.activity.ActivityListViewModel
import com.example.androidinterview.ui.home.HomeViewModel
import com.example.androidinterview.ui.payout.PayoutViewModel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

/**
 * The dependency graph, wired by hand: constructor injection is all this app needs, so a DI
 * framework would only add build time and indirection.
 */
class AppContainer(application: Application) {

    private val mockServerBaseUrl = CompletableFuture<HttpUrl>()

    /** Called by `InterviewApplication` once the mock server is listening. */
    fun onMockServerStarted(baseUrl: String) {
        mockServerBaseUrl.complete(baseUrl.toHttpUrl())
    }

    private val api: BankingApi = createBankingApi(
        client = OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor(mockServerBaseUrl))
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build(),
        baseUrl = PLACEHOLDER_BASE_URL.toHttpUrl(),
    )

    private val merchantRepository: MerchantRepository = MerchantRepositoryImpl(api)
    private val payoutRepository: PayoutRepository = PayoutRepositoryImpl(api)
    private val deviceIdentityRepository: DeviceIdentityRepository =
        DeviceIdentityRepositoryImpl(api, SharedPreferencesDeviceIdStore(application))

    val viewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            HomeViewModel(getMerchantOverview = GetMerchantOverviewUseCase(merchantRepository))
        }
        initializer {
            ActivityListViewModel(
                getActivityPage = GetActivityPageUseCase(merchantRepository),
                groupByDate = GroupActivityByDateUseCase(),
            )
        }
        initializer {
            PayoutViewModel(
                savedStateHandle = createSavedStateHandle(),
                validateDraft = ValidatePayoutDraftUseCase(),
                biometricPolicy = BiometricPolicy(),
                submitPayout = SubmitPayoutUseCase(deviceIdentityRepository, payoutRepository),
                trackPayoutStatus = TrackPayoutStatusUseCase(payoutRepository),
            )
        }
    }

    private companion object {
        const val TIMEOUT_SECONDS = 15L

        // Never contacted: BaseUrlInterceptor swaps in the real address of the mock server.
        const val PLACEHOLDER_BASE_URL = "http://mock-server.invalid/"
    }
}
