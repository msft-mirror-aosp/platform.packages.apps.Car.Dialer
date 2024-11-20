/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.car.dialer.ui.activecall;

import android.telecom.Call;
import android.telecom.CallAudioState;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.android.car.apps.common.util.LiveDataFunctions;
import com.android.car.dialer.livedata.CallerInfoLiveData;
import com.android.car.telephony.calling.AudioRouteLiveData;
import com.android.car.telephony.calling.CallComparator;
import com.android.car.telephony.calling.CallDetailLiveData;
import com.android.car.telephony.calling.CallStateLiveData;
import com.android.car.telephony.calling.InCallModel;
import com.android.car.telephony.calling.SupportedAudioRoutesLiveData;
import com.android.car.telephony.common.CallDetail;
import com.android.car.telephony.common.Contact;

import dagger.hilt.android.lifecycle.HiltViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

/**
 * View model for {@link InCallActivity} and {@link OngoingCallFragment}. UI that doesn't belong to
 * in call page should use a different ViewModel.
 */
@HiltViewModel
public class InCallViewModel extends ViewModel {
    private static final String TAG = "CD.InCallViewModel";

    private final InCallModel mInCallModel;
    private final AudioRouteLiveData.Factory mAudioRouteLiveDataFactory;
    private final SupportedAudioRoutesLiveData.Factory mSupportedAudioRoutesLiveDataFactory;
    private final LiveData<List<Contact>> mContactListLiveData;
    private final ExecutorService mExecutorService;

    private final LiveData<List<Call>> mOngoingCallListLiveData;
    private final LiveData<List<Call>> mConferenceCallListLiveData;;
    private final LiveData<List<CallDetail>> mConferenceCallDetailListLiveData;

    private final LiveData<CallDetail> mIncomingCallDetailLiveData;
    private final CallerInfoLiveData mIncomingCallerInfoLiveData;

    private final LiveData<CallDetail> mCallDetailLiveData;
    private final LiveData<Integer> mCallStateLiveData;
    private final LiveData<Call> mPrimaryCallLiveData;
    private final LiveData<Call> mSecondaryCallLiveData;
    private final LiveData<Contact> mPrimaryCallerInfoLiveData;
    private final LiveData<Contact> mSecondaryCallerInfoLiveData;
    private final LiveData<CallDetail> mSecondaryCallDetailLiveData;
    private final LiveData<Pair<Call, Call>> mOngoingCallPairLiveData;
    private final MutableLiveData<Boolean> mDialpadIsOpen;
    private final ShowOnholdCallLiveData mShowOnholdCall;
    private LiveData<Long> mCallConnectTimeLiveData;
    private LiveData<Long> mSecondaryCallConnectTimeLiveData;
    private LiveData<Pair<Integer, Long>> mCallStateAndConnectTimeLiveData;

    private final AudioRouteLiveData mAudioRouteLiveData;
    private final SupportedAudioRoutesLiveData mSupportedAudioRoutesLiveData;

