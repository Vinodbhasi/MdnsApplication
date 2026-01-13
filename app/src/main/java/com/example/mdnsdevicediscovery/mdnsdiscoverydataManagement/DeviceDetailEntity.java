package com.example.mdnsdevicediscovery.mdnsdiscoverydataManagement;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "device_details")
public class DeviceDetailEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "ip_address")
    public String deviceIpAddress;

    @ColumnInfo(name = "name")
    public String deviceName;

    @ColumnInfo(name = "status")
    public boolean deviceStatus;

    @ColumnInfo(name = "lastSeen")
    public long deviceLastSeen = 0;

    public DeviceDetailEntity(String deviceName, String deviceIpAddress, boolean deviceStatus, long deviceLastSeen) {
        this.deviceName = deviceName;
        this.deviceIpAddress = deviceIpAddress;
        this.deviceStatus = deviceStatus;
        this.deviceLastSeen = deviceLastSeen;
    }
}
