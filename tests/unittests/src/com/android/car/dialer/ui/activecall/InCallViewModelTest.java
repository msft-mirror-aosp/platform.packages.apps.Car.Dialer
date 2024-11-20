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

package com.android.car.dialer.ui.activecall;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.net.Uri;
import android.telecom.Call;
import android.telecom.DisconnectCause;
import android.telecom.GatewayInfo;

import androidx.core.util.Pair;
import androidx.lifecycle.MutableLiveData;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.dialer.telecom.DialerInCallModel;
import com.android.car.dialer.telecom.InCallServiceImpl;
import com.android.car.telephony.calling.AudioRouteLiveData;
import com.android.car.telephony.calling.CallComparator;
import com.android.car.telephony.calling.InCallServiceManager;
import com.android.car.telephony.calling.SupportedAudioRoutesLiveData;
import com.android.car.telephony.common.CallDetail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class InCallViewModelTest {
    private static final String NUMBER = "6505551234";
    private static final long CONNECT_TIME_MILLIS = 500000000;
    private static final CharSequence LABEL = "DisconnectCause";
    private static final Uri GATEWAY_ADDRESS = Uri.fromParts("tel", NUMBER, null);

    private InCallViewModel mInCallViewModel;

    @Mock
    private Call mMockActiveCall;
    @Mock
    private Call.Details mMockActiveCallDetails;
    @Mock
    private Call mMockDialingCall;
    @Mock
    private Call.Details mMockDialingCallDetails;
    @Mock
    private Call mMockHoldingCall;
    @Mock
    private Call.Details mMockHoldingCallDetails;
    @Mock
    private Call mMockRingingCall;
    @Mock
    private Call.Details mMockRingingCallDetails;
    @Mock
    private AudioRouteLiveData.Factory mMockAudioRouteLiveDataFactory;
    @Mock
    private SupportedAudioRoutesLiveData.Factory mMockSupportedAudioRoutesLiveDataFactory;
    private DialerInCallModel mInCallModel;
    @Mock
    private InCallServiceImpl mMockInCallService;
    private InCallServiceManager mInCallServiceManager;

    @Before
    @UiThreadTest
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mInCallServiceManager = new InCallServiceManager();
        mInCallServiceManager.setInCallService(mMockInCallService);

        when(mMockActiveCallDetails.getState()).thenReturn(Call.STATE_ACTIVE);
        when(mMockActiveCall.getDetails()).thenReturn(mMockActiveCallDetails);
        when(mMockDialingCallDetails.getState()).thenReturn(Call.STATE_DIALING);
        when(mMockDialingCall.getDetails()).thenReturn(mMockDialingCallDetails);
        when(mMockHoldingCallDetails.getState()).thenReturn(Call.STATE_HOLDING);
        when(mMockHoldingCall.getDetails()).thenReturn(mMockHoldingCallDetails);
        when(mMockRingingCallDetails.getState()).thenReturn(Call.STATE_RINGING);
        when(mMockRingingCall.getDetails()).thenReturn(mMockRingingCallDetails);

        // Set up call details
        GatewayInfo gatewayInfo = new GatewayInfo("", GATEWAY_ADDRESS, GATEWAY_ADDRESS);
        DisconnectCause disconnectCause = new DisconnectCause(1, LABEL, null, "");
        when(mMockDialingCallDetails.getHandle()).thenReturn(GATEWAY_ADDRESS);
        when(mMockDialingCallDetails.getDisconnectCause()).thenReturn(disconnectCause);
        when(mMockDialingCallDetails.getGatewayInfo()).thenReturn(gatewayInfo);
        when(mMockDialingCallDetails.getConnectTimeMillis()).thenReturn(CONNECT_TIME_MILLIS);

        when(mMockAudioRouteLiveDataFactory.create(any(), any())).thenReturn(
                mock(AudioRouteLiveData.class));
        when(mMockSupportedAudioRoutesLiveDataFactory.create(any())).thenReturn(
                mock(SupportedAudioRoutesLiveData.class));

        when(mMockInCallService.getCallList()).thenReturn(Arrays.asList(
                        mMockRingingCall, mMockDialingCall, mMockActiveCall, mMockHoldingCall));

        mInCallModel = new DialerInCallModel(mInCallServiceManager, new CallComparator());
        mInCallViewModel = new InCallViewModel(mInCallModel,
                mMockAudioRouteLiveDataFactory, mMockSupportedAudioRoutesLiveDataFactory,
                Executors.newSingleThreadExecutor(), new MutableLiveData<>());
        mInCallViewModel.getIncomingCall().observeForever(s -> { });
        mInCallViewModel.getOngoingCallList().observeForever(s -> { });
        mInCallViewModel.getPrimaryCall().observeForever(s -> { });
        mInCallViewModel.getPrimaryCallState().observeForever(s -> { });
        mInCallViewModel.getPrimaryCallDetail().observeForever(s -> { });
        mInCallViewModel.getCallStateAndConnectTime().observeForever(s -> { });
        mInCallViewModel.getAudioRoute().observeForever(s -> { });
    }

    @Test
    @UiThreadTest
    public void testGetCallList() {
        List<Call> callListInOrder =
                Arrays.asList(mMockDialingCall, mMockActiveCall, mMockHoldingCall);
        List<Call> viewModelCallList = mInCallViewModel.getOngoingCallList().getValue();
        assertArrayEquals(callListInOrder.toArray(), viewModelCallList.toArray());
    }

    @Test
    @UiThreadTest
    public void testStateChange_triggerCallListUpdate() {
        when(mMockActiveCallDetails.getState()).thenReturn(Call.STATE_HOLDING);
        when(mMockHoldingCallDetails.getState()).thenReturn(Call.STATE_ACTIVE);
        mInCallModel.onStateChanged(mMockActiveCall, Call.STATE_HOLDING);

        List<Call> callListInOrder =
                Arrays.asList(mMockDialingCall, mMockHoldingCall, mMockActiveCall);
        List<Call> viewModelCallList = mInCallViewModel.getOngoingCallList().getValue();
        assertArrayEquals(callListInOrder.toArray(), viewModelCallList.toArray());
    }

    @Test
    @UiThreadTest
    public void testGetIncomingCall() {
        Call incomingCall = mInCallViewModel.getIncomingCall().getValue();
        assertThat(incomingCall).isEqualTo(mMockRingingCall);
    }

    @Test
    @UiThreadTest
    public void testGetPrimaryCall() {
        assertThat(mInCallViewModel.getPrimaryCall().getValue()).isEqualTo(mMockDialingCall);
    }

    @Test
    @UiThreadTest
    public void testGetPrimaryCallState() {
        assertThat(mInCallViewModel.getPrimaryCallState().getValue()).isEqualTo(Call.STATE_DIALING);
    }

    @Test
    @UiThreadTest
    public void testGetPrimaryCallDetail() {
        CallDetail callDetail = mInCallViewModel.getPrimaryCallDetail().getValue();
        assertThat(callDetail.getNumber()).isEqualTo(NUMBER);
        assertThat(callDetail.getConnectTimeMillis()).isEqualTo(CONNECT_TIME_MILLIS);
        assertThat(callDetail.getDisconnectCause()).isEqualTo(LABEL);
        assertThat(callDetail.getGatewayInfoOriginalAddress()).isEqualTo(GATEWAY_ADDRESS);
    }

    @Test
    @UiThreadTest
    public void testGetCallStateAndConnectTime() {
        Pair<Integer, Long> pair = mInCallViewModel.getCallStateAndConnectTime().getValue();
        assertThat(pair.first).isEqualTo(Call.STATE_DIALING);
        assertThat(pair.second).isEqualTo(CONNECT_TIME_MILLIS);
    }
}
