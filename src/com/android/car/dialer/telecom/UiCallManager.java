/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static com.android.car.assist.CarVoiceInteractionSession.KEY_SEND_PENDING_INTENT;
import static com.android.car.assist.CarVoiceInteractionSession.VOICE_ACTION_SEND_SMS;
import static com.android.car.messenger.common.MessagingUtils.ACTION_DIRECT_SEND;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;

import com.android.car.apps.common.log.L;
import com.android.car.assist.CarVoiceInteractionSession;
import com.android.car.dialer.R;
import com.android.car.dialer.sms.MessagingService;
import com.android.car.dialer.ui.common.DialerUtils;
import com.android.car.telephony.common.TelecomUtils;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * The entry point for all interactions between UI and telecom.
 */
@Singleton
public final class UiCallManager {
    private static String TAG = "CD.TelecomMgr";

    private Context mContext;
    private final TelecomManager mTelecomManager;
    private LiveData<BluetoothDevice> mCurrentHfpDeviceLiveData;

    @Inject
    UiCallManager(
            @ApplicationContext Context context,
            TelecomManager telecomManager,
            @Named("Hfp") LiveData<BluetoothDevice> currentHfpDeviceLiveData) {
        L.d(TAG, "SetUp");
        mContext = context;
        mTelecomManager = telecomManager;
        mCurrentHfpDeviceLiveData = currentHfpDeviceLiveData;
    }

    /**
     * Tears down the {@link UiCallManager}. Calling this function will null out the global
     * accessible {@link UiCallManager} instance. Remember to re-initialize the
     * {@link UiCallManager}.
     */
    public void tearDown() {
        // Clear out the mContext reference to avoid memory leak.
        mContext = null;
    }

    /**
     * Places call through TelecomManager
     *
     * @return {@code true} if a call is successfully placed, false if number is invalid.
     */
    public boolean placeCall(String number) {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            L.w(TAG, "Permission is denied to place call.");
            return false;
        }

        if (isValidNumber(number)) {
            Uri uri = Uri.fromParts("tel", number, null);
            L.d(TAG, "android.telecom.TelecomManager#placeCall: %s", TelecomUtils.piiLog(number));

            try {
                mTelecomManager.placeCall(uri, null);
                return true;
            } catch (IllegalStateException e) {
                Toast.makeText(mContext, R.string.error_telephony_not_available,
                        Toast.LENGTH_SHORT).show();
                L.w(TAG, e.toString());
                return false;
            }
        } else {
            L.d(TAG, "invalid number %s dialed", number);
            Toast.makeText(mContext, R.string.error_invalid_phone_number,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Places a SMS with assistant.
     */
    public boolean placeSms(Activity activity, String number, String name, String uid) {
        BluetoothDevice device = mCurrentHfpDeviceLiveData.getValue();
        Bundle bundle = buildDirectSendBundle(number, name, uid, device);
        activity.showAssist(bundle);

        return true;
    }

    /**
     * Build the {@link Bundle} to pass to assistant to send a sms.
     */
    public Bundle buildDirectSendBundle(String number, String name, String uid,
                                        BluetoothDevice device) {
        Bundle bundle = new Bundle();
        bundle.putString(CarVoiceInteractionSession.KEY_ACTION, VOICE_ACTION_SEND_SMS);
        bundle.putString(CarVoiceInteractionSession.KEY_PHONE_NUMBER, number);
        bundle.putString(CarVoiceInteractionSession.KEY_RECIPIENT_NAME, name);
        bundle.putString(CarVoiceInteractionSession.KEY_RECIPIENT_UID, uid);
        bundle.putString(CarVoiceInteractionSession.KEY_DEVICE_ADDRESS, device.getAddress());
        bundle.putString(CarVoiceInteractionSession.KEY_DEVICE_NAME,
                DialerUtils.getDeviceName(mContext, device));
        Intent intent = new Intent(mContext, MessagingService.class)
                .setAction(ACTION_DIRECT_SEND)
                .setClass(mContext, MessagingService.class);

        int requestCode = ACTION_DIRECT_SEND.hashCode();
        PendingIntent pendingIntent = PendingIntent.getForegroundService(
                mContext, requestCode, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        bundle.putParcelable(KEY_SEND_PENDING_INTENT, pendingIntent);
        return bundle;
    }

    /**
     * Runs basic validation check of a phone number, to verify it is not empty.
     */
    private boolean isValidNumber(String number) {
        if (TextUtils.isEmpty(number)) {
            return false;
        }
        return true;
    }

    public void callVoicemail() {
        L.d(TAG, "callVoicemail");

        String voicemailNumber = TelecomUtils.getVoicemailNumber(mContext);
        if (TextUtils.isEmpty(voicemailNumber)) {
            L.w(TAG, "Unable to get voicemail number.");
            return;
        }
        placeCall(voicemailNumber);
    }

    /** Check if emergency call is supported by any phone account. */
    public boolean isEmergencyCallSupported() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            L.w(TAG, "Permission is denied to get call capable phone accounts.");
            return false;
        }

        List<PhoneAccountHandle> phoneAccountHandleList =
                mTelecomManager.getCallCapablePhoneAccounts();
        for (PhoneAccountHandle phoneAccountHandle : phoneAccountHandleList) {
            PhoneAccount phoneAccount = mTelecomManager.getPhoneAccount(phoneAccountHandle);
            L.d(TAG, "phoneAccount: %s", phoneAccount);
            if (phoneAccount != null && phoneAccount.hasCapabilities(
                    PhoneAccount.CAPABILITY_PLACE_EMERGENCY_CALLS)) {
                return true;
            }
        }
        return false;
    }
}
