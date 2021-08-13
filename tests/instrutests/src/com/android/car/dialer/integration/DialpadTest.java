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

package com.android.car.dialer.integration;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.car.dialer.R;
import com.android.car.dialer.framework.FakeHfpManager;
import com.android.car.dialer.ui.TelecomActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
public class DialpadTest {
    @Rule
    public final HiltAndroidRule mHiltAndroidRule = new HiltAndroidRule(this);
    @Inject FakeHfpManager mFakeHfpManager;
    private Context mContext;

    @Before
    public void setup() {
        mHiltAndroidRule.inject();

        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ActivityScenario.launch(new Intent(mContext, TelecomActivity.class));
        mFakeHfpManager.connectHfpDevice(/* withMockData = */false);
    }

    @Test
    public void launchDialpad() {
        onView(withText(R.string.dialpad_title)).check(matches(isDisplayed())).perform(click());
        onView(withText("0")).check(matches(isDisplayed()));
        onView(withText("1")).check(matches(isDisplayed()));
        onView(withText("2")).check(matches(isDisplayed()));
        onView(withText("3")).check(matches(isDisplayed()));
        onView(withText("4")).check(matches(isDisplayed()));
        onView(withText("5")).check(matches(isDisplayed()));
        onView(withText("6")).check(matches(isDisplayed()));
        onView(withText("7")).check(matches(isDisplayed()));
        onView(withText("8")).check(matches(isDisplayed()));
        onView(withText("9")).check(matches(isDisplayed()));
        onView(withText("*")).check(matches(isDisplayed()));
        onView(withText("#")).check(matches(isDisplayed()));
    }
}
