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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.Call;
import android.telecom.PhoneAccountHandle;
import android.text.TextUtils;
import android.util.TypedValue;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.util.Pair;

import com.android.car.apps.common.BitmapUtils;
import com.android.car.apps.common.log.L;
import com.android.car.dialer.R;
import com.android.car.dialer.bluetooth.PhoneAccountManager;
import com.android.car.telephony.common.CallDetail;
import com.android.car.telephony.common.TelecomUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/** Controller that manages the heads up notification for incoming calls. */
@Singleton
public final class InCallNotificationController {
    private static final String TAG = "CD.InCallNotificationController";
    private static final String CHANNEL_ID = "com.android.car.dialer.incoming";
    // A random number that is used for notification id.
    private static final int NOTIFICATION_ID = 20181105;

    private boolean mShowFullscreenIncallUi;

    private final Context mContext;
    private final PhoneAccountManager mPhoneAccountManager;
    private final NotificationManager mNotificationManager;
    private final Notification.Builder mNotificationBuilder;
    private final Set<String> mActiveInCallNotifications;
    private CompletableFuture<Void> mNotificationFuture;

    @Inject
    public InCallNotificationController(
            @ApplicationContext Context context, PhoneAccountManager phoneAccountManager) {
        mContext = context;
        mPhoneAccountManager = phoneAccountManager;

        mShowFullscreenIncallUi = mContext.getResources().getBoolean(
                R.bool.config_show_hun_fullscreen_incall_ui);
        mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        CharSequence name = mContext.getString(R.string.in_call_notification_channel_name);
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, name,
                NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setSound(null, null);
        mNotificationManager.createNotificationChannel(notificationChannel);

        Bundle extras = new Bundle();
        extras.putBoolean("com.android.car.notification.EXTRA_USE_LAUNCHER_ICON", false);
        mNotificationBuilder = new Notification.Builder(mContext, CHANNEL_ID)
                .setCategory(Notification.CATEGORY_CALL)
                .setOngoing(true)
                .setAutoCancel(false)
                .addExtras(extras);

        mActiveInCallNotifications = new HashSet<>();
    }


    /** Show a new incoming call notification or update the existing incoming call notification. */
    public void showInCallNotification(Call call) {
        L.d(TAG, "showInCallNotification");

        if (mNotificationFuture != null) {
            mNotificationFuture.cancel(true);
        }

        CallDetail callDetail = CallDetail.fromTelecomCallDetail(call.getDetails());
        String callNumber = callDetail.getNumber();
        mActiveInCallNotifications.add(callNumber);

        // HUN will not persist if the fullscreen intent is disabled.
        if (mShowFullscreenIncallUi) {
            mNotificationBuilder.setFullScreenIntent(
                    getFullscreenIntent(call), /* highPriority= */true);
        }

        Pair<Drawable, CharSequence> appInfo = mPhoneAccountManager.getAppInfo(
                callDetail.getPhoneAccountHandle(), callDetail.isSelfManaged());
        Bitmap appIcon = BitmapUtils.fromDrawable(appInfo.first, null);
        mNotificationBuilder
                .setSmallIcon(Icon.createWithBitmap(appIcon))
                // Per notification design, HUN only shows the large icon which is the app icon
                .setLargeIcon(appIcon)
                .setContentText(mContext.getString(R.string.notification_incoming_call))
                .setActions(
                        getAction(call, R.string.answer_call, R.drawable.ic_answer_icon,
                                NotificationService.ACTION_ANSWER_CALL),
                        getAction(call, R.string.decline_call, R.drawable.ic_decline_icon,
                                NotificationService.ACTION_DECLINE_CALL));
        String callerDisplayName = callDetail.getCallerDisplayName();
        Uri callerImageUri = callDetail.getCallerImageUri();
        if (TextUtils.isEmpty(callerDisplayName)) {
            String readableNumber = TelecomUtils.getReadableNumber(mContext, callNumber);
            mNotificationBuilder.setContentTitle(TelecomUtils.getBidiWrappedNumber(readableNumber))
                    .setContentText(mContext.getString(R.string.notification_incoming_call));
        } else if (TextUtils.isEmpty(callNumber)) {
            mNotificationBuilder.setContentTitle(callerDisplayName)
                    .setContentText(mContext.getString(R.string.notification_incoming_call));
        } else {
            mNotificationBuilder.setContentTitle(callerDisplayName)
                    .setContentText(mContext.getString(
                            R.string.notification_incoming_call_join_number,
                            TelecomUtils.getBidiWrappedNumber(callNumber)));
        }

        mNotificationManager.notify(
                callNumber,
                NOTIFICATION_ID,
                mNotificationBuilder.build());

        mNotificationFuture = NotificationUtils.getDisplayNameAndRoundedAvatar(
                mContext, callNumber, callDetail.getPhoneAccountHandle().getId(), callerDisplayName,
                        callerImageUri)
                .thenAcceptAsync((pair) -> {
                    // Check that the notification hasn't already been dismissed
                    if (mActiveInCallNotifications.contains(callNumber)) {
                        String readableNumber = TelecomUtils.getReadableNumber(
                                mContext, callNumber);
                        if (!TextUtils.isEmpty(callNumber)
                                && !TextUtils.equals(readableNumber, pair.first)) {
                            mNotificationBuilder
                                    .setContentTitle(TelecomUtils.getBidiWrappedNumber(pair.first));
                            mNotificationBuilder.setContentText(
                                    mContext.getString(
                                            R.string.notification_incoming_call_join_number,
                                            TelecomUtils.getBidiWrappedNumber(readableNumber)));
                        }

                        mNotificationManager.notify(
                                callNumber,
                                NOTIFICATION_ID,
                                mNotificationBuilder.build());
                    }
                }, mContext.getMainExecutor());
    }

