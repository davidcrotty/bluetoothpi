package net.davidcrotty.bluetoothpi;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.ParcelUuid;

import java.util.List;

import timber.log.Timber;

/**
 * Created by David Crotty on 29/07/2017.
 * <p>
 * Copyright © 2017 David Crotty - All Rights Reserved
 */

public class LEScanCallback extends ScanCallback {

    interface ServiceLocatedListener {
        void onLocated();
    }

    private final Context context;
    private final GATTClientCallback callback;
    private final ServiceLocatedListener listener;

    public LEScanCallback(Context context, GATTClientCallback callback, ServiceLocatedListener listener) {
        this.context = context;
        this.callback = callback;
        this.listener = listener;
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);
        Timber.d("Device Found");
        ScanRecord record = result.getScanRecord();
        if(record != null) {
            List<ParcelUuid> serviceList = record.getServiceUuids();
            for(ParcelUuid service : serviceList) {
                if(service.getUuid().toString().equalsIgnoreCase(BuildConfig.DEVICE_UUID)) {
                    listener.onLocated();
                    connectToDevice(result.getDevice());
                }
            }
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        device.connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                Timber.d("Connection state " + status + " new state " + newState);

                if(newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if(status != BluetoothGatt.GATT_SUCCESS) {
                    Timber.d("Service error");
                    return;
                }

                Timber.d("Service read");
            }
        });
    }
}
