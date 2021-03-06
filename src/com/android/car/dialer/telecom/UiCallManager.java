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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.car.dialer.Constants;
import com.android.car.dialer.R;
import com.android.car.dialer.bluetooth.BluetoothHeadsetClientProvider;
import com.android.car.dialer.log.L;
import com.android.car.telephony.common.TelecomUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
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
    private final BluetoothHeadsetClientProvider mBluetoothHeadsetClientProvider;
    private InCallServiceImpl mInCallService;

    @Inject
    UiCallManager(
            @ApplicationContext Context context,
            TelecomManager telecomManager,
            BluetoothHeadsetClientProvider bluetoothHeadsetClientProvider) {
        L.d(TAG, "SetUp");
        mContext = context;
        mTelecomManager = telecomManager;
        mBluetoothHeadsetClientProvider = bluetoothHeadsetClientProvider;

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
        L.d(TAG, "setMuted: " + muted);
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

    public List<Integer> getSupportedAudioRoute() {
        List<Integer> audioRouteList = new ArrayList<>();

        boolean isBluetoothPhoneCall = isBluetoothCall();
        if (isBluetoothPhoneCall) {
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
            } else if ((supportedAudioRouteMask & CallAudioState.ROUTE_BLUETOOTH) != 0) {
                audioRouteList.add(CallAudioState.ROUTE_BLUETOOTH);
            } else if ((supportedAudioRouteMask & CallAudioState.ROUTE_WIRED_HEADSET) != 0) {
                audioRouteList.add(CallAudioState.ROUTE_WIRED_HEADSET);
            } else if ((supportedAudioRouteMask & CallAudioState.ROUTE_SPEAKER) != 0) {
                audioRouteList.add(CallAudioState.ROUTE_SPEAKER);
            }
        }

        return audioRouteList;
    }

    public boolean isBluetoothCall() {
        PhoneAccountHandle phoneAccountHandle =
                mTelecomManager.getUserSelectedOutgoingPhoneAccount();
        return isBluetoothCall(phoneAccountHandle);
    }

    private boolean isBluetoothCall(@Nullable PhoneAccountHandle phoneAccountHandle) {
        if (phoneAccountHandle != null && phoneAccountHandle.getComponentName() != null) {
            return Constants.HFP_CLIENT_CONNECTION_SERVICE_CLASS_NAME.equals(
                    phoneAccountHandle.getComponentName().getClassName());
        } else {
            return false;
        }
    }

    /**
     * Returns the current audio route for given {@link PhoneAccountHandle}. If {@link
     * BluetoothHeadsetClient} hasn't been connected, return {@link CallAudioState#ROUTE_EARPIECE}.
     * The available routes are defined in {@link CallAudioState}.
     *
     * @param phoneAccountHandle the account handle for the primary ongoing call.
     */
    public int getAudioRoute(@Nullable PhoneAccountHandle phoneAccountHandle) {
        if (isBluetoothCall(phoneAccountHandle)) {
            BluetoothHeadsetClient bluetoothHeadsetClient = mBluetoothHeadsetClientProvider.get();
            // BluetoothHeadsetClient might haven't been initialized that the proxy object hasn't
            // been bind by calling BluetoothAdapter#getProfileProxy.
            if (bluetoothHeadsetClient == null) {
                return CallAudioState.ROUTE_EARPIECE;
            }

            for (BluetoothDevice bluetoothDevice : bluetoothHeadsetClient.getConnectedDevices()) {
                if (TextUtils.equals(phoneAccountHandle.getId(), bluetoothDevice.getAddress())) {
                    int audioState = bluetoothHeadsetClient.getAudioState(bluetoothDevice);
                    if (audioState == BluetoothHeadsetClient.STATE_AUDIO_CONNECTED) {
                        return CallAudioState.ROUTE_BLUETOOTH;
                    } else {
                        return CallAudioState.ROUTE_EARPIECE;
                    }
                }
            }
            // Not likely to happen.
            return CallAudioState.ROUTE_EARPIECE;
        } else {
            CallAudioState audioState = getCallAudioStateOrNull();
            int audioRoute = audioState != null ? audioState.getRoute() : 0;
            L.d(TAG, "getAudioRoute " + audioRoute);
            return audioRoute;
        }
    }

    /**
     * Re-route the audio out phone of the ongoing phone call.
     */
    public void setAudioRoute(int audioRoute, @Nullable PhoneAccountHandle phoneAccountHandle) {
        BluetoothHeadsetClient bluetoothHeadsetClient = mBluetoothHeadsetClientProvider.get();
        if (bluetoothHeadsetClient != null && isBluetoothCall(phoneAccountHandle)) {
            for (BluetoothDevice device : bluetoothHeadsetClient.getConnectedDevices()) {
                if (TextUtils.equals(phoneAccountHandle.getId(), device.getAddress())) {
                    if (audioRoute == CallAudioState.ROUTE_BLUETOOTH) {
                        bluetoothHeadsetClient.connectAudio(device);
                        setMuted(false);
                    } else if ((audioRoute & CallAudioState.ROUTE_WIRED_OR_EARPIECE) != 0) {
                        bluetoothHeadsetClient.disconnectAudio(device);
                    }
                }
            }
        }
        // TODO: Implement routing audio if current call is not a bluetooth call.
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
        if (isValidNumber(number)) {
            Uri uri = Uri.fromParts("tel", number, null);
            L.d(TAG, "android.telecom.TelecomManager#placeCall: %s", number);

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
            L.d(TAG, "invalid number dialed", number);
            Toast.makeText(mContext, R.string.error_invalid_phone_number,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
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