    /** Cancel the incoming call notification for the given call. */
    public void cancelInCallNotification(Call call) {
        L.d(TAG, "cancelInCallNotification");
        if (call.getDetails() != null) {
            String callNumber = CallDetail.fromTelecomCallDetail(call.getDetails()).getNumber();
            cancelInCallNotification(callNumber);
        }
    }

    /**
     * Cancel the incoming call notification for the given call id. Any action that dismisses the
     * notification needs to call this explicitly.
     */
    void cancelInCallNotification(String callId) {
        mActiveInCallNotifications.remove(callId);
        mNotificationManager.cancel(callId, NOTIFICATION_ID);
    }

    private PendingIntent getFullscreenIntent(Call call) {
        Intent intent = getIntent(NotificationService.ACTION_SHOW_FULLSCREEN_UI, call);
        CallDetail callDetail = CallDetail.fromTelecomCallDetail(call.getDetails());

        // Only put the extra component name for the self managed calls.
        if (callDetail.isSelfManaged()) {
            ComponentName componentName = callDetail.getInCallViewComponentName();
            if (componentName == null) {
                PhoneAccountHandle phoneAccountHandle = callDetail.getPhoneAccountHandle();
                componentName =
                        mPhoneAccountManager.getLaunchIntentComponentName(phoneAccountHandle);
            }
            intent.putExtra(Intent.EXTRA_COMPONENT_NAME, componentName);
        }
        return PendingIntent.getService(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification.Action getAction(Call call, @StringRes int actionText,
            @DrawableRes int actionIcon, String intentAction) {
        CharSequence text = mContext.getString(actionText);
        TypedValue typedValue = new TypedValue();
        mContext.getResources().getValue(actionIcon, typedValue, true);
        Icon icon = typedValue.string == null ? null : Icon.createWithResource(mContext,
                actionIcon);
        PendingIntent intent = PendingIntent.getService(
                mContext,
                0,
                getIntent(intentAction, call),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Action.Builder(icon, text, intent).build();
    }

    private Intent getIntent(String action, Call call) {
        Intent intent = new Intent(action, null, mContext, NotificationService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(NotificationService.EXTRA_PHONE_NUMBER,
                CallDetail.fromTelecomCallDetail(call.getDetails()).getNumber());
        return intent;
    }

}
