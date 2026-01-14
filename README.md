mDNS Device Discovery App 🚀
[
[

Discover all devices on your local WiFi network using mDNS protocol. View IP geolocation details with offline caching.

✨ Features
Real-time mDNS Network Discovery - Finds TVs, printers, PCs, routers, NAS
Room Database Persistence - Offline device caching + status tracking
IP Geolocation API - City, region, ISP, coordinates (ipinfo.io)
RecyclerView UI - Clean device list with online/offline status
Multi-service Discovery - AirPlay, IPP, HTTP, SMB, Chromecast, etc.
WiFi Support - Works across all local networks

📱 Screenshots
Home Screen	Device Details

🛠 Tech Stack
text
• Android SDK (Java 8)
• NsdManager (mDNS/DNS-SD)
• Room Database (SQLite)
• RecyclerView + CardView
• HttpURLConnection + JSONObject
• ExecutorService (Modern threading)

📋 Prerequisites
Physical Android device (Emulator DOES NOT work)
WiFi enabled (same network as target devices)
Min SDK 21 (Android 5.0+)

🚀 Quick Start
1. Clone & Build
  bash
  git clone https://github.com/yourusername/mdns-device-discovery.git
  cd mdns-device-discovery
  ./gradlew build
2. Install APK
  adb install app/build/outputs/apk/debug/app-debug.apk
3. Launch & Wait
  1. Open app → Wait 10 seconds (WiFi settling)
  2. See "MulticastLock ACQUIRED" in Logcat
  3. Router (192.168.x.1) appears first!
  4. Click device → View geolocation details
     
🔧 Permissions Required
AndroidManifest.xml:
  xml
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
  <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
  <uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" 
      tools:targetApi="33" />
      
🧪 Testing Checklist
  ✅ Check	Expected Result
  Physical device	Samsung/Pixel (NOT emulator)
  WiFi enabled	Same network as router
  Logcat shows	MulticastLock ACQUIRED
  First device	Router 192.168.x.1 via _http._tcp
  Status updates	Green=online, Gray=offline
  Tap device	Geolocation details load

📊 Supported mDNS Services
  _http._tcp      → Routers, Smart Devices
  _ipp._tcp       → Printers/Scanners
  _airplay._tcp   → Apple TV/AirPlay
  _googlecast._tcp→ Chromecast
  _workstation._tcp→ Windows PCs
  _smb._tcp       → NAS/File Servers
  _ssh._tcp       → Linux/Mac Servers

🏗 Project Structure
  app/src/main/java/com/example/mdnsdevicediscovery/
  ├── HomeActivity.java           # Main discovery screen
  ├── DeviceDetailActivity.java   # IP geolocation details
  ├── DeviceDetail.java           # RecyclerView model
  ├── DeviceDetailAdapter.java    # RecyclerView adapter
  ├── mdnsdiscoverydataManagement/
  │   ├── AppDatabase.java        # Room database
  │   ├── DeviceDetailDao.java    # Room DAO (insertOrUpdate)
  │   └── DeviceDetailEntity.java # Room entity
  
🔍 Key Implementation Details
1. mDNS Discovery Flow
  MulticastLock.acquire()
  ↓
  discoverServices("_http._tcp") → onServiceFound()
  ↓
  resolveService() → onServiceResolved()
  ↓
  deviceDao.insertOrUpdate() → Room DB
  ↓
  RecyclerView refresh
2. Smart Status Updates
  Discovery → online=true + timestamp
  Missing → markOfflineExcept() → online=false
  24h cleanup → deleteOldOffline()
3. IP Geolocation
  ipify.org → Public IP
  ipinfo.io/geo → City/ISP/Coords
  ExecutorService → Network OFF main thread
  JSONObject → Robust JSON parsing

🚨 Troubleshooting
  Issue	Solution
  No devices	1. Physical device 2. WiFi ON 3. MulticastLock
  Listener error	Unique DiscoveryListener per serviceType
  AsyncTask warning	Use ExecutorService + Handler
  JSON null	JSONObject.optString() parsing
  
📱 Expected Results (Your Network)
  Router: 192.168.1.1 (http._tcp)
  Your Phone: Realme-ABC (workstation._tcp) 
  Printer: HP-LaserJet (ipp._tcp)
  ISP: Bharti Airtel, Kerala [106.222.236.150]
  Location: Kanayannur, 9.9667,76.2667
  
🤝 Contributing
  Fork repository
  Create feature branch (git checkout -b feature/mdns)
  Commit changes (git commit -m 'Add new service type')
  Push (git push origin feature/mdns)
  Open Pull Request

📄 License
This project is licensed under the MIT License - see the LICENSE file for details.

🙏 Acknowledgments
  Android NsdManager - mDNS discovery core
  ipinfo.io - Free IP geolocation API
  Room Database - Offline persistence
  Your patience debugging multicast issues!