    @Inject
    public InCallViewModel(
            InCallModel inCallModel,
            AudioRouteLiveData.Factory audioRouteLiveDataFactory,
            SupportedAudioRoutesLiveData.Factory supportedAudioRouteLiveDataFactory,
            ExecutorService executorService,
            LiveData<List<Contact>> contactListLiveData) {
        mInCallModel = inCallModel;
        mAudioRouteLiveDataFactory = audioRouteLiveDataFactory;
        mSupportedAudioRoutesLiveDataFactory = supportedAudioRouteLiveDataFactory;
        mContactListLiveData = contactListLiveData;
        mExecutorService = executorService;

        mConferenceCallListLiveData = mInCallModel.getConferenceCallListLiveData();
        mOngoingCallListLiveData = mInCallModel.getOngoingCallListLiveData();

        mConferenceCallDetailListLiveData = Transformations.map(mConferenceCallListLiveData,
                callList -> {
                    List<CallDetail> detailList = new ArrayList<>();
                    for (Call call : callList) {
                        detailList.add(CallDetail.fromTelecomCall(call));
                    }
                    return detailList;
                });

        mIncomingCallDetailLiveData = Transformations.switchMap(
                mInCallModel.getIncomingCallLiveData(), call -> {
                    CallDetailLiveData callDetailLiveData = new CallDetailLiveData();
                    callDetailLiveData.setTelecomCall(call);
                    return callDetailLiveData;
                });
        mIncomingCallerInfoLiveData = new CallerInfoLiveData(
                mIncomingCallDetailLiveData, mExecutorService);

        mPrimaryCallLiveData = mInCallModel.getPrimaryCallLiveData();
        mCallDetailLiveData = Transformations.switchMap(mPrimaryCallLiveData, call -> {
            CallDetailLiveData callDetailLiveData = new CallDetailLiveData();
            callDetailLiveData.setTelecomCall(call);
            return callDetailLiveData;
        });
        mPrimaryCallerInfoLiveData = new CallerInfoLiveData(mCallDetailLiveData, mExecutorService);
        mAudioRouteLiveData = mAudioRouteLiveDataFactory.create(mCallDetailLiveData,
                mInCallModel.getCallAudioStateLiveData());
        mSupportedAudioRoutesLiveData = mSupportedAudioRoutesLiveDataFactory.create(
                mCallDetailLiveData);

        mCallStateLiveData = Transformations.switchMap(mPrimaryCallLiveData,
                input -> input != null ? new CallStateLiveData(input) : null);
        mCallConnectTimeLiveData = Transformations.map(mCallDetailLiveData, (details) -> {
            if (details == null) {
                return 0L;
            }
            return details.getConnectTimeMillis();
        });
        mCallStateAndConnectTimeLiveData =
                LiveDataFunctions.pair(mCallStateLiveData, mCallConnectTimeLiveData);

        mSecondaryCallLiveData = mInCallModel.getSecondaryCallLiveData();
        mSecondaryCallDetailLiveData = Transformations.switchMap(mSecondaryCallLiveData, call -> {
            CallDetailLiveData callDetailLiveData = new CallDetailLiveData();
            callDetailLiveData.setTelecomCall(call);
            return callDetailLiveData;
        });
        mSecondaryCallerInfoLiveData = new CallerInfoLiveData(
                mSecondaryCallDetailLiveData, mExecutorService);

        mSecondaryCallConnectTimeLiveData = Transformations.map(mSecondaryCallDetailLiveData,
                details -> {
                    if (details == null) {
                        return 0L;
                    }
                    return details.getConnectTimeMillis();
                });

        mOngoingCallPairLiveData = LiveDataFunctions.pair(mPrimaryCallLiveData,
                mSecondaryCallLiveData);

        mDialpadIsOpen = new MutableLiveData<>();
        // Set initial value to avoid NPE
        mDialpadIsOpen.setValue(false);

        mShowOnholdCall = new ShowOnholdCallLiveData(mSecondaryCallLiveData);
    }

    /** Returns if primary and secondary calls can merge. */
    public boolean canMerge() {
        CallDetail callDetail = mCallDetailLiveData.getValue();
        CallDetail otherCallDetail = mSecondaryCallDetailLiveData.getValue();

        if (callDetail == null || otherCallDetail == null) {
            return false;
        }
        // No CAPABILITY_MERGE_CONFERENCE check since Bluetooth doesn't set it for phone calls that
        // can merge.
        return !callDetail.isConference()
                && Objects.equals(callDetail.getPhoneAccountHandle(),
                    otherCallDetail.getPhoneAccountHandle());
    }

    /** Merge primary and secondary calls into a conference */
    public void mergeConference() {
        Call call = mPrimaryCallLiveData.getValue();
        Call otherCall = mSecondaryCallLiveData.getValue();

        if (call == null || otherCall == null) {
            return;
        }
        call.conference(otherCall);
    }

    /** Returns the live data which monitors conference calls */
    public LiveData<List<CallDetail>> getConferenceCallDetailList() {
        return mConferenceCallDetailListLiveData;
    }

    /** Returns the live data which monitors all the calls. */
    public LiveData<List<Call>> getAllCallList() {
        return mInCallModel.getCallListLiveData();
    }

    /** Returns the live data which monitors the current incoming call. */
    public LiveData<Call> getIncomingCall() {
        return mInCallModel.getIncomingCallLiveData();
    }

