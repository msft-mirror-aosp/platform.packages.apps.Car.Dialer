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

package com.android.car.dialer.framework;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.telecom.TelecomManager;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * A fake TelecomManager implementation
 */
@Singleton
public class FakeTelecomManager {
    private static final String TAG = "CD.FakeTelecomManager";

    private TelecomManager mSpiedTelecomManager;

    @Inject
    public FakeTelecomManager(@ApplicationContext Context context) {
        Log.d(TAG, "Create FakeTelecomManager");

        mSpiedTelecomManager = spy(context.getSystemService(TelecomManager.class));
    }

    /**
     * Gets a fake TelecomManager.
     */
    public TelecomManager getTelecomManager() {
        return mSpiedTelecomManager;
    }
}