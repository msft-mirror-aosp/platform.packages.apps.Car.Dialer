/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.dialer.ui.warning;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.android.car.apps.common.log.L;
import com.android.car.dialer.R;
import com.android.car.dialer.bluetooth.BluetoothState;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.hilt.android.scopes.ViewModelScoped;

/**
 * A {@link androidx.lifecycle.LiveData<String>} that has a string describing the current bluetooth
 * error. If there is no error, its value will be {@link #NO_BT_ERROR}.
 */
@ViewModelScoped
public class BluetoothErrorStringLiveData extends MediatorLiveData<Integer> {
    private static final String TAG = "CD.BluetoothErrorStringLiveData";

    public static final Integer NO_BT_ERROR = Integer.MIN_VALUE;

    private LiveData<List<BluetoothDevice>> mHfpDeviceListLiveData;
    private LiveData<Set<BluetoothDevice>> mPairListLiveData;
    private LiveData<Integer> mBluetoothStateLiveData;

    @Inject
    BluetoothErrorStringLiveData(
            @Named("Hfp") LiveData<List<BluetoothDevice>> hfpDeviceListLiveData,
            @Named("Bluetooth") LiveData<Set<BluetoothDevice>> pairListLiveData,
            @Named("Bluetooth") LiveData<Integer> bluetoothStateLiveData,
            @Nullable BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter == null) {
            setValue(R.string.bluetooth_unavailable);
        } else {
            setValue(NO_BT_ERROR);
            mHfpDeviceListLiveData = hfpDeviceListLiveData;
            mPairListLiveData = pairListLiveData;
            mBluetoothStateLiveData = bluetoothStateLiveData;

            addSource(mHfpDeviceListLiveData, this::onHfpDevicesChanged);
            addSource(mPairListLiveData, this::onPairListChanged);
            addSource(mBluetoothStateLiveData, this::onBluetoothStateChanged);
        }
    }

    private void onHfpDevicesChanged(List<BluetoothDevice> bluetoothDevices) {
        update();
    }

    private void onPairListChanged(Set<BluetoothDevice> pairedDevices) {
        update();
    }

    private void onBluetoothStateChanged(Integer state) {
        update();
    }

    private void update() {
        boolean isBluetoothEnabled = isBluetoothEnabled();
        boolean hasPairedDevices = hasPairedDevices();
        boolean isHfpConnected = isHfpConnected();
        L.d(TAG, "Update error string."
                        + " isBluetoothEnabled: %s"
                        + " hasPairedDevices: %s"
                        + " isHfpConnected: %s",
                isBluetoothEnabled,
                hasPairedDevices,
                isHfpConnected);
        if (isHfpConnected) {
            if (!NO_BT_ERROR.equals(getValue())) {
                setValue(NO_BT_ERROR);
            }
        } else if (!isBluetoothEnabled) {
            setValue(R.string.bluetooth_disabled);
        } else if (!hasPairedDevices) {
            setValue(R.string.bluetooth_unpaired);
        } else {
            setValue(R.string.no_hfp);
        }
    }

    private boolean isHfpConnected() {
        List<BluetoothDevice> mHfpDeviceList = mHfpDeviceListLiveData.getValue();
        return mHfpDeviceList != null && !mHfpDeviceList.isEmpty();
    }

    private boolean isBluetoothEnabled() {
        Integer bluetoothState = mBluetoothStateLiveData.getValue();
        return bluetoothState == null || bluetoothState != BluetoothState.DISABLED;
    }

    private boolean hasPairedDevices() {
        Set<BluetoothDevice> pairedDevices = mPairListLiveData.getValue();
        return pairedDevices == null || !pairedDevices.isEmpty();
    }
}
