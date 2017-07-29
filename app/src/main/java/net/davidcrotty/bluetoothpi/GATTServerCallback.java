package net.davidcrotty.bluetoothpi;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;

import timber.log.Timber;

/**
 * Created by David Crotty on 29/07/2017.
 * <p>
 * Copyright © 2017 David Crotty - All Rights Reserved
 */

public class GATTServerCallback extends AdvertiseCallback {
    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        super.onStartSuccess(settingsInEffect);
        Timber.d("onStartSuccess");
    }

    @Override
    public void onStartFailure(int errorCode) {
        super.onStartFailure(errorCode);
        Timber.d("onStartFailure " + errorCode);
    }
}
