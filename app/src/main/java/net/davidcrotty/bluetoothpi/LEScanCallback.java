package net.davidcrotty.bluetoothpi;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;

import timber.log.Timber;

/**
 * Created by David Crotty on 29/07/2017.
 * <p>
 * Copyright © 2017 David Crotty - All Rights Reserved
 */

public class LEScanCallback extends ScanCallback {

    private final Context context;
    private final GATTClientCallback callback;

    public LEScanCallback(Context context, GATTClientCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);
        result.getDevice().connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                BluetoothDevice device = gatt.getDevice();
                Timber.d("MAC:" + device.getAddress());

                if(newState == BluetoothProfile.STATE_CONNECTED) {
                    Timber.d("STATE_CONNECTED");
//                    gatt.discoverServices();
                } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Timber.d("STATE_DISCONNECTED");
                    device.getUuids(); //TODO this way seems less complex
//                    device.connectGatt(context, false, callback);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
            }
        });
    }
}
