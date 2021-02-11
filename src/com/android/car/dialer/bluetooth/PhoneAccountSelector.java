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

package com.android.car.dialer.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.android.car.dialer.Constants;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/** Monitors the current hfp device and set it as the user selected outgoing phone account. */
@Singleton
public class PhoneAccountSelector {
    private final TelecomManager mTelecomManager;

    @Inject
    public PhoneAccountSelector(
            TelecomManager telecomManager,
            @Named("Hfp") LiveData<BluetoothDevice> currentHfpDeviceLiveData) {
        mTelecomManager = telecomManager;
        currentHfpDeviceLiveData.observeForever(device -> {
            PhoneAccountHandle phoneAccountHandle = getPhoneAccountHandleForDevice(device);
            mTelecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
        });
    }

    private PhoneAccountHandle getPhoneAccountHandleForDevice(
            @Nullable BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            return null;
        }

        List<PhoneAccountHandle> phoneAccountHandleList =
                mTelecomManager.getCallCapablePhoneAccounts();
        for (PhoneAccountHandle phoneAccountHandle : phoneAccountHandleList) {
            if (Constants.HFP_CLIENT_CONNECTION_SERVICE_CLASS_NAME.equals(
                    phoneAccountHandle.getComponentName().getClassName())) {
                if (TextUtils.equals(phoneAccountHandle.getId(), bluetoothDevice.getAddress())) {
                    return phoneAccountHandle;
                }
            }
        }
        return null;
    }
}
