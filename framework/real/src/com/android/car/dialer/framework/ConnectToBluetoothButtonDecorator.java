/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.car.content.pm.CarPackageManager;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.android.car.apps.common.UxrButton;
import com.android.car.apps.common.util.CarPackageManagerUtils;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ActivityContext;

/**
 * Sets up the "Connect to Bluetooth" uxr button in the no hfp error page.
 */
public class ConnectToBluetoothButtonDecorator implements UxrButtonDecorator {
    private final Context mContext;
    private CarPackageManager mCarPackageManager;

    @Inject
    ConnectToBluetoothButtonDecorator(
            @ActivityContext Context context, @Nullable CarPackageManager carPackageManager) {
        mContext = context;
        mCarPackageManager = carPackageManager;
    }

    @Override
    public void decorate(UxrButton button) {
        Intent launchIntent = new Intent();
        launchIntent.setAction("android.settings.BLUETOOTH_SETTINGS");
        launchIntent.addCategory("android.intent.category.DEFAULT");
        if (mCarPackageManager != null) {
            boolean isDistractionOptimized = CarPackageManagerUtils.isDistractionOptimized(
                    mCarPackageManager, mContext.getPackageManager(), launchIntent);
            button.setUxRestrictions(isDistractionOptimized
                    ? CarUxRestrictions.UX_RESTRICTIONS_BASELINE
                    : CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP);
        }
        button.setOnClickListener(v -> mContext.startActivity(launchIntent));
    }
}
