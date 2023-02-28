/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.dialer.ui;

import android.telecom.Call;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.android.car.dialer.livedata.RefreshUiEvent;
import com.android.car.dialer.telecom.LocalCallHandler;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * View model for {@link TelecomActivity}.
 */
@HiltViewModel
public class TelecomActivityViewModel extends ViewModel {
    private final LocalCallHandler mLocalCallHandler;
    private final LiveData<Boolean> mHasHfpDeviceConnectedLiveData;

    private RefreshUiEvent mRefreshTabsLiveData;

    private final ToolbarTitleLiveData mToolbarTitleLiveData;
    private final MutableLiveData<Integer> mToolbarTitleMode;

    @Inject
    public TelecomActivityViewModel(
            @Named("Hfp") LiveData<Boolean> hasHfpDeviceConnectedLiveData,
            RefreshUiEvent refreshUiEvent,
            LocalCallHandler localCallHandler,
            ToolbarTitleLiveData toolbarTitleLiveData) {
        mLocalCallHandler = localCallHandler;
        mHasHfpDeviceConnectedLiveData = hasHfpDeviceConnectedLiveData;
        mToolbarTitleLiveData = toolbarTitleLiveData;
        mRefreshTabsLiveData = refreshUiEvent;

        mToolbarTitleMode = mToolbarTitleLiveData.getToolbarTitleModeLiveData();
    }

    /**
     * Returns the {@link LiveData} for the toolbar title, which provides the toolbar title
     * depending on the {@link com.android.car.dialer.R.attr#toolbarTitleMode}.
     */
    public LiveData<String> getToolbarTitle() {
        return mToolbarTitleLiveData;
    }

    /**
     * Returns the {@link MutableLiveData} of the toolbar title mode. The value should be set by the
     * {@link TelecomActivity}.
     */
    public MutableLiveData<Integer> getToolbarTitleMode() {
        return mToolbarTitleMode;
    }

    /**
     * Returns the live data which monitors whether to refresh Dialer.
     */
    public LiveData<Boolean> getRefreshTabsLiveData() {
        return mRefreshTabsLiveData;
    }

    /** Returns a {@link LiveData} which monitors if there are any connected HFP devices. */
    public LiveData<Boolean> hasHfpDeviceConnected() {
        return mHasHfpDeviceConnectedLiveData;
    }

    /** Returns the live data which monitors the ongoing call list. */
    public LiveData<List<Call>> getOngoingCallListLiveData() {
        return mLocalCallHandler.getOngoingCallListLiveData();
    }

    @Override
    protected void onCleared() {
        mLocalCallHandler.tearDown();
    }
}
