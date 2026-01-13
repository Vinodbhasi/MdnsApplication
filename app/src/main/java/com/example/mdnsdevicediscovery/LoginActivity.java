package com.example.mdnsdevicediscovery;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.OnFailureListener;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

@SuppressWarnings("deprecation") // Legacy GSI fully functional in 21.2.0
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = getSharedPreferences("auth", MODE_PRIVATE);

        // Configure Google Sign-In (LEGACY - WORKS PERFECTLY)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))  // Web Client ID from Google Console
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        SignInButton signInButton = findViewById(R.id.google_login_button);
        signInButton.setOnClickListener(v -> signIn());

        // Silent authentication check
        checkSilentAuth();
    }

    private void checkSilentAuth() {
        if (!isNetworkAvailable()) {
            forceLogout();
            return;
        }

        String cachedToken = prefs.getString("id_token", null);
        if (cachedToken != null) {
            // Token exists and network available - proceed to Home
            Log.d(TAG, "Silent auth successful");
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        // Show login UI
        findViewById(R.id.google_login_button).setVisibility(View.VISIBLE);
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                handleSignInSuccess(account);
            } catch (ApiException e) {
                Log.w(TAG, "SignIn failed: " + e.getStatusCode());
                Toast.makeText(this, "Login failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleSignInSuccess(GoogleSignInAccount account) {
        // Cache OAuth token
        String idToken = account.getIdToken();
        prefs.edit()
                .putString("id_token", idToken)
                .putString("email", account.getEmail())
                .putString("display_name", account.getDisplayName())
                .putLong("login_time", System.currentTimeMillis())
                .apply();

        Log.d(TAG, "OAuth login successful: " + account.getEmail());
        Toast.makeText(this, "Welcome " + account.getDisplayName(), Toast.LENGTH_SHORT).show();

        // Navigate to HomeActivity
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (capabilities != null) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            }
        }
        return false;
    }

    private void forceLogout() {
        prefs.edit().clear().apply();
        Toast.makeText(this, "No network connection. Please connect to WiFi.", Toast.LENGTH_LONG).show();
    }
}
