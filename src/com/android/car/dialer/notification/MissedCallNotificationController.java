/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.dialer.notification;

import static com.android.car.messenger.common.MessagingUtils.ACTION_DIRECT_SEND;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.icu.text.MessageFormat;
import android.os.Bundle;
import android.provider.CallLog;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.android.car.apps.common.log.L;
import com.android.car.apps.common.util.LiveDataFunctions;
import com.android.car.dialer.R;
import com.android.car.dialer.bluetooth.PhoneAccountManager;
import com.android.car.dialer.livedata.UnreadMissedCallLiveData;
import com.android.car.dialer.sms.DirectSendActivity;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.ui.TelecomActivity;
import com.android.car.telephony.common.PhoneCallLog;
import com.android.car.telephony.common.TelecomUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/** Controller that manages the missed call notifications. */
@Singleton
public final class MissedCallNotificationController {
    private static final String TAG = "CD.MissedCallNotification";
    private static final String CHANNEL_ID = "com.android.car.dialer.missedcall";
    // A random number that is used for notification id.
    private static final int NOTIFICATION_ID = 20190520;

    /** Tear down the global missed call notification controller. */
    public void tearDown() {
        mUnreadMissedCallLiveData.removeObserver(mUnreadMissedCallObserver);
    }

    private final Context mContext;
    private final PhoneAccountManager mPhoneAccountManager;
    private final UiCallManager mUiCallManager;
    private final NotificationManager mNotificationManager;
    private final LiveData<List<PhoneCallLog>> mUnreadMissedCallLiveData;
    private final Observer<List<PhoneCallLog>> mUnreadMissedCallObserver;
    private final List<PhoneCallLog> mCurrentPhoneCallLogList;
    private final Map<String, CompletableFuture<Void>> mUpdateFutures = new HashMap<>();

    @Inject
    MissedCallNotificationController(
            @ApplicationContext Context context,
            PhoneAccountManager phoneAccountManager,
            UiCallManager uiCallManager,
            @Named("Hfp") LiveData<BluetoothDevice> currentHfpDeviceLiveData) {
        mContext = context;
        mPhoneAccountManager = phoneAccountManager;
        mUiCallManager = uiCallManager;
        mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        CharSequence name = mContext.getString(R.string.missed_call_notification_channel_name);
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, name,
                NotificationManager.IMPORTANCE_DEFAULT);
        mNotificationManager.createNotificationChannel(notificationChannel);

