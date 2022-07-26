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

package com.android.car.dialer.telecom;

import android.car.drivingstate.CarUxRestrictions;
import android.telecom.Call;

import androidx.annotation.NonNull;

import com.android.car.ui.utils.CarUxRestrictionsUtil;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**Handles the driving state change and show incall ui for self managed calls when start driving. */
public class SelfManagedCallHandler implements InCallServiceImpl.ActiveCallListChangedCallback,
        CarUxRestrictionsUtil.OnUxRestrictionsChangedListener {
    private final CarUxRestrictionsUtil mCarUxRestrctionsUtil;
    private final List<Call> mSelfManagedCallList;
    private final InCallRouter mInCallRouter;
    @Inject
    SelfManagedCallHandler(CarUxRestrictionsUtil carUxRestrictionsUtil,
                           InCallRouter inCallRouter) {
        mCarUxRestrctionsUtil = carUxRestrictionsUtil;
        mSelfManagedCallList = new ArrayList<>();
        mInCallRouter = inCallRouter;

        carUxRestrictionsUtil.register(this);
    }

    @Override
    public boolean onTelecomCallAdded(Call telecomCall) {
        if (telecomCall.getDetails().hasProperty(Call.Details.PROPERTY_SELF_MANAGED)) {
            mSelfManagedCallList.add(telecomCall);
            return !mCarUxRestrctionsUtil.getCurrentRestrictions()
                    .isRequiresDistractionOptimization();
        }
        return false;
    }

    @Override
    public boolean onTelecomCallRemoved(Call telecomCall) {
        if (telecomCall.getDetails().hasProperty(Call.Details.PROPERTY_SELF_MANAGED)) {
            mSelfManagedCallList.remove(telecomCall);
            return !mCarUxRestrctionsUtil.getCurrentRestrictions()
                    .isRequiresDistractionOptimization();
        }
        return false;
    }

    @Override
    public void onRestrictionsChanged(@NonNull CarUxRestrictions carUxRestrictions) {
        if (carUxRestrictions.isRequiresDistractionOptimization()) {
            for (Call call : mSelfManagedCallList) {
                int state = call.getDetails().getState();
                if (state != Call.STATE_DISCONNECTED && state != Call.STATE_RINGING) {
                    // Don't launch the in call page if state is disconnected.
                    // Otherwise, the InCallActivity finishes right after onCreate() and flashes.
                    // Don't launch the in call page for ringing VoIP calls.
                    mInCallRouter.routeToFullScreenIncomingCallPage(false, false);
                }
            }
        }
    }
}
