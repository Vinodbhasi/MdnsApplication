package com.example.mdnsdevicediscovery;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class DeviceDetailActivity extends AppCompatActivity {

    private static final String TAG = "DeviceDetailActivity";
    private TextView publicIpAddressDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_device_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        String deviceIp = getIntent().getStringExtra("device_ip");

        // Start network calls in background thread
        new FetchIpDetailsTask().execute();

    }

    private void initViews() {
        publicIpAddressDetails = findViewById(R.id.publicIpAddressDetails);
    }

    // AsyncTask for network operations (MainThread → Background → UI Thread)
    private class FetchIpDetailsTask extends AsyncTask<Void, Void, IpInfo> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(DeviceDetailActivity.this);
            progressDialog.setMessage("Fetching IP details...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected IpInfo doInBackground(Void... voids) {
            try {
                // Step 1: Get Public IP
                String publicIp = fetchPublicIp();
                if (publicIp == null) return null;

                // Step 2: Get Geo details
                return fetchIpGeoDetails(publicIp);

            } catch (Exception e) {
                Log.e(TAG, "Network error", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(IpInfo ipInfo) {
            progressDialog.dismiss();

            if (ipInfo != null) {
                populateUI(ipInfo);
            } else {
                Toast.makeText(DeviceDetailActivity.this,
                        "Failed to fetch IP details. Check internet connection.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private String fetchPublicIp() throws Exception {
        URL url = new URL("https://api.ipify.org?format=json");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "MdnsApp/1.0");

        if (conn.getResponseCode() == 200) {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String json = br.readLine();
            br.close();

            // Parse {"ip":"106.222.236.150"}
            return parseIpFromJson(json);
        }
        conn.disconnect();
        return null;
    }

    private IpInfo fetchIpGeoDetails(String ip) throws Exception {
        URL url = new URL("https://ipinfo.io/" + ip + "/geo");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "MdnsApp/1.0");

        if (conn.getResponseCode() == 200) {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder fullResponse = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                fullResponse.append(line);
            }
            br.close();

            String json = fullResponse.toString();
            System.out.println("FULL JSON: " + json);

            IpInfo info = parseGeoJson(json);
            System.out.println("ip is : "+info.ip+", info city :"+info.city);
            info.ip = ip;
            conn.disconnect();
            return info;
        }
        conn.disconnect();
        return null;
    }

    private String parseIpFromJson(String json) {
        if (json == null || !json.contains("ip")) return null;
        try {
            int start = json.indexOf("\"ip\":\"") + 6;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            Log.e(TAG, "JSON parse error", e);
            return null;
        }
    }

    private IpInfo parseGeoJson(String json) {
        IpInfo info = new IpInfo();
        if (json == null || json.trim().isEmpty()) return info;

        try {
            JSONObject jsonObject = new JSONObject(json);

            // Safe extraction with .optString() - never null
            info.city = jsonObject.optString("city", "N/A");
            info.region = jsonObject.optString("region", "N/A");
            info.country = jsonObject.optString("country", "N/A");
            info.location = jsonObject.optString("loc", "N/A");
            info.org = jsonObject.optString("org", "N/A");
            info.postal = jsonObject.optString("postal", "N/A");
            info.timezone = jsonObject.optString("timezone", "N/A");

            System.out.println("Parsed: " + info.city + ", " + info.region);

        } catch (Exception e) {
            System.out.println("JSON parse error: " + e.getMessage());
        }

        return info;
    }

    private void populateUI(IpInfo info) {
        publicIpAddressDetails.setText("Ip : "+info.ip+", City : "+info.city+", Region : "+info.region+", Country : "+info.country+", Org : "+info.org+", Loc : "+info.location+", Carrier : N/A");
    }

    // Data class for IP info
    private static class IpInfo {
        String ip, city, region, country, location, org, postal, timezone;
    }

}