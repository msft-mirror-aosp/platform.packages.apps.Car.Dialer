/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.car.dialer.sms;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import com.android.car.assist.CarVoiceInteractionSession;
import com.android.car.telephony.common.TelecomUtils;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A transparent activity to show assistant to send sms and finish itself. This is used to handle
 * notification action where an activity object is not available.
 */
@AndroidEntryPoint(FragmentActivity.class)
public class DirectSendActivity extends Hilt_DirectSendActivity {

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTranslucent(true);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Add a 100ms delay so the ActivityRecord for this activity becomes visible to show assist.
        if (hasFocus) {
            new Handler().postDelayed(() -> {
                Intent intent = getIntent();
                if (intent != null) {
                    showAssist(intent.getExtras());
                    String phoneNumber =
                            intent.getStringExtra(CarVoiceInteractionSession.KEY_PHONE_NUMBER);
                    if (!TextUtils.isEmpty(phoneNumber)) {
                        TelecomUtils.markCallLogAsRead(getApplicationContext(), phoneNumber);
                    }
                }
                finish();
            }, 100);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        finish();
        return true;
    }
}
