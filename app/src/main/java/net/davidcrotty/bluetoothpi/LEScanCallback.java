package net.davidcrotty.bluetoothpi;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
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

    private final Context context;
    private final GATTClientCallback callback;

    public LEScanCallback(Context context, GATTClientCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);
//        BluetoothDevice device = result.getDevice();
        Timber.d("Device Found");
//        boolean fetchedUUIDS = device.fetchUuidsWithSdp(); //TODO this way seems less complex
        ScanRecord record = result.getScanRecord();
        if(record != null) {
            List<ParcelUuid> serviceList = record.getServiceUuids();
            for(ParcelUuid service : serviceList) {
                Timber.d("Service found: " + service.getUuid().toString());
            }
        }
    }
}
