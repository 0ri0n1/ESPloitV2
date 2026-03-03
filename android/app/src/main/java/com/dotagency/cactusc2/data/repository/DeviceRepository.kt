package com.dotagency.cactusc2.data.repository

import com.dotagency.cactusc2.data.db.DeviceDao
import com.dotagency.cactusc2.data.db.PayloadDao
import com.dotagency.cactusc2.data.model.Device
import com.dotagency.cactusc2.data.model.Payload
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for local device and payload data.
 * Wraps [DeviceDao] and [PayloadDao] to provide a clean API to the ViewModel layer.
 * Exposes reactive [Flow]s for the UI to collect.
 */
class DeviceRepository(
    private val deviceDao: DeviceDao,
    private val payloadDao: PayloadDao
) {
    val allDevices: Flow<List<Device>> = deviceDao.getAllDevices()
    val allPayloads: Flow<List<Payload>> = payloadDao.getAllPayloads()

    suspend fun getDevice(id: Long) = deviceDao.getDevice(id)
    suspend fun addDevice(device: Device) = deviceDao.insert(device)
    suspend fun updateDevice(device: Device) = deviceDao.update(device)
    suspend fun deleteDevice(device: Device) = deviceDao.delete(device)

    suspend fun setDefaultDevice(id: Long) {
        deviceDao.clearDefault()
        deviceDao.setDefault(id)
    }

    suspend fun getPayload(id: Long) = payloadDao.getPayload(id)
    suspend fun addPayload(payload: Payload) = payloadDao.insert(payload)
    suspend fun updatePayload(payload: Payload) = payloadDao.update(payload)
    suspend fun deletePayload(payload: Payload) = payloadDao.delete(payload)
}
