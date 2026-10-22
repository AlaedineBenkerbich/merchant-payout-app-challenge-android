package com.example.androidinterview.data.local

import android.content.Context
import androidx.core.content.edit

/** Remembers the device identifier between launches. */
interface DeviceIdStore {
    fun read(): String?

    fun write(deviceId: String)
}

/**
 * Backed by a private preferences file. The file is excluded from backups and device transfers
 * (see `data_extraction_rules.xml`), so the identifier never follows the data to another device.
 */
class SharedPreferencesDeviceIdStore(context: Context) : DeviceIdStore {

    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun read(): String? = preferences.getString(KEY_DEVICE_ID, null)

    override fun write(deviceId: String) {
        preferences.edit { putString(KEY_DEVICE_ID, deviceId) }
    }

    companion object {
        const val FILE_NAME = "device_identity"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
