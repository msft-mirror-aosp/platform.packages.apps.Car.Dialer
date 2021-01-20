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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.android.car.dialer.CarDialerRobolectricTestRunner;
import com.android.car.dialer.R;
import com.android.car.dialer.bluetooth.BluetoothState;
import com.android.car.dialer.testutils.ShadowBluetoothAdapterForDialer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@RunWith(CarDialerRobolectricTestRunner.class)
@Config(shadows = ShadowBluetoothAdapterForDialer.class)
public class BluetoothErrorStringLiveDataTest {

    private Context mContext;
    private MutableLiveData<List<BluetoothDevice>> mHfpDeviceListLiveData;
    private MutableLiveData<Set<BluetoothDevice>> mPairedListLiveData;
    private MutableLiveData<Integer> mBluetoothStateLiveData;
    private BluetoothErrorStringLiveData mBluetoothErrorStringLiveData;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testDialerAppState_defaultBluetoothAdapterIsNull_bluetoothError() {
        ShadowBluetoothAdapterForDialer.setBluetoothAvailable(false);

        initializeBluetoothErrorStringLiveData();

        assertThat(mBluetoothErrorStringLiveData.getValue()).isEqualTo(
                mContext.getString(R.string.bluetooth_unavailable));
    }

    @Test
    public void testDialerAppState_bluetoothNotEnabled_bluetoothError() {
        ShadowBluetoothAdapterForDialer.setBluetoothAvailable(true);
        mBluetoothStateLiveData.setValue(BluetoothState.DISABLED);

        initializeBluetoothErrorStringLiveData();

        assertThat(mBluetoothErrorStringLiveData.getValue()).isEqualTo(
                mContext.getString(R.string.bluetooth_disabled));
    }

    @Test
    public void testDialerAppState_noPairedDevices_bluetoothError() {
        ShadowBluetoothAdapterForDialer.setBluetoothAvailable(true);
        mBluetoothStateLiveData.setValue(BluetoothState.ENABLED);
        mPairedListLiveData.setValue(Collections.emptySet());

        initializeBluetoothErrorStringLiveData();

        assertThat(mBluetoothErrorStringLiveData.getValue()).isEqualTo(
                mContext.getString(R.string.bluetooth_unpaired));
    }

    @Test
    public void testDialerAppState_hfpNoConnected_bluetoothError() {
        ShadowBluetoothAdapterForDialer.setBluetoothAvailable(true);
        mBluetoothStateLiveData.setValue(BluetoothState.ENABLED);
        BluetoothDevice mockBluetoothDevice = mock(BluetoothDevice.class);
        mPairedListLiveData.setValue(Collections.singleton(mockBluetoothDevice));
        mHfpDeviceListLiveData.setValue(Collections.emptyList());

        initializeBluetoothErrorStringLiveData();

        assertThat(mBluetoothErrorStringLiveData.getValue()).isEqualTo(
                mContext.getString(R.string.no_hfp));
    }

    @Test
    public void testDialerAppState_bluetoothAllSet_dialerAppNoError() {
        ShadowBluetoothAdapterForDialer.setBluetoothAvailable(true);
        mBluetoothStateLiveData.setValue(BluetoothState.ENABLED);
        BluetoothDevice mockBluetoothDevice = mock(BluetoothDevice.class);
        mPairedListLiveData.setValue(Collections.singleton(mockBluetoothDevice));
        mHfpDeviceListLiveData.setValue(Collections.singletonList(mockBluetoothDevice));

        initializeBluetoothErrorStringLiveData();

        assertThat(mBluetoothErrorStringLiveData.getValue()).isEqualTo(
                BluetoothErrorStringLiveData.NO_BT_ERROR);
    }

    private void initializeBluetoothErrorStringLiveData() {
        mBluetoothErrorStringLiveData = new BluetoothErrorStringLiveData(mContext,
                mHfpDeviceListLiveData, mPairedListLiveData, mBluetoothStateLiveData,
                BluetoothAdapter.getDefaultAdapter());
        // Observers needed so that the liveData's internal initialization is triggered
        mBluetoothStateLiveData.observeForever(error -> {
        });
    }
}
