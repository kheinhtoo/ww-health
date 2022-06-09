package com.weeswares.iok.health;

import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.weeswares.iok.health.databinding.ActivityMainBinding;
import com.weeswares.iok.health.fragments.LogSheetDialogFragment;
import com.weeswares.iok.health.fragments.OutputFragment;
import com.weeswares.iok.health.helpers.Bluetooth;
import com.weeswares.iok.health.helpers.HexString;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final static int LOCATION_REQUEST_CODE = 521;
    private final static int REQUEST_TURN_ON_BT = 522;


    private static final String HEART_RATE_DEVICE = "BX02-7394";// wearable (confirmed)
    private static final String HEART_RATE_DEVICE_CHAR_ID = "000033f2-0000-1000-8000-00805f9b34fb";

    private static final String OXI_METER_DEVICE = "KS M6100P";// oximeter (3rd pair & 4th pair). (confirmed)
    private static final String OXI_METER_DEVICE_CHAR_ID = "0000fff1-0000-1000-8000-00805f9b34fb";

    private static final String BP_DEVICE = "Yuwell BP-YE670D";// BP.  (confirmed)
    private static final String BP_DEVICE_CHAR_ID = "00002a35-0000-1000-8000-00805f9b34fb";

    private static final String TEMPERATURE_DEVICE = "Bluetooth BP";// thermometer  (confirmed)
    public static final String TEMP_DEVICE_NAME = TEMPERATURE_DEVICE;
    private static final String TEMPERATURE_DEVICE_CHAR_ID = "0000fff1-0000-1000-8000-00805f9b34fb";

    private static final String WEIGHT_DEVICE = "MI_SCALE";// weight scale (confirmed)
    private static final String WEIGHT_DEVICE_CHAR_ID = "00002a9d-0000-1000-8000-00805f9b34fb";
    private final OutputFragment heartRateFragment = OutputFragment.newInstance("Heart rate", HEART_RATE_DEVICE_CHAR_ID, true, R.drawable.ic_heart_rate),
            temperatureFragment = OutputFragment.newInstance("Temperature", TEMPERATURE_DEVICE_CHAR_ID, true, R.drawable.ic_temperature),
            oximeterFragment = OutputFragment.newInstance("Blood oxygen", OXI_METER_DEVICE_CHAR_ID, false, R.drawable.ic_blood_oxygen),
            weightFragment = OutputFragment.newInstance("Weight", WEIGHT_DEVICE_CHAR_ID, true, R.drawable.ic_weight),
            bpFragment = OutputFragment.newInstance("Blood pressure", BP_DEVICE_CHAR_ID, true, R.drawable.ic_blood_pressure);
    private ActivityMainBinding activityMainBinding;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner leScanner;
    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            deviceFound(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            //

        }

        @Override
        public void onScanFailed(int errorCode) {
            /*if (errorCode == SCAN_FAILED_ALREADY_STARTED) {
                showToast("already start");
            }*/
            if (errorCode == SCAN_FAILED_FEATURE_UNSUPPORTED) {
                showToast("scan settings not supported");
            }
            if (errorCode == 6) {
                showToast("too frequently");
            }
        }
    };

    public static boolean isLocationEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        return gps || network;
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setSupportActionBar(activityMainBinding.toolbar);
        checkPermission();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        displayFoundDevice(R.id.heart_rate, heartRateFragment);
        displayFoundDevice(R.id.temperature, temperatureFragment);
        displayFoundDevice(R.id.oximeter, oximeterFragment);
        displayFoundDevice(R.id.weight, weightFragment);
        displayFoundDevice(R.id.bp, bpFragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.logs).setOnMenuItemClickListener(menuItem -> {
            LogSheetDialogFragment bottomSheetFragment = LogSheetDialogFragment.newInstance();
            bottomSheetFragment.show(getSupportFragmentManager(), bottomSheetFragment.getTag());
            return true;
        });
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        scanDevice(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        scanDevice(false);
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }
        // scan
        iniBT();
    }

    @SuppressLint("MissingPermission")
    private void scanDevice(final boolean enable) {
        AsyncTask.execute(() -> {
            if (enable) {
                ScanSettings settings;
                if (Build.VERSION.SDK_INT >= 23) {
                    settings = new ScanSettings.Builder()
                            .setScanMode(SCAN_MODE_LOW_LATENCY)
                            .setCallbackType(CALLBACK_TYPE_ALL_MATCHES)
                            .build();
                } else {
                    settings = new ScanSettings.Builder()
                            .setScanMode(SCAN_MODE_LOW_LATENCY)
                            .build();
                }
                leScanner.startScan(null, settings, leScanCallback);
            } else {
                if (leScanner != null && leScanCallback != null) {
                    if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                        leScanner.stopScan(leScanCallback);
                    }
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void deviceFound(ScanResult result) {
        BluetoothDevice device = result.getDevice();
        if (device.getName() != null && device.getName().length() > 0) {
            Bluetooth b = new Bluetooth(device.getName(), device, result.getRssi());
            if (b.getName().startsWith(HEART_RATE_DEVICE)) {
                heartRateFragment.setResultParser(response -> String.format(Locale.ENGLISH, "%d BPM", response[3] & 0xFF));
                heartRateFragment.setBluetooth(b);
            } else if (b.getName().startsWith(TEMPERATURE_DEVICE)) {
                temperatureFragment.setResultParser(response -> {
                    double temperature = (response[5] - 0x30) * 10 + (response[6] - 0x30) + (response[8] - 0x30) * 0.1;
                    return String.format(Locale.ENGLISH, "%.1f \u00B0C", temperature);
                });
                temperatureFragment.setBluetooth(b);
            } else if (b.getName().startsWith(OXI_METER_DEVICE)) {
                oximeterFragment.setResultParser(response -> String.format(Locale.ENGLISH, "%d %d", response[3] & 0xFF, response[4] & 0xFF));
                oximeterFragment.setBluetooth(b);
            } else if (b.getName().startsWith(WEIGHT_DEVICE)) {
                weightFragment.setResultParser(response -> String.format(Locale.ENGLISH, "%.2f kg", HexString.calculateWeight(response)));
                weightFragment.setBluetooth(b);
            } else if (b.getName().startsWith(BP_DEVICE)) {
                bpFragment.setResultParser(response -> String.format(Locale.ENGLISH, "%d/%d %d", response[1] & 0xFF, response[3] & 0xFF, response[14] & 0xFF));
                bpFragment.setBluetooth(b);
            }
        }
        if (heartRateFragment.getBluetooth() != null
                && temperatureFragment.getBluetooth() != null
                && oximeterFragment.getBluetooth() != null
                && weightFragment.getBluetooth() != null
                && bpFragment.getBluetooth() != null) {
            // if all devices are already found, stop the device scan.
            scanDevice(false);
        }
    }

    private void displayFoundDevice(int layoutID, Fragment f) {
        getSupportFragmentManager()
                .beginTransaction()
                .add(layoutID, f)
                .addToBackStack(null)
                .commit();
    }

    @SuppressLint("MissingPermission")
    private void iniBT() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_TURN_ON_BT);
        } else if (!isLocationEnable(this) && Build.VERSION.SDK_INT >= 23) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 1);
        } else {
            BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = manager.getAdapter();
            leScanner = bluetoothAdapter.getBluetoothLeScanner();

            scanDevice(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            checkPermission();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        iniBT();
    }
}