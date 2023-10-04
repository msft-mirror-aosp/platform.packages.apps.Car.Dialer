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

package com.android.car.dialer.livedata;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;

import com.android.car.apps.common.log.L;
import com.android.car.apps.common.util.LiveDataFunctions;
import com.android.car.telephony.common.CallDetail;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.InMemoryPhoneBook;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * A {@link LiveData} that maps the {@link com.android.car.telephony.common.CallDetail} to a
 * {@link Contact}.
 */
public class CallerInfoLiveData extends MediatorLiveData<Contact> {
    private static final String TAG = "CD.CallerInfo";
    private final LiveData<CallDetail> mCallDetailLiveData;
    private final ExecutorService mExecutorService;
    private Future<?> mLookupFuture;

    public CallerInfoLiveData(
            LiveData<CallDetail> callDetailLiveData, ExecutorService executorService) {
        mCallDetailLiveData = callDetailLiveData;
        mExecutorService = executorService;

        LiveData<CallMetadata> callMetadata = Transformations.distinctUntilChanged(
                LiveDataFunctions.mapNonNull(
                        mCallDetailLiveData, callDetail -> new CallMetadata(
                                callDetail.getNumber(),
                                callDetail.getCallerDisplayName(),
                                callDetail.getCallerImageUri(),
                                callDetail.getCurrentSpeaker(),
                                callDetail.getParticipantCount())));
        addSource(callMetadata, number -> lookupContact());

        LiveData<String> accountNameLiveData = Transformations.distinctUntilChanged(
                LiveDataFunctions.mapNonNull(
                        mCallDetailLiveData,
                        callDetail -> callDetail.getPhoneAccountName()));
        LiveData<List<Contact>> contactListLiveData = LiveDataFunctions.switchMapNonNull(
                accountNameLiveData,
                accountName -> InMemoryPhoneBook.get().getContactsLiveDataByAccount(accountName));
        addSource(contactListLiveData, contacts -> lookupContact());
    }

    private void lookupContact() {
        L.i(TAG, "Look up contact for the call");
        if (mLookupFuture != null) {
            mLookupFuture.cancel(false);
            mLookupFuture = null;
        }

        CallDetail callDetail = mCallDetailLiveData.getValue();
        if (callDetail == null || TextUtils.isEmpty(callDetail.getNumber())) {
            setValue(null);
            return;
        }

        String number = callDetail.getNumber();
        String accountName = callDetail.getPhoneAccountName();
        Contact contact = InMemoryPhoneBook.get().lookupContactEntry(number, accountName);
        setValue(contact);
        if (contact == null) {
            mLookupFuture = mExecutorService.submit(() -> {
                Contact dbContact = InMemoryPhoneBook.get().lookupContactEntryAsync(
                        number, accountName);
                // Check if the call has changed, abandon if true.
                if (callDetail.equals(mCallDetailLiveData.getValue()) && dbContact != null) {
                    postValue(dbContact);
                }
            });
        }
    }

    private static class CallMetadata {
        private String mNumber;
        private String mCallerDisplayName;
        private Uri mCallerImageUri;
        private String mCurrentSpeaker;
        private int mParticipantCount;

        CallMetadata(
                String number,
                String callerDisplayName,
                Uri callerImageUri,
                String currentSpeaker,
                int participantCount) {
            mNumber = number;
            mCallerDisplayName = callerDisplayName;
            mCallerImageUri = callerImageUri;
            mCurrentSpeaker = currentSpeaker;
            mParticipantCount = participantCount;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof CallMetadata
                    && TextUtils.equals(mNumber, ((CallMetadata) obj).mNumber)
                    && TextUtils.equals(mCallerDisplayName, ((CallMetadata) obj).mCallerDisplayName)
                    && Objects.equals(mCallerImageUri, ((CallMetadata) obj).mCallerImageUri)
                    && TextUtils.equals(mCurrentSpeaker, ((CallMetadata) obj).mCurrentSpeaker)
                    && mParticipantCount == ((CallMetadata) obj).mParticipantCount;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mNumber, mCallerDisplayName, mCallerImageUri, mCurrentSpeaker,
                    mParticipantCount);
        }
    }

}
