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

import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;

import com.android.car.apps.common.log.L;
import com.android.car.apps.common.util.LiveDataFunctions;
import com.android.car.telephony.common.CallDetail;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.InMemoryPhoneBook;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * A {@link LiveData} that maps the {@link com.android.car.telephony.common.CallDetail} to a
 * {@link Contact}.
 */
public class CallerInfoLiveData extends MediatorLiveData<Contact> {
    private static final String TAG = "CD.CallProfile";
    private final LiveData<CallDetail> mCallDetailLiveData;
    private final ExecutorService mExecutorService;
    private final LiveData<String> mPhoneNumberLiveData;
    private final LiveData<String> mAccountNameLiveData;
    private final LiveData<List<Contact>> mContactListLiveData;
    private Future<?> mLookupFuture;

    public CallerInfoLiveData(
            LiveData<CallDetail> callDetailLiveData, ExecutorService executorService) {
        mCallDetailLiveData = callDetailLiveData;
        mExecutorService = executorService;

        mPhoneNumberLiveData = Transformations.distinctUntilChanged(
                LiveDataFunctions.mapNonNull(
                        mCallDetailLiveData, callDetail -> callDetail.getNumber()));
        mAccountNameLiveData = Transformations.distinctUntilChanged(
                LiveDataFunctions.mapNonNull(
                        mCallDetailLiveData,
                        callDetail -> callDetail.getPhoneAccountHandle().getId()));

        mContactListLiveData = LiveDataFunctions.switchMapNonNull(mAccountNameLiveData,
                accountName -> InMemoryPhoneBook.get().getContactsLiveDataByAccount(accountName));

        addSource(mPhoneNumberLiveData, number -> lookupContact());
        addSource(mContactListLiveData, contacts -> lookupContact());
    }

    private void lookupContact() {
        L.i(TAG, "Look up contact for the call");
        if (mLookupFuture != null) {
            mLookupFuture.cancel(false);
            mLookupFuture = null;
        }

        String number = mPhoneNumberLiveData.getValue();
        if (TextUtils.isEmpty(number)) {
            setValue(null);
            return;
        }

        // CallDetail can not be null when phone number is not null.
        String accountName = mAccountNameLiveData.getValue();
        Contact contact = InMemoryPhoneBook.get().lookupContactEntry(number, accountName);
        setValue(contact);
        if (contact == null) {
            mLookupFuture = mExecutorService.submit(() -> {
                Contact dbContact = InMemoryPhoneBook.get().lookupContactEntryAsync(
                        number, accountName);
                // Check if the call has changed, abandon if true.
                if (TextUtils.equals(number, mPhoneNumberLiveData.getValue())
                        && TextUtils.equals(accountName, mAccountNameLiveData.getValue())
                        && dbContact != null) {
                    postValue(dbContact);
                }
            });
        }
    }

}
