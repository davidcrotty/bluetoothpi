package net.davidcrotty.bluetoothpi;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelUuid;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import net.davidcrotty.bluetoothpi.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final String BLUETOOTH_SCAN_THREAD = "BLUETOOTH_SCAN_THREAD";
    private final int SCAN_DURATION_MS = 10000;
    private final int ENABLE_BLUETOOTH_REQUEST = 1;
    private final int ENABLE_LOCATION_REQUEST = 2;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private LEScanCallback scanCallback;
    private GATTServerCallback gattServerCallback;
    private BluetoothGattServer gattServer;
    private BluetoothAdvertiseCallback advertiseCallback;
    private HandlerThread scanThread;
    private boolean isScanning;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setHandler(this);
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        scanCallback = new LEScanCallback(this);
        gattServerCallback = new GATTServerCallback();

        scanThread = new HandlerThread(BLUETOOTH_SCAN_THREAD);
        scanThread.start();
    }

    public void checkedChangedListener(View view, boolean checked) {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestCoarseLocationRuntimePermission();
            view.setTag(R.id.TAG_BT_ACTION, true);
            return;
        }
        if(bluetoothEnabled()) {
            switch (view.getId()) {
                case R.id.scan_toggle:
                    toggleScan(checked);
                    break;
                case R.id.advertise_toggle:
                    if(bluetoothGATTServerEnabled()) {
                        toggleAdvertise(checked);
                    } else {
                        binding.advertiseToggle.performClick();
                        binding.advertiseToggle.setEnabled(false);
                        Toast.makeText(this, "Whoops looks like this device does not support" +
                                "Bluetooth LE advertising", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        } else {
            binding.scanToggle.setChecked(false);
            view.setTag(R.id.TAG_BT_ACTION, true);
            promptBluetoothEnableDialog();
        }
    }

    @Override
    public void finish() {
        super.finish();
        closeGattServer();
    }

    private void requestCoarseLocationRuntimePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.dialog_title))
                    .setMessage(getResources().getString(R.string.dialog_description))
                    .setPositiveButton(getResources().getString(R.string.dialog_positive), null)
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    ENABLE_LOCATION_REQUEST);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ENABLE_BLUETOOTH_REQUEST) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    retryToggleAction();
                    break;
                default:
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length != 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            retryToggleAction();
        }
    }

    private void retryToggleAction() {
        View target = null;
        boolean scanToggle = binding.scanToggle.getTag(R.id.TAG_BT_ACTION) == null ? false : (boolean) binding.scanToggle.getTag(R.id.TAG_BT_ACTION);
        boolean advertiseToggle = binding.advertiseToggle.getTag(R.id.TAG_BT_ACTION) == null ? false : (boolean) binding.advertiseToggle.getTag(R.id.TAG_BT_ACTION);
        if(scanToggle) {
            target = binding.scanToggle;
            binding.scanToggle.setTag(R.id.TAG_BT_ACTION, false);
        } else if (advertiseToggle){
            target = binding.advertiseToggle;
            binding.advertiseToggle.setTag(R.id.TAG_BT_ACTION, false);
        }
        ((ToggleButton) target).setChecked(true);
        target.setSelected(true);
        checkedChangedListener(target, true);
    }

    private void promptBluetoothEnableDialog() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST);
    }

    private void toggleAdvertise(boolean shouldAdvertise) {
        if(shouldAdvertise) {
            GATTServerCallback serverCallback = new GATTServerCallback();
            gattServer = bluetoothManager.openGattServer(this.getApplicationContext(), serverCallback);
            BluetoothGattService service = new BluetoothGattService(UUID.fromString(BuildConfig.DEVICE_UUID),
                    BluetoothGattService.SERVICE_TYPE_PRIMARY);
            gattServer.addService(service);
            AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
                AdvertiseSettings settings = settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setTimeout(0)
                .build();

                ParcelUuid pUuid = ParcelUuid.fromString(BuildConfig.DEVICE_UUID);

                AdvertiseData data = new AdvertiseData.Builder()
                        .setIncludeDeviceName(true) //setting to true breaks LE 31 byte limit
                        .addServiceUuid( pUuid )
                        .build();

                advertiseCallback = new BluetoothAdvertiseCallback();

                bluetoothAdapter.getBluetoothLeAdvertiser().startAdvertising(settings,
                        data,
                        advertiseCallback);
        } else {
            closeGattServer();
            bluetoothAdapter.getBluetoothLeAdvertiser().stopAdvertising(advertiseCallback);
        }
    }

    private void closeGattServer() {
        if(gattServer != null) {
            gattServer.close();
        }
    }

    private void toggleScan(boolean shouldScan) {
        if(shouldScan) {
            if(isScanning) return;
            Handler scanHandler = new Handler(scanThread.getLooper());
            scanHandler.post(new Runnable() {
                @Override
                public void run() {

                    List<ScanFilter> filters = new ArrayList<ScanFilter>();

                    ScanFilter filter = new ScanFilter.Builder()
                            .setServiceUuid( new ParcelUuid(UUID.fromString(BuildConfig.DEVICE_UUID)))
                            .build();
                    filters.add(filter);

                    ScanSettings settings = new ScanSettings.Builder()
                            .setScanMode( ScanSettings.SCAN_MODE_LOW_LATENCY )
                            .build();

                    bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
                }
            });
            scanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(bluetoothEnabled() == false) return;
                    bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.scanToggle.setChecked(false);
                            isScanning = false;
                        }
                    });
                }
            }, SCAN_DURATION_MS);
            isScanning = true;
        } else {
            if(isScanning == false) return;
            bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
            bluetoothAdapter.disable();
            isScanning = false;
        }
    }

    private boolean bluetoothEnabled() {
        if(bluetoothAdapter == null) return false;
        return bluetoothAdapter.isEnabled();
    }

    /**
     * Appears isEnabled is not enough, bluetoothAdapter can still through an NPE
     * when getting the LE Advertiser. This check performs the same as getBluetoothLeAdvertiser()
     * without throwing the NPE.
     *
     * @return
     */
    private boolean bluetoothGATTServerEnabled() {
        if(bluetoothAdapter == null) return false;
        if(bluetoothAdapter.isEnabled()) {
          if(bluetoothAdapter.isMultipleAdvertisementSupported())   {
              return true;
          }
        }

        return false;
    }
}
