package net.davidcrotty.bluetoothpi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import net.davidcrotty.bluetoothpi.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private final String BLUETOOTH_SCAN_THREAD = "BLUETOOTH_SCAN_THREAD";
    private final int SCAN_DURATION_MS = 1000;
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
        if(bluetoothAdapter.isEnabled()) {
            toggleScan(checked);
        } else {
            binding.scanToggle.setChecked(false);
            promptBluetoothEnableDialog();
        }
    }

    private void promptBluetoothEnableDialog() {
        new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.dialog_title))
            .setMessage(getResources().getString(R.string.dialog_text))
            .setPositiveButton(getResources().getString(R.string.dialog_positive), null)
            .create()
            .show();
    }

    private void toggleScan(boolean shouldScan) {
        if(shouldScan) {
            if(isScanning) return;
            Handler scanHandler = new Handler(scanThread.getLooper());
            scanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
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
        }
    }
}
