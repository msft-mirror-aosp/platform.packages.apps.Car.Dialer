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

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.telecom.Call;

import com.android.car.telephony.calling.SimpleInCallServiceImpl;

import java.util.List;

/**
 * Fake implementation of InCallServiceProxy.
 */
public abstract class InCallServiceProxy extends SimpleInCallServiceImpl {
    private static final String ACTION_PROXY_BIND = "proxy_bind";
    private List<Call> mCallList;

    /**
     * Returns the mocked call list.
     *
     * This function acts as a wrapper and replaces the usage of InCallService.getCalls() in the
     * fake implementation. The mocked call list is self managed.
     */
    public final List<Call> getCallList() {
        return mCallList;
    }

    void setCallList(List<Call> callList) {
        mCallList = callList;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return ACTION_PROXY_BIND.equals(intent.getAction())
                ? new LocalBinder()
                : super.onBind(intent);
    }

    /**
     * Local binder so fakes can bind and access InCallService APIs.
     */
    public class LocalBinder extends Binder {

        /**
         * Returns a reference to {@link InCallServiceProxy}.
         */
        public InCallServiceProxy getService() {
            return InCallServiceProxy.this;
        }
    }
}
