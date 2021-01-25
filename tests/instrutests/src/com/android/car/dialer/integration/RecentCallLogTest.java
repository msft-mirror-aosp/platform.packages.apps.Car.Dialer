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

import static org.mockito.Mockito.mock;

import android.bluetooth.BluetoothDevice;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import com.android.car.dialer.R;
import com.android.car.dialer.framework.FakeBluetoothAdapter;
import com.android.car.dialer.ui.TelecomActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@SmallTest
@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
public class RecentCallLogTest {
    @Inject
    FakeBluetoothAdapter mFakeBluetoothAdapter;

    @Rule
    public final HiltAndroidRule mHiltAndroidRule = new HiltAndroidRule(this);
    @Rule
    public final ActivityTestRule<TelecomActivity> mActivityTestRule =
            new ActivityTestRule<TelecomActivity>(TelecomActivity.class) {
                @Override
                protected void afterActivityLaunched() {
                    super.afterActivityLaunched();
                    mHiltAndroidRule.inject();
                    mFakeBluetoothAdapter.connectHfpDevice(mock(BluetoothDevice.class));
                }
            };

    @Test
    public void verifyRecentCallScreen() {
        onView(withText(R.string.call_history_title)).check(matches(isDisplayed()));
        // TODO implement the test.
    }
}
