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

package com.android.car.dialer.framework;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

import com.android.car.apps.common.log.L;
import com.android.car.dialer.framework.testdata.ContactRawData;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Broadcast receiver for receiving Dialer debug commands from ADB.
 */
@Singleton
public class AdbBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "CD.ADBHandler";
    private static final String ACTION_PREFIX = "com.android.car.dialer.intent.action";
    // adb shell am broadcast -a com.android.car.dialer.intent.action.connect
    private static final String ACTION_CONNECT = ACTION_PREFIX + ".connect";
    // adb shell am broadcast -a com.android.car.dialer.intent.action.disconnect
    // adb shell am broadcast -a com.android.car.dialer.intent.action.disconnect --es device_id 0
    private static final String ACTION_DISCONNECT = ACTION_PREFIX + ".disconnect";
    private static final String ACTION_ADDCALL = ACTION_PREFIX + ".addCall";
    private static final String ACTION_RECEIVECALL = ACTION_PREFIX + ".rcvCall";
    private static final String ACTION_ENDCALL = ACTION_PREFIX + ".endCall";
    private static final String ACTION_CLEARALL = ACTION_PREFIX + ".clearAll";
    private static final String ACTION_MERGE = ACTION_PREFIX + ".merge";
    // adb shell am broadcast -a com.android.car.dialer.intent.action.addContact --es name \
    // Contact --number 511 --address "100\ Hello\ Street,\ World,\ CA"
    private static final String ACTION_ADD_CONTACT = ACTION_PREFIX + ".addContact";
    private static final String EXTRA_CALL_ID = "id";
    private static final String EXTRA_DEVICE_ID = "device_id";
    private static final String EXTRA_CONTACT_NAME = "name";
    private static final String EXTRA_PHONE_NUMBER = "number";
    private static final String EXTRA_PHONE_LABEL = "number_label";
    private static final String EXTRA_ADDRESS = "address";
    private static final String EXTRA_ADDRESS_LABEL = "address_label";

    private final FakeTelecomManager mFakeTelecomManager;
    private final FakeHfpManager mFakeHfpManager;

    @Inject
    AdbBroadcastReceiver(FakeTelecomManager fakeTelecomManager, FakeHfpManager fakeHfpManager) {
        mFakeTelecomManager = fakeTelecomManager;
        mFakeHfpManager = fakeHfpManager;
    }

    /**
     * Registers this class to an application context
     */
    public void registerReceiver(@ApplicationContext Context context) {
        Log.d(TAG, "Registered to " + context);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CONNECT);
        filter.addAction(ACTION_DISCONNECT);
        filter.addAction(ACTION_ADDCALL);
        filter.addAction(ACTION_RECEIVECALL);
        filter.addAction(ACTION_ENDCALL);
        filter.addAction(ACTION_CLEARALL);
        filter.addAction(ACTION_MERGE);
        filter.addAction(ACTION_ADD_CONTACT);
        context.registerReceiver(this, filter);
    }

    /**
     * Unregisters this class from the application context
     */
    public void unregisterReceiver(@ApplicationContext Context context) {
        Log.d(TAG, "Unregistered from " + context);
        context.unregisterReceiver(this);
    }

    /**
     * Handles received broadcast intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String id;
        String action = intent.getAction();

        switch (action) {
            case ACTION_ADDCALL:
                id = intent.getStringExtra(EXTRA_CALL_ID);
                Log.d(TAG, action + id);
                mFakeTelecomManager.placeCall(id);
                break;
            case ACTION_ENDCALL:
                id = intent.getStringExtra(EXTRA_CALL_ID);
                Log.d(TAG, action + id);
                mFakeTelecomManager.endCall(id);
                break;
            case ACTION_RECEIVECALL:
                id = intent.getStringExtra(EXTRA_CALL_ID);
                Log.d(TAG, action + id);
                mFakeTelecomManager.receiveCall(id);
                break;
            case ACTION_CLEARALL:
                Log.d(TAG, action);
                mFakeTelecomManager.clearCalls();
                break;
            case ACTION_MERGE:
                Log.d(TAG, action);
                mFakeTelecomManager.mergeCalls();
                break;
            case ACTION_CONNECT:
                Log.d(TAG, action);
                mFakeHfpManager.connectHfpDevice(/* withMockData= */true);
                break;
            case ACTION_DISCONNECT:
                id = intent.getStringExtra(EXTRA_DEVICE_ID);
                Log.d(TAG, action + id);
                mFakeHfpManager.disconnectHfpDevice(id);
                break;
            case ACTION_ADD_CONTACT:
                id = intent.getStringExtra(EXTRA_DEVICE_ID);
                SimulatedBluetoothDevice device = mFakeHfpManager.getHfpDevice(id);
                if (device != null) {
                    String contactName = intent.getStringExtra(EXTRA_CONTACT_NAME);
                    String phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER);
                    String phoneLabel = intent.getStringExtra(EXTRA_PHONE_LABEL);
                    String address = intent.getStringExtra(EXTRA_ADDRESS);
                    String addressLabel = intent.getStringExtra(EXTRA_ADDRESS_LABEL);
                    if (TextUtils.isEmpty(contactName)
                            && TextUtils.isEmpty(phoneNumber)
                            && TextUtils.isEmpty(address)) {
                        L.e(TAG, "Invalid contact raw data.");
                    } else {
                        ContactRawData contactRawData = new ContactRawData();
                        contactRawData.setDisplayName(contactName);
                        contactRawData.setNumber(phoneNumber);
                        contactRawData.setNumberLabel(phoneLabel);
                        contactRawData.setAddress(address);
                        contactRawData.setAddressLabel(addressLabel);
                        device.insertContactInBackground(contactRawData);
                    }
                }
                break;
            default:
                Log.d(TAG, "Unknown command " + action);
        }
    }
}
