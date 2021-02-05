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

package com.android.car.dialer.ui.dialpad;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.android.car.dialer.ComponentFetcher;
import com.android.car.dialer.inject.ViewModelComponent;
import com.android.car.dialer.ui.common.ContactResultsLiveData;
import com.android.car.dialer.ui.search.ContactResultsViewModel;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {link AndroidViewModel} used for type down functionality.
 */
public class TypeDownResultsViewModel extends ContactResultsViewModel {

    @Inject @Named("Hfp") LiveData<BluetoothDevice> mCurrentHfpDeviceLiveData;
    private final ContactResultsLiveData mContactSearchResultsLiveData;

    public TypeDownResultsViewModel(@NonNull Application application) {
        super(application);
        ComponentFetcher.from(application, ViewModelComponent.class).inject(this);
        mContactSearchResultsLiveData = new ContactResultsLiveData(application,
                getSearchQueryLiveData(),
                mCurrentHfpDeviceLiveData,
                getSharedPreferencesLiveData(),
                /* showOnlyOneEntry */ false);
    }

    @Override
    public LiveData<List<ContactResultsLiveData.ContactResultListItem>> getContactSearchResults() {
        return mContactSearchResultsLiveData;
    }
}
