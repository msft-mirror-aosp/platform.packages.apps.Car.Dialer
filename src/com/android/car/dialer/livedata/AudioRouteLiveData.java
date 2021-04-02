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

package com.android.car.dialer.livedata;

import android.bluetooth.BluetoothHeadsetClient;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telecom.PhoneAccountHandle;

import androidx.lifecycle.MediatorLiveData;

import com.android.car.dialer.bluetooth.BluetoothHeadsetClientProvider;
import com.android.car.dialer.log.L;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.telephony.common.CallDetail;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Provides the current connecting audio route.
 */
public class AudioRouteLiveData extends MediatorLiveData<Integer> {
    private static final String TAG = "CD.AudioRouteLiveData";

    private final Context mContext;
    private final IntentFilter mAudioRouteChangeFilter;
    private final UiCallManager mUiCallManager;
    private final CallDetailLiveData mPrimaryCallDetailLiveData;

    private final BroadcastReceiver mAudioRouteChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateAudioRoute();
        }
    };

    @AssistedInject
    public AudioRouteLiveData(
            @ApplicationContext Context context,
            @Assisted CallDetailLiveData primaryCallDetailLiveData,
            BluetoothHeadsetClientProvider bluetoothHeadsetClientProvider,
            UiCallManager callManager) {
        mContext = context;
        mAudioRouteChangeFilter =
                new IntentFilter(BluetoothHeadsetClient.ACTION_AUDIO_STATE_CHANGED);
        mUiCallManager = callManager;
        mPrimaryCallDetailLiveData = primaryCallDetailLiveData;

        // TODO: introduce a new AudioStateChanged listener for listening to the audio state change.
        addSource(bluetoothHeadsetClientProvider.isBluetoothHeadsetClientConnected(),
                connected -> updateAudioRoute());
        addSource(mPrimaryCallDetailLiveData, this::updateOngoingCallAudioRoute);
    }

    @Override
    protected void onActive() {
        super.onActive();
        updateAudioRoute();
        mContext.registerReceiver(mAudioRouteChangeReceiver, mAudioRouteChangeFilter);
    }

    @Override
    protected void onInactive() {
        mContext.unregisterReceiver(mAudioRouteChangeReceiver);
        super.onInactive();
    }

    private void updateAudioRoute() {
        CallDetail primaryCallDetail = mPrimaryCallDetailLiveData.getValue();
        updateOngoingCallAudioRoute(primaryCallDetail);
    }

    private void updateOngoingCallAudioRoute(CallDetail callDetail) {
        if (callDetail == null) {
            // Phone call might have disconnected, no action.
            return;
        }
        PhoneAccountHandle phoneAccountHandle = callDetail.getPhoneAccountHandle();
        int audioRoute = mUiCallManager.getAudioRoute(phoneAccountHandle);
        if (getValue() == null || audioRoute != getValue()) {
            L.d(TAG, "updateAudioRoute to %s", audioRoute);
            setValue(audioRoute);
        }
    }

    /**
     * Factory to create {@link AudioRouteLiveData} instances via the {@link AssistedInject}
     * constructor.
     */
    @AssistedFactory
    public interface Factory {
        /** Creates an {@link AudioRouteLiveData} instance. */
        AudioRouteLiveData create(CallDetailLiveData primaryCallDetailLiveData);
    }
}