    public LiveData<CallDetail> getIncomingCallDetail() {
        return mIncomingCallDetailLiveData;
    }

    /** Returns {@link LiveData} for the ongoing call list which excludes the ringing call. */
    public LiveData<List<Call>> getOngoingCallList() {
        return mOngoingCallListLiveData;
    }

    /**
     * Returns the live data which monitors the primary call details.
     */
    public LiveData<CallDetail> getPrimaryCallDetail() {
        return mCallDetailLiveData;
    }

    /**
     * Returns the live data which monitors the primary call state.
     */
    public LiveData<Integer> getPrimaryCallState() {
        return mCallStateLiveData;
    }

    /**
     * Returns the live data which monitors the primary call state and the start time of the call.
     */
    public LiveData<Pair<Integer, Long>> getCallStateAndConnectTime() {
        return mCallStateAndConnectTimeLiveData;
    }

    /**
     * Returns the live data which monitor the primary call.
     * A primary call in the first call in the ongoing call list,
     * which is sorted based on {@link CallComparator}.
     */
    public LiveData<Call> getPrimaryCall() {
        return mPrimaryCallLiveData;
    }

    /**
     * Returns the live data which monitor the secondary call.
     * A secondary call in the second call in the ongoing call list,
     * which is sorted based on {@link CallComparator}.
     * The value will be null if there is no second call in the call list.
     */
    public LiveData<Call> getSecondaryCall() {
        return mSecondaryCallLiveData;
    }

    /**
     * Returns the live data which monitors the secondary call details.
     */
    public LiveData<CallDetail> getSecondaryCallDetail() {
        return mSecondaryCallDetailLiveData;
    }

    /**
     * Returns the live data which monitors the secondary call connect time.
     */
    public LiveData<Long> getSecondaryCallConnectTime() {
        return mSecondaryCallConnectTimeLiveData;
    }

    /**
     * Returns the live data that monitors the primary and secondary calls.
     */
    public LiveData<Pair<Call, Call>> getOngoingCallPair() {
        return mOngoingCallPairLiveData;
    }

    /**
     * Returns current audio route.
     */
    public LiveData<Integer> getAudioRoute() {
        return mAudioRouteLiveData;
    }

    public LiveData<List<Integer>> getSupportedAudioRoutes() {
        return mSupportedAudioRoutesLiveData;
    }

    /**
     * Returns contact list.
     */
    public LiveData<List<Contact>> getContactListLiveData() {
        return mContactListLiveData;
    }

    /**
     * Returns current call audio state.
     */
    public LiveData<CallAudioState> getCallAudioState() {
        return mInCallModel.getCallAudioStateLiveData();
    }

    /** Return the {@link MutableLiveData} for dialpad open state. */
    public MutableLiveData<Boolean> getDialpadOpenState() {
        return mDialpadIsOpen;
    }

    /** Return the livedata monitors onhold call status. */
    public LiveData<Boolean> shouldShowOnholdCall() {
        return mShowOnholdCall;
    }

    public LiveData<Contact> getPrimaryCallerInfoLiveData() {
        return mPrimaryCallerInfoLiveData;
    }

    public LiveData<Contact> getSecondaryCallerInfoLiveData() {
        return mSecondaryCallerInfoLiveData;
    }

    public LiveData<Contact> getIncomingCallerInfoLiveData() {
        return mIncomingCallerInfoLiveData;
    }

    private static class ShowOnholdCallLiveData extends MediatorLiveData<Boolean> {
        private final LiveData<Call> mSecondaryCallLiveData;

        private ShowOnholdCallLiveData(LiveData<Call> secondaryCallLiveData) {
            mSecondaryCallLiveData = secondaryCallLiveData;
            setValue(false);

            addSource(mSecondaryCallLiveData, v -> update());
        }

        private void update() {
            Call onholdCall = mSecondaryCallLiveData.getValue();
            setValue(onholdCall != null && onholdCall.getState() == Call.STATE_HOLDING);
        }

        @Override
        public void setValue(Boolean newValue) {
            // Only set value and notify observers when the value changes.
            if (getValue() != newValue) {
                super.setValue(newValue);
            }
        }
    }
}
