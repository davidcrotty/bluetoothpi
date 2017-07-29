package net.davidcrotty.bluetoothpi;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.util.Log;

/**
 * Created by David Crotty on 29/07/2017.
 * <p>
 * Copyright © 2017 David Crotty - All Rights Reserved
 */

public class LEScanCallback extends ScanCallback {
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);
        Log.d("LEScanCallback", result.getDevice().getName());
    }
}
