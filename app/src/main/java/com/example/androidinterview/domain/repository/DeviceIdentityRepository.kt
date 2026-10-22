package com.example.androidinterview.domain.repository

import com.example.androidinterview.domain.model.Outcome

interface DeviceIdentityRepository {
    /** The identifier of this installation; it stays the same across app restarts. */
    suspend fun getDeviceId(): Outcome<String>
}
