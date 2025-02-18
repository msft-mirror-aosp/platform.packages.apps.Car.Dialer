/*
 * Copyright 2024 The Android Open Source Project
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

import android.telecom.Call;

import com.android.car.dialer.framework.InCallServiceProxy;
import com.android.car.telephony.calling.InCallModel;
import com.android.car.telephony.calling.InCallServiceManager;

import java.util.Comparator;
import java.util.List;

/**
 * Dialer specific implementation of {@link InCallModel}.
 */
public class DialerInCallModel extends InCallModel {

    public DialerInCallModel(InCallServiceManager inCallServiceManager,
                             Comparator<Call> callComparator) {
        super(inCallServiceManager, callComparator);
    }

    /**
     * To support the 'Fake' build variant of Dialer, {@link InCallServiceImpl#getCalls()} is
     * abstracted by {@link InCallServiceProxy#getCallList()}. InCallModel must use this abstraction
     * to ensure that the correct call list is returned for the current build variant.
     */
    @Override
    public final List<Call> getCallList() {
        return ((InCallServiceImpl) mInCallService).getCallList();
    }
}
