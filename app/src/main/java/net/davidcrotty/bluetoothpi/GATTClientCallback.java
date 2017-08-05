package net.davidcrotty.bluetoothpi;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.os.ParcelUuid;

import timber.log.Timber;

/**
 * Created by David Crotty on 02/08/2017.
 * <p>
 * Copyright © 2017 David Crotty - All Rights Reserved
 */

public class GATTClientCallback extends BluetoothGattCallback {

    private final MainActivity activity;
    private boolean connected = false;

    public GATTClientCallback(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if(newState == BluetoothProfile.STATE_CONNECTED) {
            Timber.d("CONNECTED");
            connected = true;
        } else {
            Timber.d("Not connected, status: " + status);
            connected = false;
        }

    }
}
