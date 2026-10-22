package com.example.androidinterview.data.repository

import com.example.androidinterview.data.local.DeviceIdStore
import com.example.androidinterview.data.remote.BankingApi
import com.example.androidinterview.data.remote.apiCall
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.repository.DeviceIdentityRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The API hands out the identifier of this device. It is kept locally after the first successful
 * call so the identity stays stable across launches, even when the server is unreachable later.
 */
class DeviceIdentityRepositoryImpl(
    private val api: BankingApi,
    private val store: DeviceIdStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DeviceIdentityRepository {

    // Concurrent first calls must not each fetch (and possibly store) a different identifier.
    private val mutex = Mutex()

    override suspend fun getDeviceId(): Outcome<String> = mutex.withLock {
        // Preferences are read from disk the first time, which must not happen on the main thread.
        val stored = withContext(ioDispatcher) { store.read() }
        if (stored != null) {
            Outcome.Success(stored)
        } else {
            fetchAndStore()
        }
    }

    private suspend fun fetchAndStore(): Outcome<String> {
        val outcome = apiCall { api.getDevice().deviceId }
        if (outcome is Outcome.Success) withContext(ioDispatcher) { store.write(outcome.value) }
        return outcome
    }
}
