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
import android.util.Log;

/**
 * Broadcast receiver for receiving Dialer debug commands from ADB.
 */
public class AdbBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "CD.ADBHandler";
    private static final String INTENT_ACTION = "com.android.car.dialer.intent.action.adb";
    private static final String ACTION_TAG = "action";

    /**
     * Registers this class to a context
     */
    public void registerReceiver(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_ACTION);
        context.registerReceiver(this, filter);
    }

    /**
     * Unregisters this class from the context
     */
    public void unregisterReceiver(Context context) {
        context.unregisterReceiver(this);
    }

    /**
     * Handles received broadcast intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra(ACTION_TAG);

        switch(action) {
            default:
                Log.d(TAG, "Unknown command " + action);
        }
    }
}
