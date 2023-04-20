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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;

import com.android.car.apps.common.log.L;
import com.android.car.assist.CarVoiceInteractionSession;
import com.android.car.dialer.R;
import com.android.car.dialer.bluetooth.PhoneAccountManager;
import com.android.car.dialer.sms.MessagingService;
import com.android.car.dialer.ui.common.DialerUtils;
import com.android.car.telephony.common.CallDetail;
import com.android.car.telephony.common.TelecomUtils;

import java.util.ArrayList;
import java.util.Collections;
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

    private static final String EVENT_SCO_CONNECT = "com.android.bluetooth.hfpclient.SCO_CONNECT";
    private static final String EVENT_SCO_DISCONNECT =
            "com.android.bluetooth.hfpclient.SCO_DISCONNECT";

    private Context mContext;
    private final TelecomManager mTelecomManager;
    private final PhoneAccountManager mPhoneAccountManager;
    private InCallServiceImpl mInCallService;
    private LiveData<BluetoothDevice> mCurrentHfpDeviceLiveData;

    @Inject
    UiCallManager(
            @ApplicationContext Context context,
            TelecomManager telecomManager,
            PhoneAccountManager phoneAccountManager,
            @Named("Hfp") LiveData<BluetoothDevice> currentHfpDeviceLiveData) {
        L.d(TAG, "SetUp");
        mContext = context;
        mTelecomManager = telecomManager;
        mPhoneAccountManager = phoneAccountManager;
        mCurrentHfpDeviceLiveData = currentHfpDeviceLiveData;

        Intent intent = new Intent(context, InCallServiceImpl.class);
        intent.setAction(InCallServiceImpl.ACTION_LOCAL_BIND);
        context.bindService(intent, mInCallServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection mInCallServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            L.d(TAG, "onServiceConnected: %s, service: %s", name, binder);
            mInCallService = ((InCallServiceImpl.LocalBinder) binder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            L.d(TAG, "onServiceDisconnected: %s", name);
            mInCallService = null;
        }
    };

    /**
     * Tears down the {@link UiCallManager}. Calling this function will null out the global
     * accessible {@link UiCallManager} instance. Remember to re-initialize the
     * {@link UiCallManager}.
     */
    public void tearDown() {
        if (mInCallService != null) {
            mContext.unbindService(mInCallServiceConnection);
            mInCallService = null;
        }
        // Clear out the mContext reference to avoid memory leak.
        mContext = null;
    }

    public boolean getMuted() {
        L.d(TAG, "getMuted");
        if (mInCallService == null) {
            return false;
        }
        CallAudioState audioState = mInCallService.getCallAudioState();
        return audioState != null && audioState.isMuted();
    }

    public void setMuted(boolean muted) {
        L.d(TAG, "setMuted: %b", muted);
        if (mInCallService == null) {
            return;
        }
        mInCallService.setMuted(muted);
    }

    public int getSupportedAudioRouteMask() {
        L.d(TAG, "getSupportedAudioRouteMask");

        CallAudioState audioState = getCallAudioStateOrNull();
        return audioState != null ? audioState.getSupportedRouteMask() : 0;
    }

    /** Returns a list of supported CallAudioRoute for the given {@link PhoneAccountHandle}. */
    public List<Integer> getSupportedAudioRoute(@Nullable PhoneAccountHandle phoneAccountHandle) {
        List<Integer> audioRouteList = new ArrayList<>();

        BluetoothDevice device = mPhoneAccountManager.getMatchingDevice(phoneAccountHandle);
        if (device != null) {
            // if this is bluetooth phone call, we can only select audio route between vehicle
            // and phone.
            // Vehicle speaker route.
            audioRouteList.add(CallAudioState.ROUTE_BLUETOOTH);
            // Headset route.
            audioRouteList.add(CallAudioState.ROUTE_EARPIECE);
        } else {
            // Most likely we are making phone call with on board SIM card.
            int supportedAudioRouteMask = getSupportedAudioRouteMask();

            if ((supportedAudioRouteMask & CallAudioState.ROUTE_EARPIECE) != 0) {
                audioRouteList.add(CallAudioState.ROUTE_EARPIECE);
            } else if ((supportedAudioRouteMask & CallAudioState.ROUTE_WIRED_HEADSET) != 0) {
                audioRouteList.add(CallAudioState.ROUTE_WIRED_HEADSET);
            }
            if ((supportedAudioRouteMask & CallAudioState.ROUTE_BLUETOOTH) != 0) {
                audioRouteList.add(CallAudioState.ROUTE_BLUETOOTH);
            }
            if ((supportedAudioRouteMask & CallAudioState.ROUTE_SPEAKER) != 0) {
                audioRouteList.add(CallAudioState.ROUTE_SPEAKER);
            }
        }

        return audioRouteList;
    }

    /**
     * Returns the current audio route given the SCO state. See {@link CallDetail#getScoState()}.
     * The available routes are defined in {@link CallAudioState}.
     */
    public int getAudioRoute(int scoState) {
        if (scoState != CallDetail.STATE_AUDIO_ERROR) {
            if (scoState == CallDetail.STATE_AUDIO_CONNECTED) {
                return CallAudioState.ROUTE_BLUETOOTH;
            } else {
                return CallAudioState.ROUTE_EARPIECE;
            }
        } else {
            CallAudioState audioState = getCallAudioStateOrNull();
            int audioRoute = audioState != null ? audioState.getRoute() : 0;
            L.d(TAG, "getAudioRoute: %d", audioRoute);
            return audioRoute;
        }
    }

    /**
     * Re-route the audio out phone of the ongoing phone call.
     */
    public void setAudioRoute(int audioRoute, Call primaryCall) {
        if (primaryCall == null) {
            return;
        }

        boolean isConference = !primaryCall.getChildren().isEmpty()
                && primaryCall.getDetails().hasProperty(Call.Details.PROPERTY_CONFERENCE);
        Call call = isConference ? primaryCall.getChildren().get(0) : primaryCall;

        // For bluetooth call, we need to send the sco call events for hfp client to handle.
        if (audioRoute == CallAudioState.ROUTE_BLUETOOTH) {
            call.sendCallEvent(EVENT_SCO_CONNECT, null);
            setMuted(false);
        } else if ((audioRoute & CallAudioState.ROUTE_WIRED_OR_EARPIECE) != 0) {
            call.sendCallEvent(EVENT_SCO_DISCONNECT, null);
        }
        // The following doesn't really switch audio route for a bluetooth call. The api works only
        //  if the call is non bluetooth call(a self managed call for example).
        if (mInCallService != null) {
            mInCallService.setAudioRoute(audioRoute);
        }
    }

    private CallAudioState getCallAudioStateOrNull() {
        return mInCallService != null ? mInCallService.getCallAudioState() : null;
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

    /** Return the current active call list from delegated {@link InCallServiceImpl} */
    public List<Call> getCallList() {
        return mInCallService == null ? Collections.emptyList() : mInCallService.getCallList();
    }
}
