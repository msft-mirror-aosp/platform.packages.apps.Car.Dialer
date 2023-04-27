/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.car.dialer.livedata;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.android.car.apps.common.log.L;
import com.android.car.dialer.ui.common.SingleLiveEvent;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.hilt.android.scopes.ActivityRetainedScoped;

/**
 * This is an event live data to determine if the Ui needs to be refreshed.
 */
@ActivityRetainedScoped
public class RefreshUiEvent extends SingleLiveEvent<Boolean> {
    private static final String TAG = "CD.RefreshUiEvent";
    private LiveData<BluetoothDevice> mCurrentHfpDevice;
    private BluetoothDevice mBluetoothDevice;

    @Inject
    RefreshUiEvent(
            @Named("Hfp") LiveData<List<BluetoothDevice>> hfpDeviceListLiveData,
            @Named("Hfp") LiveData<BluetoothDevice> currentHfpDevice) {
        mCurrentHfpDevice = currentHfpDevice;
        addSource(hfpDeviceListLiveData, v -> update(v));
        observeForever(event -> L.i(TAG, "Refresh ui event triggered."));
    }

    private void update(List<BluetoothDevice> hfpDeviceList) {
        L.v(TAG, "HfpDeviceList update");
        if (mBluetoothDevice != null && !listContainsDevice(hfpDeviceList, mBluetoothDevice)) {
            setValue(true);
        }
        mBluetoothDevice = mCurrentHfpDevice.getValue();
    }

    private boolean listContainsDevice(@Nullable List<BluetoothDevice> hfpDeviceList,
                                       @NonNull BluetoothDevice device) {
        if (hfpDeviceList != null && hfpDeviceList.contains(device)) {
            return true;
        }

        return false;
    }
}
