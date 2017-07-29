package net.davidcrotty.bluetoothpi;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import net.davidcrotty.bluetoothpi.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private final String BLUETOOTH_SCAN_THREAD = "BLUETOOTH_SCAN_THREAD";
    private final int SCAN_DURATION_MS = 10000;
    private final int ENABLE_BLUETOOTH_REQUEST = 1;
    private BluetoothAdapter bluetoothAdapter;
    private LEScanCallback scanCallback;
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
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = manager.getAdapter();
        scanCallback = new LEScanCallback();

        scanThread = new HandlerThread(BLUETOOTH_SCAN_THREAD);
        scanThread.start();
    }

    public void checkedChangedListener(View view, boolean checked) {
        if(bluetoothEnabled()) {
            toggleScan(checked);
        } else {
            binding.scanToggle.setChecked(false);
            promptBluetoothEnableDialog();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ENABLE_BLUETOOTH_REQUEST) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    binding.scanToggle.performClick();
                    break;
                default:
            }
        }
    }

    private void promptBluetoothEnableDialog() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST);
    }

    private void toggleScan(boolean shouldScan) {
        if(shouldScan) {
            if(isScanning) return;
            Handler scanHandler = new Handler(scanThread.getLooper());
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
            scanHandler.post(new Runnable() {
                @Override
                public void run() {
                    bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
                }
            });
            isScanning = true;
        } else {
            if(isScanning == false) return;
            bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
            isScanning = false;
        }
    }

    private boolean bluetoothEnabled() {
        if(bluetoothAdapter == null) return false;
        return bluetoothAdapter.isEnabled();
    }
}
