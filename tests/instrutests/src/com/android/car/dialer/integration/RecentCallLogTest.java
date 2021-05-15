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

package com.android.car.dialer.integration;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.mockito.Mockito.when;

import android.content.Intent;

import androidx.lifecycle.MutableLiveData;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.car.dialer.R;
import com.android.car.dialer.bluetooth.CallHistoryManager;
import com.android.car.dialer.framework.FakeHfpManager;
import com.android.car.dialer.livedata.CallHistoryLiveData;
import com.android.car.dialer.ui.TelecomActivity;
import com.android.car.telephony.common.PhoneCallLog;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@SmallTest
@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
public class RecentCallLogTest {
    @Inject FakeHfpManager mFakeHfpManager;
    @BindValue @Mock CallHistoryManager mMockCallHistoryManager;
    @Mock PhoneCallLog mMockPhoneCallLog;
    @Mock PhoneCallLog.Record mRecord;

    @Rule
    public final HiltAndroidRule mHiltAndroidRule = new HiltAndroidRule(this);

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(RecentCallLogTest.this);
        MutableLiveData<List<PhoneCallLog>> callLogLiveData = new MutableLiveData<>();
        callLogLiveData.postValue(Collections.singletonList(mMockPhoneCallLog));
        when(mMockCallHistoryManager.getCallHistoryLiveData()).thenReturn(callLogLiveData);
        when(mRecord.getCallType()).thenReturn(CallHistoryLiveData.CallType.INCOMING_TYPE);
        when(mMockPhoneCallLog.getAllCallRecords()).thenReturn(Collections.singletonList(mRecord));

        mHiltAndroidRule.inject();
        mFakeHfpManager.connectHfpDevice();
    }

    @Test
    public void verifyRecentCallScreen() {
        when(mMockPhoneCallLog.getPhoneNumberString()).thenReturn("511");

        ActivityScenario.launch(
                new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        TelecomActivity.class));
        onView(withText(R.string.call_history_title)).check(matches(isDisplayed()));
        onView(withText("511")).check(matches(isDisplayed()));
        // TODO implement the test.
    }

    @Test
    public void emptyPhoneNumber_showAsUnknownCall() {
        when(mMockPhoneCallLog.getPhoneNumberString()).thenReturn("");

        ActivityScenario.launch(
                new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        TelecomActivity.class));
        // Verify there is no loading issue and the call is displayed as unknown calls.
        onView(withText("Unknown")).check(matches(isDisplayed()));
    }
}
