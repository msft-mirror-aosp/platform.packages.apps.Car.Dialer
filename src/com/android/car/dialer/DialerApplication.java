/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.dialer;

import android.app.Application;

import com.android.car.dialer.bluetooth.CallHistoryManager;
import com.android.car.dialer.bluetooth.UiBluetoothMonitor;
import com.android.car.dialer.notification.InCallNotificationController;
import com.android.car.dialer.notification.MissedCallNotificationController;
import com.android.car.dialer.servicelocator.DialerServiceLocator;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.telephony.common.InMemoryPhoneBook;

import dagger.hilt.android.HiltAndroidApp;

/** Application for Dialer app. */
@HiltAndroidApp(Application.class)
public final class DialerApplication extends Hilt_DialerApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        DialerServiceLocator.get().init(this);
        InMemoryPhoneBook.init(this);
        UiCallManager.init(this);
        UiBluetoothMonitor.init(this);
        CallHistoryManager.init(this);
        InCallNotificationController.init(this);
        MissedCallNotificationController.init(this);
    }
}