        mCurrentPhoneCallLogList = new ArrayList<>();
        mUnreadMissedCallLiveData = LiveDataFunctions.switchMapNonNull(
                currentHfpDeviceLiveData,
                device-> UnreadMissedCallLiveData.newInstance(context, device.getAddress()));
        mUnreadMissedCallObserver = this::updateNotifications;
        mUnreadMissedCallLiveData.observeForever(mUnreadMissedCallObserver);
    }

    /**
     * The phone call log list might be null when switching users if permission gets denied and
     * throws exception.
     */
    private void updateNotifications(@Nullable List<PhoneCallLog> phoneCallLogs) {
        List<PhoneCallLog> updatedPhoneCallLogs =
                phoneCallLogs == null ? Collections.emptyList() : phoneCallLogs;
        for (PhoneCallLog phoneCallLog : updatedPhoneCallLogs) {
            showMissedCallNotification(phoneCallLog);
            mCurrentPhoneCallLogList.remove(phoneCallLog);
        }

        for (PhoneCallLog phoneCallLog : mCurrentPhoneCallLogList) {
            cancelMissedCallNotification(phoneCallLog);
        }
        mCurrentPhoneCallLogList.clear();
        mCurrentPhoneCallLogList.addAll(updatedPhoneCallLogs);
    }

    private void showMissedCallNotification(PhoneCallLog callLog) {
        L.d(TAG, "show missed call notification %s", callLog);
        String phoneNumber = callLog.getPhoneNumberString();
        String tag = getTag(callLog);
        cancelLoadingRunnable(tag);
        String accountName = callLog.getAccountName();
        BluetoothDevice bluetoothDevice = mPhoneAccountManager.getMatchingDevice(accountName);
        CompletableFuture<Void> updateFuture = NotificationUtils.getDisplayNameAndRoundedAvatar(
                mContext, phoneNumber, accountName)
                .thenAcceptAsync((pair) -> {
                    int callLogSize = callLog.getAllCallRecords().size();
                    MessageFormat msgFmt = new MessageFormat(mContext.getResources().getString(
                            R.string.notification_missed_call), Locale.getDefault());
                    Map<String, Object> strArgs = new HashMap<>();
                    strArgs.put("count", callLogSize);
                    String contentTitle = msgFmt.format(strArgs);

                    Notification.Builder builder = new Notification.Builder(mContext, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_phone)
                            .setColor(mContext.getColor(R.color.notification_app_icon_color))
                            .setLargeIcon(pair.second)
                            .setContentTitle(contentTitle)
                            .setContentText(TelecomUtils.getBidiWrappedNumber(pair.first))
                            .setContentIntent(getContentPendingIntent())
                            .setDeleteIntent(getDeleteIntent(callLog))
                            .setOnlyAlertOnce(true)
                            .setShowWhen(true)
                            .setWhen(callLog.getLastCallEndTimestamp())
                            .setAutoCancel(false);

                    if (!TextUtils.isEmpty(phoneNumber)) {
                        builder.addAction(getCallBackAction(phoneNumber, tag));
                        builder.addAction(
                                getSmsAction(phoneNumber, pair.first, bluetoothDevice, tag));
                    }

                    mNotificationManager.notify(
                            tag,
                            NOTIFICATION_ID,
                            builder.build());
                }, mContext.getMainExecutor());
        mUpdateFutures.put(tag, updateFuture);
    }

    private void cancelMissedCallNotification(PhoneCallLog phoneCallLog) {
        L.d(TAG, "cancel missed call notification %s", phoneCallLog);
        String tag = getTag(phoneCallLog);
        cancelMissedCallNotification(tag);
    }

    /**
     * Explicitly cancels the notification that in some circumstances the database update operation
     * has a delay to notify the cursor to reload.
     */
    void cancelMissedCallNotification(String tag) {
        if (TextUtils.isEmpty(tag)) {
            L.w(TAG, "Invalid notification tag, ignore canceling request.");
            return;
        }
        cancelLoadingRunnable(tag);
        mNotificationManager.cancel(tag, NOTIFICATION_ID);
    }

    private void cancelLoadingRunnable(String tag) {
        CompletableFuture<Void> completableFuture = mUpdateFutures.get(tag);
        if (completableFuture != null) {
            completableFuture.cancel(true);
        }
        mUpdateFutures.remove(tag);
    }

    private PendingIntent getContentPendingIntent() {
        Intent intent = new Intent(mContext, TelecomActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setType(CallLog.Calls.CONTENT_TYPE);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return pendingIntent;
    }

    private PendingIntent getDeleteIntent(PhoneCallLog phoneCallLog) {
        Intent intent = new Intent(NotificationService.ACTION_READ_MISSED, null, mContext,
                NotificationService.class);
        String phoneNumberString = phoneCallLog.getPhoneNumberString();
        if (TextUtils.isEmpty(phoneNumberString)) {
            // For unknown call, pass the call log id to mark as read
            intent.putExtra(NotificationService.EXTRA_CALL_LOG_ID, phoneCallLog.getPhoneLogId());
        } else {
            intent.putExtra(NotificationService.EXTRA_PHONE_NUMBER, phoneNumberString);
        }
        intent.putExtra(NotificationService.EXTRA_NOTIFICATION_TAG, getTag(phoneCallLog));
        PendingIntent pendingIntent = PendingIntent.getService(
                mContext,
                // Unique id for PendingIntents with different extras
                /* requestCode= */(int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_IMMUTABLE);
        return pendingIntent;
    }

    private Notification.Action getCallBackAction(String phoneNumberString, String tag) {
        CharSequence text = mContext.getString(R.string.call_back);
        PendingIntent intent = getCallBackIntent(
                NotificationService.ACTION_CALL_BACK_MISSED, phoneNumberString, tag);
        return new Notification.Action.Builder(null, text, intent).build();
    }

    private PendingIntent getCallBackIntent(String action, String phoneNumberString, String tag) {
        Intent intent = new Intent(action, null, mContext, NotificationService.class);
        intent.putExtra(NotificationService.EXTRA_PHONE_NUMBER, phoneNumberString);
        intent.putExtra(NotificationService.EXTRA_NOTIFICATION_TAG, tag);
        return PendingIntent.getService(
                mContext,
                // Unique id for PendingIntents with different extras
                /* requestCode= */(int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification.Action getSmsAction(String phoneNumberString, String displayName,
            BluetoothDevice device, String tag) {
        Intent showAssistIntent = new Intent(mContext, DirectSendActivity.class);
        showAssistIntent.setAction(ACTION_DIRECT_SEND);
        showAssistIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle bundle = mUiCallManager.buildDirectSendBundle(phoneNumberString, displayName, tag,
                device);
        showAssistIntent.putExtras(bundle);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                mContext,
                // Unique id for PendingIntents with different extras
                /* requestCode= */(int) System.currentTimeMillis(),
                showAssistIntent,
                PendingIntent.FLAG_IMMUTABLE);

        CharSequence text = mContext.getString(R.string.message);
        return new Notification.Action.Builder(null, text, pendingIntent).build();
    }

    private String getTag(@NonNull PhoneCallLog phoneCallLog) {
        return String.valueOf(phoneCallLog.hashCode());
    }
}
