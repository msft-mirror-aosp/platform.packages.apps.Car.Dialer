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

package com.android.car.dialer.telecom;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.Uri;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class UiCallManagerTest {

    private static final String TEL_SCHEME = "tel";

    private UiCallManager mUiCallManager;
    @Mock
    private TelecomManager mMockTelecomManager;
    @Mock
    private Context mMockContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mUiCallManager = new UiCallManager(mMockContext, mMockTelecomManager, null);
    }

    @Test
    public void testPlaceCall() {
        String[] phoneNumbers = {
                "6505551234", // US Number
                "511", // Special number
                "911", // Emergency number
                "122", // Emergency number
                "#77" // Emergency number
        };

        for (int i = 0; i < phoneNumbers.length; i++) {
            checkPlaceCall(phoneNumbers[i], i + 1);
        }
    }

    private void checkPlaceCall(String phoneNumber, int timesCalled) {
        ArgumentCaptor<Uri> uriCaptor = ArgumentCaptor.forClass(Uri.class);

        assertThat(mUiCallManager.placeCall(phoneNumber)).isTrue();
        verify(mMockTelecomManager, times(timesCalled)).placeCall(uriCaptor.capture(), isNull());
        assertThat(uriCaptor.getValue().getScheme()).isEqualTo(TEL_SCHEME);
        assertThat(uriCaptor.getValue().getSchemeSpecificPart()).isEqualTo(phoneNumber);
        assertThat(uriCaptor.getValue().getFragment()).isNull();
    }

    @Test
    public void noPhoneAccounts_emergencyCallNotSupported() {
        when(mMockTelecomManager.getCallCapablePhoneAccounts()).thenReturn(Collections.EMPTY_LIST);

        assertThat(mUiCallManager.isEmergencyCallSupported()).isFalse();
    }

    @Test
    public void hasPhoneAccountWithEmergencyCallCapability_emergencyCallSupported() {
        PhoneAccountHandle mockPhoneAccountHandle = mock(PhoneAccountHandle.class);
        PhoneAccount mockPhoneAccount = mock(PhoneAccount.class);

        when(mMockTelecomManager.getCallCapablePhoneAccounts()).thenReturn(
                Collections.singletonList(mockPhoneAccountHandle));
        when(mMockTelecomManager.getPhoneAccount(eq(mockPhoneAccountHandle))).thenReturn(
                mockPhoneAccount);
        when(mockPhoneAccount.hasCapabilities(
                eq(PhoneAccount.CAPABILITY_PLACE_EMERGENCY_CALLS))).thenReturn(true);

        assertThat(mUiCallManager.isEmergencyCallSupported()).isTrue();
    }

    @After
    public void tearDown() {
        mUiCallManager.tearDown();
    }
}
