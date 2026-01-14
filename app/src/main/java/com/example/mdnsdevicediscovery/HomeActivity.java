package com.example.mdnsdevicediscovery;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
    private WifiManager.MulticastLock multicastLock;

    // SIMPLIFIED - START WITH ONE SERVICE FIRST
    private final String[] serviceTypes = {"_http._tcp", "_airplay._tcp", "_raop._tcp", "_ipp._tcp", "_ipps._tcp", "_googlecast._tcp", "_workstation._tcp", "_smb._tcp", "_ssh._tcp"};  // ROUTERS ALWAYS ADVERTISE THIS
    private List<String> currentDiscoveryIps = new ArrayList<>();
    private List<NsdManager.DiscoveryListener> activeListeners = new ArrayList<>();
    private static final String TAG = "HomeActivity";
    private boolean isDiscovering = false;

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

        // 1. PERMISSIONS FIRST
        requestPermissions();

        // 2. INIT UI + DB
        initDatabase();
        setupRecyclerView();

        // 3. 10s DELAY + WiFi check + MulticastLock
        new Handler(Looper.getMainLooper()).postDelayed(this::startDiscoveryWithChecks, 10000);
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissions.toArray(new String[0]), 1001);
        }
    }

    private void startDiscoveryWithChecks() {
        // WiFi CHECK
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "❌ WiFi must be ENABLED", Toast.LENGTH_LONG).show();
            return;
        }

        // MULTICAST LOCK - CRITICAL!
        acquireMulticastLock();

        // START DISCOVERY
        startDiscovery();
    }

    private void acquireMulticastLock() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifiManager.createMulticastLock("mdns_lock");
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();
        Log.d(TAG, "✅ MulticastLock ACQUIRED - mDNS NOW POSSIBLE");
    }

    private void initDatabase() {
        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "mDNS-device-discovery")
                .allowMainThreadQueries()  // Remove in production
                .build();
        deviceDetailDao = db.deviceDetailDao();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeviceDetailAdapter(this);
        recyclerView.setAdapter(adapter);
        loadCachedDevices();
    }

    private void loadCachedDevices() {
        new Thread(() -> {
            List<DeviceDetailEntity> entities = deviceDetailDao.getAllDevices();
            runOnUiThread(() -> {
                deviceList.clear();
                for (DeviceDetailEntity entity : entities) {
                    deviceList.add(new DeviceDetail(entity.deviceName, entity.deviceIpAddress, entity.deviceStatus));
                }
                adapter.updateDevices(deviceList);
            });
        }).start();
    }

    // 🔥 SIMPLIFIED SINGLE SERVICE DISCOVERY FIRST
    private void startDiscovery() {
        if (isDiscovering) return;
        isDiscovering = true;

        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);

        // ONLY _http._tcp FIRST - EVERY ROUTER HAS THIS
        for (String serviceType : serviceTypes) {
            NsdManager.DiscoveryListener listener = createDiscoveryListener(serviceType);
            activeListeners.add(listener);
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener);
            Log.d(TAG, "🔍 Started discovery: " + serviceType);
        }
    }

    private NsdManager.DiscoveryListener createDiscoveryListener(final String serviceType) {
        return new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "🚀 Discovery STARTED: " + serviceType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "📡 SERVICE FOUND: " + service.getServiceName() + " (" + serviceType + ")");

                // NEW ResolveListener per service
                NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        handleServiceResolved(serviceInfo);
                    }
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        Log.e(TAG, "❌ Resolve FAILED: " + errorCode);
                    }
                };
                nsdManager.resolveService(service, resolveListener);
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.d(TAG, "📴 Service LOST: " + service.getServiceName());
            }

            @Override
            public void onDiscoveryStopped(String regType) {
                Log.d(TAG, "⏹️ Discovery STOPPED: " + serviceType);
                processDiscoverySession();
            }

            @Override
            public void onStartDiscoveryFailed(String regType, int errorCode) {
                Log.e(TAG, "💥 Start FAILED: " + serviceType + " code=" + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String regType, int errorCode) {
                Log.e(TAG, "💥 Stop FAILED: " + serviceType + " code=" + errorCode);
            }
        };
    }

    private void handleServiceResolved(NsdServiceInfo serviceInfo) {
        String name = serviceInfo.getServiceName();
        String ip = serviceInfo.getHost().getHostAddress();

        Log.d(TAG, "✅ RESOLVED: " + name + " @ " + ip);

        DeviceDetailEntity device = new DeviceDetailEntity(name, ip, true, System.currentTimeMillis());
        deviceDetailDao.insertOrUpdate(device);
        currentDiscoveryIps.add(ip);
        refreshDisplay();
    }

    private void processDiscoverySession() {
        long now = System.currentTimeMillis();
        deviceDetailDao.markOfflineExcept(currentDiscoveryIps, now);
        refreshDisplay();
    }

    private void refreshDisplay() {
        runOnUiThread(() -> {
            deviceList.clear();
            List<DeviceDetailEntity> entities = deviceDetailDao.getAllDevices();
            for (DeviceDetailEntity entity : entities) {
                deviceList.add(new DeviceDetail(entity.deviceName, entity.deviceIpAddress, entity.deviceStatus));
            }
            adapter.updateDevices(deviceList);
            Log.d(TAG, "📱 UI Updated: " + deviceList.size() + " devices");
        });
    }

    @Override
    protected void onDestroy() {
        stopAllDiscovery();
        if (multicastLock != null && multicastLock.isHeld()) {
            multicastLock.release();
        }
        super.onDestroy();
    }

    private void stopAllDiscovery() {
        if (nsdManager == null) return;
        for (NsdManager.DiscoveryListener listener : activeListeners) {
            try {
                nsdManager.stopServiceDiscovery(listener);
            } catch (Exception e) {
                Log.e(TAG, "Stop discovery error", e);
            }
        }
        activeListeners.clear();
        isDiscovering = false;
    }

    @Override
    public void onDeviceClick(DeviceDetail device) {
        Intent intent = new Intent(this, DeviceDetailActivity.class);
        intent.putExtra("device_ip", device.getIp());
        startActivity(intent);
    }
}