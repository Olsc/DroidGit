package com.olsc.droidgit.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.olsc.droidgit.R;
import com.olsc.droidgit.business.RepositoryManager;
import com.olsc.droidgit.core.ServerService;
import com.olsc.droidgit.ui.repository.RepositoryListActivity;
import com.olsc.droidgit.ui.settings.AboutActivity;
import com.olsc.droidgit.ui.settings.SettingsActivity;
import com.olsc.droidgit.ui.widget.EulaHelper;
import com.olsc.droidgit.ui.widget.MIUIHelper;
import com.olsc.droidgit.util.Constants;
import com.olsc.droidgit.util.LocaleHelper;
import com.olsc.droidgit.util.NetworkUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final static String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_REQ_CODE = 100;

    private TextView ipTextView;
    private TextView logTextView;
    private TextView wifiStatusTextView;
    private android.widget.ImageView wirelessImageView;
    private Button btnStartStop;

    private BroadcastReceiver serverStatusReceiver;
    private boolean isServerRunning = false;
    private EulaHelper eulaHelper;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_home_dashboard);

        initViews();

        eulaHelper = new EulaHelper(this);
        if (!EulaHelper.isAccepted(this)) {
            eulaHelper.show();
            eulaHelper.setOnRequirementAcceptedListener(this::onAppReadyLogic);
        } else {
            onAppReadyLogic();
        }
    }

    private void onAppReadyLogic() {
        if (MIUIHelper.shouldShowMIUIDialog(this)) {
            MIUIHelper.showMIUIWhitelistDialog(this);
        }
        setupServerStatusReceiver();
        checkAndImportRepositories();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean autoStartOnWifi = prefs.getBoolean(Constants.Prefs.AUTO_START_ON_WIFI,
                Constants.Prefs.DEFAULT_AUTO_START_ON_WIFI);
        if (autoStartOnWifi && NetworkUtils.isWifiReady(this) && !isServerRunning) {
            startServer();
        }
    }

    private void initViews() {
        ipTextView = findViewById(R.id.homeServerInfoTextView);
        logTextView = findViewById(R.id.homeWifiSSID);
        wifiStatusTextView = findViewById(R.id.homeWifiStatus);
        wirelessImageView = findViewById(R.id.homeWirelessImage);

        btnStartStop = findViewById(R.id.homeBtnStartStop);
        btnStartStop.setOnClickListener(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isServerRunning = NetworkUtils.isServiceRunning(this, ServerService.class);
        updateUI();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.homeBtnStartStop) {
            if (isServerRunning) {
                stopServer();
            } else {
                startServer();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        recreate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem settingsItem = menu.add(0, 1, 0, R.string.menu_settings);
        settingsItem.setIcon(R.drawable.ic_actionbar_settings);
        settingsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        settingsItem.setOnMenuItemClickListener(item -> {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        });

        MenuItem repoItem = menu.add(0, 3, 0, R.string.menu_repositories);
        repoItem.setIcon(R.drawable.ic_repository);
        repoItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        repoItem.setOnMenuItemClickListener(item -> {
            startActivity(new Intent(this, RepositoryListActivity.class));
            return true;
        });

        MenuItem scanItem = menu.add(0, 4, 0, R.string.menu_scan);
        scanItem.setIcon(android.R.drawable.ic_menu_rotate);
        scanItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        scanItem.setOnMenuItemClickListener(item -> {
            checkAndImportRepositories();
            return true;
        });

        MenuItem aboutItem = menu.add(0, 2, 0, R.string.about_support_title);
        aboutItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        aboutItem.setOnMenuItemClickListener(item -> {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        });

        return true;
    }

    private void startServer() {
        if (!checkAndRequestPermissions())
            return;

        checkAndImportRepositories();

        Intent intent = new Intent(this, ServerService.class);
        intent.setAction(Constants.Action.GIT_SERVER_STARTED);
        startForegroundService(intent);
    }

    private void stopServer() {
        Intent intent = new Intent(this, ServerService.class);
        intent.setAction(Constants.Action.GIT_SERVER_STOPPED);
        stopService(intent);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void setupServerStatusReceiver() {
        serverStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Constants.Action.GIT_SERVER_STARTED.equals(action)) {
                    isServerRunning = true;
                    updateUI();
                } else if (Constants.Action.GIT_SERVER_STOPPED.equals(action)) {
                    isServerRunning = false;
                    updateUI();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.Action.GIT_SERVER_STARTED);
        filter.addAction(Constants.Action.GIT_SERVER_STOPPED);
        if (Build.VERSION.SDK_INT >= 34) {
            registerReceiver(serverStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(serverStatusReceiver, filter);
        }

        isServerRunning = NetworkUtils.isServiceRunning(this, ServerService.class);
        updateUI();
    }

    @SuppressLint("SetTextI18n")
    private void updateUI() {
        // 获取网络状态
        boolean networkReady = NetworkUtils.isWifiReady(this);
        String ipAddress = NetworkUtils.getIpAddress();

        if (isServerRunning) {
            btnStartStop.setText(R.string.home_stop);

            int port;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            try {
                port = Integer.parseInt(
                        prefs.getString(Constants.Prefs.HTTP_PORT, String.valueOf(Constants.Prefs.DEFAULT_HTTP_PORT)));
            } catch (ClassCastException e) {
                try {
                    port = prefs.getInt(Constants.Prefs.HTTP_PORT, Constants.Prefs.DEFAULT_HTTP_PORT);
                } catch (Exception ex) {
                    port = Constants.Prefs.DEFAULT_HTTP_PORT;
                }
            } catch (NumberFormatException e) {
                port = Constants.Prefs.DEFAULT_HTTP_PORT;
            }

            if (networkReady && ipAddress != null && !ipAddress.isEmpty()) {
                // 网络可用，显示访问地址
                String info = getString(R.string.server_info_format, ipAddress, String.valueOf(port));
                ipTextView.setText(info);
                ipTextView.setVisibility(View.VISIBLE);
                logTextView.setText(R.string.notification_title);

                // 更新网络状态显示
                wifiStatusTextView.setText(R.string.network_ready);
                wirelessImageView.setImageResource(R.drawable.ic_wireless_enabled);
            } else {
                // 服务运行但网络不可用
                ipTextView.setText(R.string.wifi_not_connected);
                ipTextView.setVisibility(View.VISIBLE);
                logTextView.setText(R.string.server_running_no_network);

                // 更新网络状态显示
                wifiStatusTextView.setText(R.string.wifi_not_connected);
                wirelessImageView.setImageResource(R.drawable.ic_wireless_disabled);
            }
        } else {
            btnStartStop.setText(R.string.home_start);

            if (networkReady && ipAddress != null && !ipAddress.isEmpty()) {
                // 网络可用但服务未启动
                ipTextView.setText(getString(R.string.network_ready_format, ipAddress));
                ipTextView.setVisibility(View.VISIBLE);
                logTextView.setText(R.string.server_stopped);

                // 更新网络状态显示
                wifiStatusTextView.setText(R.string.network_ready);
                wirelessImageView.setImageResource(R.drawable.ic_wireless_enabled);
            } else {
                // 网络不可用且服务未启动
                ipTextView.setText(R.string.wifi_not_connected);
                ipTextView.setVisibility(View.VISIBLE);
                logTextView.setText(R.string.server_stopped_no_network);

                // 更新网络状态显示
                wifiStatusTextView.setText(R.string.wifi_not_connected);
                wirelessImageView.setImageResource(R.drawable.ic_wireless_disabled);
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private void checkAndImportRepositories() {
        if (checkAndRequestPermissions()) {
            new Thread(() -> {
                Log.i(TAG, "Starting repository scan thread...");
                RepositoryManager rm = new RepositoryManager(MainActivity.this);
                int count = rm.scanAndImportAll();
                Log.i(TAG, "Scan context: default path, result count: " + count);

                if (count > 0) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            getString(R.string.imported_repositories_toast, count), Toast.LENGTH_SHORT).show());
                }
            }).start();
        } else {
            Log.d(TAG, "Scan deferred: Permissions not granted yet.");
        }
    }

    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
                Toast.makeText(this, R.string.enable_all_files_access_toast, Toast.LENGTH_LONG).show();
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE },
                        PERMISSION_REQ_CODE);
                return false;
            }
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this,
                    "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { "android.permission.POST_NOTIFICATIONS" },
                        PERMISSION_REQ_CODE);
                return false;
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 收到权限后尝试导入库
                checkAndImportRepositories();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 给系统一点宽限时间来更新权限状态
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (Environment.isExternalStorageManager()) {
                    Log.i(TAG, "All Files Access granted, triggering scan.");
                    checkAndImportRepositories();
                } else {
                    Log.w(TAG, "Returned from Settings but All Files Access still not granted.");
                }
            }, 500);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serverStatusReceiver != null) {
            unregisterReceiver(serverStatusReceiver);
        }
    }
}
