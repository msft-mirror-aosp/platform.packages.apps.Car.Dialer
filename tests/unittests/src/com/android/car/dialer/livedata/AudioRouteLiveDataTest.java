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

package com.android.car.dialer.livedata;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothHeadsetClient;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.telecom.CallAudioState;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.dialer.bluetooth.BluetoothHeadsetClientProvider;
import com.android.car.dialer.telecom.UiCallManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class AudioRouteLiveDataTest {
    private AudioRouteLiveData mAudioRouteLiveData;
    private MutableLiveData<Boolean> mBluetoothConnectedLiveData;

    @Mock
    private Context mMockContext;

    @Mock
    private Observer<Integer> mMockObserver;
    @Mock
    private UiCallManager mMockUiCallManager;
    @Mock
    private BluetoothHeadsetClientProvider mMockHeadsetClientProvider;

    @Captor
    private ArgumentCaptor<IntentFilter> mIntentFilterCaptor;
    @Captor
    private ArgumentCaptor<BroadcastReceiver> mReceiverCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mMockUiCallManager.getAudioRoute(any())).thenReturn(CallAudioState.ROUTE_EARPIECE);
        mBluetoothConnectedLiveData = new MutableLiveData<>();
        when(mMockHeadsetClientProvider.isBluetoothHeadsetClientConnected()).thenReturn(
                mBluetoothConnectedLiveData);
        mAudioRouteLiveData = new AudioRouteLiveData(
                mMockContext, new CallDetailLiveData(), mMockHeadsetClientProvider,
                mMockUiCallManager);
    }

    @Test
    @UiThreadTest
    public void testOnActive() {
        verify(mMockObserver, never()).onChanged(any());
        mAudioRouteLiveData.observeForever(mMockObserver);
        verify(mMockContext).registerReceiver(any(), mIntentFilterCaptor.capture());

        assertThat(mIntentFilterCaptor.getValue().getAction(0))
                .isEqualTo(BluetoothHeadsetClient.ACTION_AUDIO_STATE_CHANGED);
        verify(mMockObserver).onChanged(CallAudioState.ROUTE_EARPIECE);
    }

    @Test
    @UiThreadTest
    public void testBluetoothHfpStateChanged_changeToBluetoothRoute() {
        mAudioRouteLiveData.observeForever(mMockObserver);
        assertThat(mAudioRouteLiveData.getValue()).isEqualTo(CallAudioState.ROUTE_EARPIECE);

        when(mMockUiCallManager.getAudioRoute(any())).thenReturn(CallAudioState.ROUTE_BLUETOOTH);
        mBluetoothConnectedLiveData.setValue(Boolean.TRUE);

        verify(mMockObserver).onChanged(CallAudioState.ROUTE_BLUETOOTH);
        assertThat(mAudioRouteLiveData.getValue()).isEqualTo(CallAudioState.ROUTE_BLUETOOTH);
    }

    @Test
    @UiThreadTest
    public void testOnInactive() {
        verify(mMockObserver, never()).onChanged(any());
        mAudioRouteLiveData.observeForever(mMockObserver);
        verify(mMockContext).registerReceiver(mReceiverCaptor.capture(), any());

        mAudioRouteLiveData.removeObserver(mMockObserver);
        verify(mMockContext).unregisterReceiver(mReceiverCaptor.getValue());
    }
}
