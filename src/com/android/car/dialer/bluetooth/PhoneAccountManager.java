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

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.util.Pair;

import com.android.car.apps.common.log.L;
import com.android.car.dialer.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/** User phone account handle management. */
@Singleton
public class PhoneAccountManager {
    private static final String TAG = "CD.PhoneAccountMgr";
    private final Context mContext;
    private final TelecomManager mTelecomManager;
    private final BluetoothAdapter mBluetoothAdapter;

    @Inject
    public PhoneAccountManager(
            @ApplicationContext Context context,
            TelecomManager telecomManager,
            @Nullable BluetoothAdapter bluetoothAdapter) {
        mContext = context;
        mTelecomManager = telecomManager;
        mBluetoothAdapter = bluetoothAdapter;
    }

    /**
     * Sets the {@link PhoneAccountHandle} associated with the {@link BluetoothDevice} as the
     * user selected outgoing phone account.
     */
    public void setUserSelectedOutgoingDevice(BluetoothDevice device) {
        PhoneAccountHandle phoneAccountHandle = getMatchingPhoneAccount(device);
        mTelecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
    }

    /**
     * Returns the {@link BluetoothDevice} for the given device address.
     */
    public BluetoothDevice getMatchingDevice(@Nullable String deviceId) {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            L.w(TAG, "Permission is denied to get paired bluetooth devices.");
            return null;
        }
        Set<BluetoothDevice> bondedDevices =
                mBluetoothAdapter == null ? null : mBluetoothAdapter.getBondedDevices();
        if (bondedDevices == null) {
            return null;
        }
        for (BluetoothDevice bluetoothDevice : bondedDevices) {
            if (TextUtils.equals(bluetoothDevice.getAddress(), deviceId)) {
                return bluetoothDevice;
            }
        }
        return null;
    }

    /**
     * Returns the {@link BluetoothDevice} for the given {@link PhoneAccountHandle} if the account
     * is for hfp connection.
     */
    public BluetoothDevice getMatchingDevice(
            @Nullable PhoneAccountHandle phoneAccountHandle) {
        if (isHfpConnectionService(phoneAccountHandle)) {
            return getMatchingDevice(phoneAccountHandle.getId());
        }
        return null;
    }

    /** Returns the list of hfp {@link BluetoothDevice}s for current callable phone accounts. */
    @NonNull
    public List<BluetoothDevice> getHfpDeviceList() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            L.w(TAG, "Permission is denied to get call capable phone accounts.");
            return Collections.EMPTY_LIST;
        }

        List<PhoneAccountHandle> phoneAccountHandles =
                mTelecomManager.getCallCapablePhoneAccounts(true);
        List<BluetoothDevice> hfpDeviceList = new ArrayList<>();
        for (PhoneAccountHandle phoneAccountHandle : phoneAccountHandles) {
            BluetoothDevice bluetoothDevice = getMatchingDevice(phoneAccountHandle);
            if (bluetoothDevice != null) {
                hfpDeviceList.add(bluetoothDevice);
            }
        }
        return hfpDeviceList;
    }

    /** Returns the {@link PhoneAccountHandle} for the given bluetooth device. */
    public PhoneAccountHandle getMatchingPhoneAccount(@Nullable BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            return null;
        }
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            L.w(TAG, "Permission is denied to get call capable phone accounts.");
            return null;
        }

        List<PhoneAccountHandle> phoneAccountHandleList =
                mTelecomManager.getCallCapablePhoneAccounts(/* includeDisabledAccounts= */true);
        for (PhoneAccountHandle phoneAccountHandle : phoneAccountHandleList) {
            if (isHfpConnectionService(phoneAccountHandle)) {
                if (TextUtils.equals(phoneAccountHandle.getId(), bluetoothDevice.getAddress())) {
                    return phoneAccountHandle;
                }
            }
        }
        return null;
    }

    /** Returns if the {@link PhoneAccountHandle} is from a hfp connection for bluetooth call. */
    public boolean isHfpConnectionService(@Nullable PhoneAccountHandle phoneAccountHandle) {
        if (phoneAccountHandle == null) {
            return false;
        }

        return Constants.HFP_CLIENT_CONNECTION_SERVICE_CLASS_NAME.equals(
                phoneAccountHandle.getComponentName().getClassName())
                || Constants.HFP_CLIENT_CONNECTION_SERVICE_CLASS_NAME_T.equals(
                phoneAccountHandle.getComponentName().getClassName());
    }

    /**
     * Get the calling app icon and app name. Bluetooth phone call will show Dialer's app icon and
     * app name.
     */
    public Pair<Drawable, CharSequence> getAppInfo(
            @Nullable PhoneAccountHandle phoneAccountHandle, boolean isSelfManaged) {
        PackageManager packageManager = mContext.getPackageManager();

        ApplicationInfo appInfo = mContext.getApplicationInfo();
        if (isSelfManaged) {
            if (phoneAccountHandle != null) {
                String packageName = phoneAccountHandle.getComponentName().getPackageName();
                try {
                    appInfo = packageManager.getApplicationInfo(
                            packageName, PackageManager.GET_META_DATA);
                } catch (PackageManager.NameNotFoundException e) {
                    L.e(TAG, e, "Failed to get self managed call app info.");
                }
            }
        }

        Drawable appIcon = packageManager.getApplicationIcon(appInfo);
        CharSequence appName = packageManager.getApplicationLabel(appInfo);
        return Pair.create(appIcon, appName);

    }

    /**
     * Get the launch intent for the calling app with the given {@link PhoneAccountHandle}.
     */
    @Nullable
    public Intent getLaunchIntent(PhoneAccountHandle phoneAccountHandle) {
        if (phoneAccountHandle == null) {
            return null;
        }
        String packageName = phoneAccountHandle.getComponentName().getPackageName();
        if (TextUtils.isEmpty(packageName)) {
            L.w(TAG, "Package not found.");
            return null;
        }
        return mContext.getPackageManager().getLaunchIntentForPackage(packageName);
    }
}
