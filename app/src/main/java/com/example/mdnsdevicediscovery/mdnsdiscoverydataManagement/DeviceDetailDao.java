package com.example.mdnsdevicediscovery.mdnsdiscoverydataManagement;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.ArrayList;
import java.util.List;

@Dao
public interface DeviceDetailDao {

    // NEW: Single device insertOrUpdate for NsdManager onServiceResolved()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(DeviceDetailEntity device);

    // Scenario 1 & 2: Insert new devices OR update existing (batch upsert)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateBatch(List<DeviceDetailEntity> devices);

    // Scenario 3: Update specific device by IP (name change + status)
    @Query("UPDATE device_details SET name = :name, status = :online, lastSeen = :lastSeen WHERE ip_address = :ip")
    int updateDeviceByIp(String ip, String name, boolean online, long lastSeen);

    // Scenario 4 & 5: Mark ALL non-discovered devices as offline
    @Query("UPDATE device_details SET status = 0, lastSeen = :timestamp WHERE ip_address NOT IN (:discoveredIps)")
    int markOfflineExcept(List<String> discoveredIps, long timestamp);

    // Get all devices for RecyclerView
    @Query("SELECT * FROM device_details ORDER BY name ASC, lastSeen DESC")
    List<DeviceDetailEntity> getAllDevices();

    // Get device by IP
    @Query("SELECT * FROM device_details WHERE ip_address = :ip LIMIT 1")
    DeviceDetailEntity getDeviceByIp(String ip);

    // Cleanup old offline devices (>24h)
    @Query("DELETE FROM device_details WHERE status = 0 AND lastSeen < :threshold")
    int cleanupOldOffline(long threshold);

    // COMPLETE TRANSACTION METHOD - Handles ALL Scenarios
    @Transaction
    default void handleDiscoveryBatch(List<DeviceDetailEntity> discoveredDevices) {
        if (discoveredDevices == null || discoveredDevices.isEmpty()) return;

        // Extract discovered IPs
        List<String> discoveredIps = new ArrayList<>();
        for (DeviceDetailEntity device : discoveredDevices) {
            discoveredIps.add(device.deviceIpAddress);
        }

        long now = System.currentTimeMillis();

        // 1. Mark ALL non-discovered devices offline (Scenario 4)
        markOfflineExcept(discoveredIps, now);

        // 2. Insert new OR update existing (Scenarios 1,2,3)
        insertOrUpdateBatch(discoveredDevices);
    }

}
