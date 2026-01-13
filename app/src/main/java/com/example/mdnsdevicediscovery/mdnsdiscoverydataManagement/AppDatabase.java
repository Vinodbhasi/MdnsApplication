package com.example.mdnsdevicediscovery.mdnsdiscoverydataManagement;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {DeviceDetailEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract DeviceDetailDao deviceDetailDao();
}
