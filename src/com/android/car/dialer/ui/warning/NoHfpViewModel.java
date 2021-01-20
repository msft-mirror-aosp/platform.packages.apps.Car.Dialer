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

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.android.car.dialer.ComponentFetcher;
import com.android.car.dialer.bluetooth.UiBluetoothMonitor;
import com.android.car.dialer.inject.ViewModelComponent;

import javax.inject.Inject;

/** View model for {@link NoHfpFragment} */
public class NoHfpViewModel extends AndroidViewModel {

    @Inject UiBluetoothMonitor mUiBluetoothMonitor;
    @Inject BluetoothErrorStringLiveData mBluetoothErrorStringLiveData;
    private final LiveData<Boolean> mHasHfpDeviceConnectedLiveData;

    public NoHfpViewModel(@NonNull Application application) {
        super(application);
        ComponentFetcher.from(application, ViewModelComponent.class).inject(this);
        mHasHfpDeviceConnectedLiveData = mUiBluetoothMonitor.hasHfpDeviceConnected();
    }

    public LiveData<String> getBluetoothErrorStringLiveData() {
        return mBluetoothErrorStringLiveData;
    }

    /** Returns a {@link LiveData} which monitors if there are any connected HFP devices. */
    public LiveData<Boolean> hasHfpDeviceConnected() {
        return mHasHfpDeviceConnectedLiveData;
    }
}
