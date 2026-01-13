package com.example.mdnsdevicediscovery;

import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import com.example.mdnsdevicediscovery.mdnsdiscoverydataManagement.AppDatabase;
import com.example.mdnsdevicediscovery.mdnsdiscoverydataManagement.DeviceDetail;
import com.example.mdnsdevicediscovery.mdnsdiscoverydataManagement.DeviceDetailAdapter;
import com.example.mdnsdevicediscovery.mdnsdiscoverydataManagement.DeviceDetailEntity;
import com.example.mdnsdevicediscovery.mdnsdiscoverydataManagement.DeviceDetailDao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements DeviceDetailAdapter.OnDeviceClickListener {

    private List<DeviceDetail> deviceList = new ArrayList<>();
    private DeviceDetailAdapter adapter;
    private DeviceDetailDao deviceDetailDao;
    private NsdManager nsdManager;
    private List<String> serviceTypes = Arrays.asList(
            "_airplay._tcp",        // ✅ Apple TV, AirPlay (mDNS)
            "_raop._tcp",           // ✅ AirPlay Audio (mDNS)
            "_ipp._tcp",            // ✅ Printers/Scanners (mDNS)
            "_ipps._tcp",           // ✅ Secure printers (mDNS)
            "_http._tcp",           // ✅ Smart devices, routers (mDNS)
            "_googlecast._tcp",     // ✅ Chromecast (mDNS)
            "_workstation._tcp",    // ✅ Windows PCs (mDNS)
            "_smb._tcp",            // ✅ File servers/NAS (mDNS)
            "_ssh._tcp"             // ✅ Linux/Mac servers (mDNS)
    );
    private static final String TAG = "HomeActivity";
    private List<String> currentDiscoveryIps = new ArrayList<>();  // Track current scan
    private List<NsdManager.DiscoveryListener> activeListeners = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Log.i(TAG, "Reached");
        initDatabase();
        setupRecyclerView();
        loadCachedDevices();  // Load from SQLite on app start
        startDiscovery();  // Begin mDNS scanning
    }

    private void initDatabase() {
        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "mDNS-device-discovery").allowMainThreadQueries().build();
        deviceDetailDao = db.deviceDetailDao();
    }

    private void setupRecyclerView() {
        List<DeviceDetailEntity> deviceDetailsList = deviceDetailDao.getAllDevices();
        for (DeviceDetailEntity entity : deviceDetailsList) {
            deviceList.add(new DeviceDetail(entity.deviceName, entity.deviceIpAddress, entity.deviceStatus));
        }
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeviceDetailAdapter(this);
        recyclerView.setAdapter(adapter);
        adapter.updateDevices(deviceList);
    }

    private NsdManager.DiscoveryListener createDiscoveryListener(final String serviceType) {
        return new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "Discovery started: " + serviceType);
                currentDiscoveryIps.clear();  // Reset current scan
                runOnUiThread(() -> adapter.updateDevices(new ArrayList<>()));  // Clear UI
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // RESOLVE service to get IP address
//                nsdManager.resolveService(service, new ResolveListener());
                Log.d(TAG, "Service found: "+ serviceType + ": "  + service.getServiceName());
                // NEW ResolveListener per service (already fixed)
                NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        handleServiceResolved(serviceInfo);
                    }
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        Log.e(TAG, "Resolve failed: " + errorCode);
                    }
                };
                nsdManager.resolveService(service, resolveListener);
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.d(TAG, "Service lost: " + serviceType + ": " + service.getServiceName());
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "Discovery stopped: " + serviceType + ": " + serviceType);
                // Scenario 4: Mark non-discovered devices offline
                processDiscoverySession();
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: " + serviceType + ": " + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Stop discovery failed: " + serviceType + ": " + errorCode);
            }
        };
    }

    private void handleServiceResolved(NsdServiceInfo serviceInfo) {
        String name = serviceInfo.getServiceName();
        String ip = serviceInfo.getHost().getHostAddress();

        Log.d(TAG, "Resolved: " + name + " @ " + ip);

        // Create DeviceEntity and save to DB
        DeviceDetailEntity device = new DeviceDetailEntity(name, ip, true, System.currentTimeMillis());
        deviceDetailDao.insertOrUpdate(device);

        // Track for offline detection
        currentDiscoveryIps.add(ip);

        // Update UI immediately
        refreshDisplay();
    }


    private class ResolveListener implements NsdManager.ResolveListener {
        @Override
        public void onResolveFailed(NsdServiceInfo service, int errorCode) {
            Log.e(TAG, "Resolve failed: " + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo service) {
            String name = service.getServiceName();
            String ip = service.getHost().getHostAddress();

            Log.d(TAG, "Resolved: " + name + " @ " + ip);

            DeviceDetailEntity device = new DeviceDetailEntity(name, ip, true, System.currentTimeMillis());
            // IMMEDIATE insert/update (optimistic)
            deviceDetailDao.insertOrUpdate(device);
            // Add to current discovery batch
            currentDiscoveryIps.add(ip);
            refreshDisplay();
        }

    }

    private void processDiscoverySession() {
        // Scenario 4: Mark ALL devices not in this discovery as OFFLINE
        long now = System.currentTimeMillis();
        deviceDetailDao.markOfflineExcept(currentDiscoveryIps, now);

        refreshDisplay();
        Log.d(TAG, "Discovery session complete. Processed " + currentDiscoveryIps.size() + " devices");
    }

    private void startDiscovery() {
        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        for (String serviceType : serviceTypes) {
            NsdManager.DiscoveryListener listener = createDiscoveryListener(serviceType);
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener);
            Log.d(TAG, "Started discovery for: " + serviceType);
        }
    }

    private void loadCachedDevices() {
        List<DeviceDetailEntity> entities = deviceDetailDao.getAllDevices();
        for (DeviceDetailEntity entity : entities) {
            deviceList.add(new DeviceDetail(entity.deviceName, entity.deviceIpAddress, entity.deviceStatus));
        }
        adapter.updateDevices(deviceList);
    }

    private void refreshDisplay() {
        runOnUiThread(() -> {
            deviceList.clear();
            List<DeviceDetailEntity> entities = deviceDetailDao.getAllDevices();
            for (DeviceDetailEntity entity : entities) {
                deviceList.add(new DeviceDetail(entity.deviceName, entity.deviceIpAddress, entity.deviceStatus));
            }
            adapter.updateDevices(deviceList);
        });
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (nsdManager != null) {
//            nsdManager.stopServiceDiscovery(discoveryListener);
//        }
//    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAllDiscovery();
    }

    private void stopAllDiscovery() {
        for (NsdManager.DiscoveryListener listener : activeListeners) {
            try {
                nsdManager.stopServiceDiscovery(listener);
            } catch (Exception e) {
                Log.e(TAG, "Stop discovery failed", e);
            }
        }
        activeListeners.clear();
    }

    @Override
    public void onDeviceClick(DeviceDetail device) {
        Intent intent = new Intent(this, DeviceDetailActivity.class);
        intent.putExtra("device_ip", device.getIp());
        startActivity(intent);
    }

}