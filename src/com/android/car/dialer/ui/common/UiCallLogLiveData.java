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

package com.android.car.dialer.ui.common;

import android.content.Context;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.android.car.dialer.R;
import com.android.car.dialer.livedata.HeartBeatLiveData;
import com.android.car.dialer.log.L;
import com.android.car.dialer.ui.common.entity.UiCallLog;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.InMemoryPhoneBook;
import com.android.car.telephony.common.PhoneCallLog;
import com.android.car.telephony.common.PhoneNumber;
import com.android.car.telephony.common.TelecomUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Represents a list of {@link UiCallLog}s and label {@link String}s for UI representation. This
 * live data gets data source from both call log and contact list. It also refresh itself on the
 * relative time in the body text.
 */
public class UiCallLogLiveData extends MediatorLiveData<List<Object>> {
    private static final String TAG = "CD.UiCallLogLiveData";

    private final ExecutorService mSingleThreadExecutor;
    private Future<?> mRunnableFuture;
    private Context mContext;

    @Inject
    public UiCallLogLiveData(
            @ApplicationContext Context context,
            HeartBeatLiveData.Factory heartBeatLiveDataFactory,
            LiveData<List<PhoneCallLog>> callHistoryLiveData,
            LiveData<List<Contact>> contactListLiveData,
            ExecutorService singleThreadExecutor) {
        mContext = context;
        mSingleThreadExecutor = singleThreadExecutor;

        addSource(callHistoryLiveData, this::onCallHistoryChanged);
        addSource(contactListLiveData,
                (contacts) -> onContactsChanged(callHistoryLiveData.getValue()));

        HeartBeatLiveData heartBeatLiveData =
                heartBeatLiveDataFactory.create(DateUtils.MINUTE_IN_MILLIS);
        addSource(heartBeatLiveData, (trigger) -> updateRelativeTime());
    }

    private void onCallHistoryChanged(@Nullable List<PhoneCallLog> callLogs) {
        if (mRunnableFuture != null) {
            mRunnableFuture.cancel(true);
        }
        Runnable runnable = () -> postValue(convert(callLogs));
        mRunnableFuture = mSingleThreadExecutor.submit(runnable);
    }

    private void onContactsChanged(List<PhoneCallLog> callLogs) {
        // When contacts change, do not set value to trigger an update when there are no
        // call logs loaded yet. An update will switch the loading state to loaded in the ViewModel.
        if (callLogs == null || callLogs.isEmpty()) {
            return;
        }
        onCallHistoryChanged(callLogs);
    }

    private void updateRelativeTime() {
        boolean hasChanged = false;
        List<Object> uiCallLogs = getValue();
        if (uiCallLogs == null) {
            return;
        }
        for (Object object : uiCallLogs) {
            if (object instanceof UiCallLog) {
                UiCallLog uiCallLog = (UiCallLog) object;

                String newRelativeTime = getRelativeTime(uiCallLog.getMostRecentCallEndTimestamp());
                if (uiCallLog.setRelativeTime(newRelativeTime)) {
                    hasChanged = true;
                }
            }
        }

        if (hasChanged) {
            setValue(getValue());
        }
    }

    /**
     * Convert {@link PhoneCallLog}s to UI friendly {@link UiCallLog}s. The list will be grouped by
     * time and each group starts with a header indicating the time range the calls fall into.
     */
    @NonNull
    private List<Object> convert(@Nullable List<PhoneCallLog> phoneCallLogs) {
        if (phoneCallLogs == null) {
            return Collections.emptyList();
        }
        List<Object> uiCallLogs = new ArrayList<>();
        String preHeader = null;

        InMemoryPhoneBook inMemoryPhoneBook = InMemoryPhoneBook.get();
        for (PhoneCallLog phoneCallLog : phoneCallLogs) {
            String header = getHeader(phoneCallLog.getTimeRange());
            if (preHeader == null || (!header.equals(preHeader))) {
                uiCallLogs.add(header);
            }
            preHeader = header;

            String number = phoneCallLog.getPhoneNumberString();
            String relativeTime = getRelativeTime(phoneCallLog.getLastCallEndTimestamp());

            Contact contact = inMemoryPhoneBook.lookupContactEntry(
                    number, phoneCallLog.getAccountName());

            String readableNumber = TelecomUtils.getReadableNumber(mContext, number);
            UiCallLog uiCallLog = new UiCallLog(
                    contact == null ? readableNumber : contact.getDisplayName(),
                    contact == null ? readableNumber : contact.getDisplayNameAlt(),
                    number,
                    contact,
                    phoneCallLog.getAllCallRecords());

            uiCallLog.setRelativeTime(relativeTime);
            PhoneNumber phoneNumber = contact == null ? null : contact.getPhoneNumber(number);
            uiCallLog.setLabel(phoneNumber == null
                    ? null : phoneNumber.getReadableLabel(mContext.getResources()));
            uiCallLogs.add(uiCallLog);
        }
        L.i(TAG, "phoneCallLog size: %d, uiCallLog size: %d",
                phoneCallLogs.size(), uiCallLogs.size());

        return uiCallLogs;
    }

    private String getRelativeTime(long millis) {
        boolean validTimestamp = millis > 0;

        return validTimestamp ? DateUtils.getRelativeTimeSpanString(
                millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE).toString() : "";
    }

    private String getHeader(@PhoneCallLog.TimeRange int timeRange) {
        switch (timeRange) {
            case PhoneCallLog.TimeRange.TODAY:
                return mContext.getResources().getString(R.string.call_log_header_today);
            case PhoneCallLog.TimeRange.YESTERDAY:
                return mContext.getResources().getString(R.string.call_log_header_yesterday);
            case PhoneCallLog.TimeRange.OLDER:
            default:
                return mContext.getResources().getString(R.string.call_log_header_older);
        }
    }
}
