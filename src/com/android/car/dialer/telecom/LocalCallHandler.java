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

package com.android.car.dialer.telecom;

import android.car.drivingstate.CarUxRestrictions;
import android.telecom.Call;
import android.telecom.CallAudioState;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.car.apps.common.log.L;
import com.android.car.telephony.calling.InCallServiceManager;
import com.android.car.telephony.calling.SimpleInCallServiceImpl.ActiveCallListChangedCallback;
import com.android.car.telephony.calling.SimpleInCallServiceImpl.CallAudioStateCallback;
import com.android.car.telephony.selfmanaged.SelfManagedCallUtil;
import com.android.car.ui.utils.CarUxRestrictionsUtil;

import com.google.common.base.Predicate;

import dagger.hilt.android.scopes.ViewModelScoped;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

/**
 * Binds to the {@link InCallServiceImpl}, and upon establishing a connection, handles call list
 * change and call audio state change.
 */
@ViewModelScoped
public class LocalCallHandler
        implements CarUxRestrictionsUtil.OnUxRestrictionsChangedListener, PropertyChangeListener {
    private static final String TAG = "CD.CallHandler";
    private final CarUxRestrictionsUtil mCarUxRestrictionsUtil;
    private final InCallServiceManager mInCallServiceManager;
    private final SelfManagedCallUtil mSelfManagedCallUtil;
    private final Call.Callback mRingingCallStateChangeCallback;

    private final MutableLiveData<CallAudioState> mCallAudioStateLiveData = new MutableLiveData<>();
    private final CallAudioStateCallback mCallAudioStateCallback =
            mCallAudioStateLiveData::setValue;

    private final MutableLiveData<List<Call>> mCallListLiveData;
    private final MutableLiveData<Call> mIncomingCallLiveData;
    private final MutableLiveData<List<Call>> mOngoingCallListLiveData;
    private final ActiveCallListChangedCallback mActiveCallListChangedCallback =
            new ActiveCallListChangedCallback() {
                @Override
                public boolean onTelecomCallAdded(Call telecomCall) {
                    notifyCallAdded(telecomCall);
                    notifyCallListChanged();
                    return false;
                }

                @Override
                public boolean onTelecomCallRemoved(Call telecomCall) {
                    notifyCallRemoved(telecomCall);
                    notifyCallListChanged();
                    return false;
                }
            };

    /**
     * Initiate a LocalCallHandler.
     */
    @Inject
    public LocalCallHandler(
            CarUxRestrictionsUtil carUxRestrictionsUtil,
            InCallServiceManager inCallServiceManager,
            SelfManagedCallUtil selfManagedCallUtil) {
        mCarUxRestrictionsUtil = carUxRestrictionsUtil;
        mInCallServiceManager = inCallServiceManager;
        mSelfManagedCallUtil = selfManagedCallUtil;

        mCallListLiveData = new MutableLiveData<>();
        mIncomingCallLiveData = new MutableLiveData<>();
        mOngoingCallListLiveData = new MutableLiveData<>();

        mRingingCallStateChangeCallback = new Call.Callback() {
            @Override
            public void onStateChanged(Call call, int state) {
                // Don't show in call activity by declining a ringing call to avoid UI flashing.
                if (state != Call.STATE_DISCONNECTED) {
                    notifyCallListChanged();
                }
                call.unregisterCallback(this);
            }
        };

        mInCallServiceManager.addObserver(this);
        if (mInCallServiceManager.getInCallService() != null) {
            onInCallServiceConnected();
        }
    }

    /** Returns the {@link LiveData} which monitors the call audio state change. */
    public LiveData<CallAudioState> getCallAudioStateLiveData() {
        return mCallAudioStateLiveData;
    }

    /** Returns the {@link LiveData} which monitors the call list. */
    @NonNull
    public LiveData<List<Call>> getCallListLiveData() {
        return mCallListLiveData;
    }

    /** Returns the {@link LiveData} which monitors the active call list. */
    @NonNull
    public LiveData<List<Call>> getOngoingCallListLiveData() {
        return mOngoingCallListLiveData;
    }

    /** Returns the {@link LiveData} which monitors the ringing call. */
    @NonNull
    public LiveData<Call> getIncomingCallLiveData() {
        return mIncomingCallLiveData;
    }

    @Override
    public void onRestrictionsChanged(@NonNull CarUxRestrictions carUxRestrictions) {
        notifyCallListChanged();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        L.i(TAG, "InCallService has updated.");
        if (mInCallServiceManager.getInCallService() == null) {
            cleanup();
        } else {
            onInCallServiceConnected();
        }
    }

    private void onInCallServiceConnected() {
        InCallServiceImpl inCallService =
                (InCallServiceImpl) mInCallServiceManager.getInCallService();
        for (Call call : inCallService.getCallList()) {
            notifyCallAdded(call);
        }
        inCallService.addActiveCallListChangedCallback(mActiveCallListChangedCallback);
        inCallService.addCallAudioStateChangedCallback(mCallAudioStateCallback);
        // Register will call onRestrictionsChanged(CarUxRestrictions) and notify call list
        // changed, so we don't need to call notifyCallListChanged() again.
        mCarUxRestrictionsUtil.register(LocalCallHandler.this);
    }

    /** Disconnects the {@link InCallServiceImpl} and cleanup. */
    public void tearDown() {
        mInCallServiceManager.removeObserver(this);
        cleanup();
    }

    private void cleanup() {
        mCarUxRestrictionsUtil.unregister(this);
        InCallServiceImpl inCallService =
                (InCallServiceImpl) mInCallServiceManager.getInCallService();
        if (inCallService != null) {
            for (Call call : inCallService.getCallList()) {
                notifyCallRemoved(call);
            }
            inCallService.removeActiveCallListChangedCallback(mActiveCallListChangedCallback);
            inCallService.removeCallAudioStateChangedCallback(mCallAudioStateCallback);
        }
    }

    /**
     * The call list has updated, notify change. It will update when:
     * <ul>
     *     <li> {@link InCallServiceImpl} has been bind, init the call list.
     *     <li> A call has been added to the {@link InCallServiceImpl}.
     *     <li> A call has been removed from the {@link InCallServiceImpl}.
     *     <li> A call has changed.
     * </ul>
     */
    private void notifyCallListChanged() {
        InCallServiceImpl inCallService =
                (InCallServiceImpl) mInCallServiceManager.getInCallService();
        if (inCallService == null) {
            return;
        }

        List<Call> callList = new ArrayList<>(inCallService.getCallList());
        // If car is not driving(parked or idle), filter self managed calls.
        if (mSelfManagedCallUtil.canShowCalInCallView()) {
            callList = filter(callList, call -> call != null
                    && !call.getDetails().hasProperty(Call.Details.PROPERTY_SELF_MANAGED));
        }

        List<Call> activeCallList = filter(callList,
                call -> call != null && call.getDetails().getState() != Call.STATE_RINGING);
        mOngoingCallListLiveData.setValue(activeCallList);

        Call ringingCall = firstMatch(callList,
                call -> call != null && call.getDetails().getState() == Call.STATE_RINGING);
        mIncomingCallLiveData.setValue(ringingCall);

        mCallListLiveData.setValue(callList);
    }

    private void notifyCallAdded(Call call) {
        if (call.getDetails().getState() == Call.STATE_RINGING) {
            call.registerCallback(mRingingCallStateChangeCallback);
        }
    }

    private void notifyCallRemoved(Call call) {
        call.unregisterCallback(mRingingCallStateChangeCallback);
    }

    @Nullable
    private static Call firstMatch(List<Call> callList, Predicate<Call> predicate) {
        List<Call> filteredResults = filter(callList, predicate);
        return filteredResults.isEmpty() ? null : filteredResults.get(0);
    }

    @NonNull
    private static List<Call> filter(List<Call> callList, Predicate<Call> predicate) {
        if (callList == null || predicate == null) {
            return Collections.emptyList();
        }

        List<Call> filteredResults = new ArrayList<>();
        for (Call call : callList) {
            if (predicate.apply(call)) {
                filteredResults.add(call);
            }
        }
        return filteredResults;
    }
}
