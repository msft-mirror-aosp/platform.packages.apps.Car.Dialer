/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.car.dialer.framework;

import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.car.arch.common.LiveDataFunctions;
import com.android.car.dialer.framework.testdata.CallLogDataHandler;
import com.android.car.dialer.framework.testdata.ContactDataHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manager for mocked bluetooth devices.
 */
@Singleton
public class FakeHfpManager {
    private static final String CONTACT_DATA_FILE = "ContactsForPhone%d.json";
    private static final String CALL_LOG_DATA_FILE = "CallLogForPhone%d.json";

    private Map<String, SimulatedBluetoothDevice> mDeviceMap = new HashMap<>();
    private List<BluetoothDevice> mDeviceList;
    private LiveData<Integer> mBluetoothStateLiveData;
    private LiveData<Set<BluetoothDevice>> mBluetoothPairListLiveData;
    private MutableLiveData<List<BluetoothDevice>> mHfpDeviceListLiveData;

    private final CallLogDataHandler mCallLogDataHandler;
    private final ContactDataHandler mContactDataHandler;

    @Inject
    public FakeHfpManager(CallLogDataHandler callLogDataHandler,
            ContactDataHandler contactDataHandler) {
        mCallLogDataHandler = callLogDataHandler;
        mContactDataHandler = contactDataHandler;

        mDeviceList = new ArrayList<>();
        mBluetoothStateLiveData = LiveDataFunctions.dataOf(/* enabled */2);
        mBluetoothPairListLiveData = LiveDataFunctions.dataOf(Collections.emptySet());
        mHfpDeviceListLiveData = LiveDataFunctions.dataOf(mDeviceList);
    }

    /**
     * Connects a mocked bluetooth device.
     */
    public void connectHfpDevice() {
        SimulatedBluetoothDevice device = prepareNewDevice();
        device.connect();
        mDeviceMap.put(String.valueOf(mDeviceMap.size()), device);
        mDeviceList.add(device.getBluetoothDevice());
        mHfpDeviceListLiveData.setValue(mDeviceList);
    }

    /**
     * Disconnects a mocked bluetooth device.
     */
    public void disconnectHfpDevice(String id) {
        SimulatedBluetoothDevice simulatedBluetoothDevice = mDeviceMap.remove(id);
        mDeviceList.remove(simulatedBluetoothDevice.getBluetoothDevice());
        simulatedBluetoothDevice.disconnect();
        mHfpDeviceListLiveData.setValue(mDeviceList);
    }

    private SimulatedBluetoothDevice prepareNewDevice() {
        String contactDataFile = String.format(CONTACT_DATA_FILE, mDeviceMap.size() + 1);
        String callLogDataFile = String.format(CALL_LOG_DATA_FILE, mDeviceMap.size() + 1);
        SimulatedBluetoothDevice simulatedBluetoothDevice = new SimulatedBluetoothDevice(
                mContactDataHandler, mCallLogDataHandler, contactDataFile, callLogDataFile);

        return simulatedBluetoothDevice;
    }

    public LiveData<Integer> getBluetoothStateLiveData() {
        return mBluetoothStateLiveData;
    }

    public LiveData<Set<BluetoothDevice>> getBluetoothPairListLiveData() {
        return mBluetoothPairListLiveData;
    }

    public LiveData<List<BluetoothDevice>> getHfpDeviceListLiveData() {
        return mHfpDeviceListLiveData;
    }
}
