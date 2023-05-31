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
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;
import android.telecom.PhoneAccountHandle;

import androidx.lifecycle.LiveData;

import com.android.car.apps.common.log.L;
import com.android.car.dialer.bluetooth.PhoneAccountManager;
import com.android.car.dialer.framework.InCallServiceProxy;
import com.android.car.telephony.calling.InCallServiceManager;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * An implementation of {@link InCallService}. This service is bounded by android telecom and
 * {@link UiCallManager}. For incoming calls it will launch Dialer app.
 */
@AndroidEntryPoint(InCallServiceProxy.class)
public class InCallServiceImpl extends Hilt_InCallServiceImpl {
    private static final String TAG = "CD.InCallService";

    /** An action which indicates a bind is from local component. */
    public static final String ACTION_LOCAL_BIND = "local_bind";

    private CopyOnWriteArrayList<CallAudioStateCallback> mCallAudioStateCallbacks =
            new CopyOnWriteArrayList<>();

    private final ArrayList<ActiveCallListChangedCallback>
            mActiveCallListChangedCallbacks = new ArrayList<>();

    @Inject InCallServiceManager mInCallServiceManager;
    @Inject InCallRouter mInCallRouter;
    @Inject SelfManagedCallHandler mSelfManagedCallHandler;
    @Inject ProjectionCallHandler mProjectionCallHandler;
    @Inject PhoneAccountManager mPhoneAccountManager;
    @Inject @Named("Hfp") LiveData<BluetoothDevice> mCurrentHfpDeviceLiveData;

    /** Listens to active call list changes. Callbacks will be called on main thread. */
    public interface ActiveCallListChangedCallback {

        /**
         * Called when a new call is added.
         *
         * @return if the given call has been handled by this callback.
         */
        boolean onTelecomCallAdded(Call telecomCall);

        /**
         * Called when an existing call is removed.
         *
         * @return if the given call has been handled by this callback.
         */
        boolean onTelecomCallRemoved(Call telecomCall);
    }

    /** Listens to call audio state changes. Callbacks will be called on the main thread. */
    public interface CallAudioStateCallback {
        /**
         * Called when the call audio state has changed.
         */
        void onCallAudioStateChanged(CallAudioState callAudioState);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInCallServiceManager.setInCallService(this);
        mProjectionCallHandler.start();
        mActiveCallListChangedCallbacks.add(mProjectionCallHandler);
        mSelfManagedCallHandler.start();
        mActiveCallListChangedCallbacks.add(mSelfManagedCallHandler);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mActiveCallListChangedCallbacks.remove(mSelfManagedCallHandler);
        mSelfManagedCallHandler.stop();
        mActiveCallListChangedCallbacks.remove(mProjectionCallHandler);
        mProjectionCallHandler.stop();
        mInCallServiceManager.setInCallService(null);
    }

    @Override
    public void onCallAdded(Call telecomCall) {
        L.d(TAG, "onCallAdded: %s", telecomCall);
        if (telecomCall.getDetails().getState() == Call.STATE_SELECT_PHONE_ACCOUNT) {
            BluetoothDevice currentHfpDevice = mCurrentHfpDeviceLiveData.getValue();
            PhoneAccountHandle currentPhoneAccountHandle =
                    mPhoneAccountManager.getMatchingPhoneAccount(currentHfpDevice);
            if (currentPhoneAccountHandle != null) {
                telecomCall.phoneAccountSelected(currentPhoneAccountHandle, false);
            } else {
                L.e(TAG, "Not able to get the phone account handle for current hfp device.");
            }
        }

        if (telecomCall.getDetails().getState() == Call.STATE_RINGING) {
            telecomCall.registerCallback(new Call.Callback() {
                @Override
                public void onStateChanged(Call call, int state) {
                    // Listens to user action of answering the call or rejecting the call.
                    handleOtherActiveCalls(telecomCall);
                    telecomCall.unregisterCallback(this);
                }
            });
        } else {
            handleOtherActiveCalls(telecomCall);
        }

        boolean isHandled = routeToActiveCallListChangedCallback(telecomCall);
        if (isHandled) {
            return;
        }
        mInCallRouter.onCallAdded(telecomCall);
    }

    @Override
    public void onCallRemoved(Call telecomCall) {
        L.d(TAG, "onCallRemoved: %s", telecomCall);
        for (InCallServiceImpl.ActiveCallListChangedCallback callback :
                mActiveCallListChangedCallbacks) {
            callback.onTelecomCallRemoved(telecomCall);
        }
        mInCallRouter.onCallRemoved(telecomCall);
    }

    @Override
    public IBinder onBind(Intent intent) {
        L.d(TAG, "onBind: %s", intent);
        return ACTION_LOCAL_BIND.equals(intent.getAction())
                ? new LocalBinder()
                : super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        L.d(TAG, "onUnbind, intent: %s", intent);
        if (ACTION_LOCAL_BIND.equals(intent.getAction())) {
            return false;
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState audioState) {
        for (CallAudioStateCallback callback : mCallAudioStateCallbacks) {
            callback.onCallAudioStateChanged(audioState);
        }
    }

    @Override
    public void onBringToForeground(boolean showDialpad) {
        L.d(TAG, "onBringToForeground: %s", showDialpad);
        mInCallRouter.routeToFullScreenIncomingCallPage(true, showDialpad);
    }

    public void addCallAudioStateChangedCallback(CallAudioStateCallback callback) {
        mCallAudioStateCallbacks.add(callback);
    }

    public void removeCallAudioStateChangedCallback(CallAudioStateCallback callback) {
        mCallAudioStateCallbacks.remove(callback);
    }

    public void addActiveCallListChangedCallback(ActiveCallListChangedCallback callback) {
        mActiveCallListChangedCallbacks.add(callback);
    }

    public void removeActiveCallListChangedCallback(ActiveCallListChangedCallback callback) {
        mActiveCallListChangedCallbacks.remove(callback);
    }

    /**
     * Dispatches the call to {@link InCallServiceImpl.ActiveCallListChangedCallback}.
     */
    private boolean routeToActiveCallListChangedCallback(Call call) {
        boolean isHandled = false;
        for (InCallServiceImpl.ActiveCallListChangedCallback callback :
                mActiveCallListChangedCallbacks) {
            if (callback.onTelecomCallAdded(call)) {
                isHandled = true;
            }
        }

        return isHandled;
    }

    private void handleOtherActiveCalls(Call telecomCall) {
        // Telecom does not always put other active calls from different phone accounts on hold.
        if (telecomCall.getDetails().getState() != Call.STATE_HOLDING
                && telecomCall.getDetails().getState() != Call.STATE_DISCONNECTED) {
            for (Call call : getCallList()) {
                // Same call, do nothing.
                if (telecomCall.equals(call)) {
                    continue;
                }
                // Same phone account handle, let Telecom do the job.
                if (Objects.equals(telecomCall.getDetails().getAccountHandle(),
                        call.getDetails().getAccountHandle())) {
                    continue;
                }
                if (call.getDetails().getState() == Call.STATE_ACTIVE) {
                    if (call.getDetails().can(Call.Details.CAPABILITY_SUPPORT_HOLD)
                            || call.getDetails().can(Call.Details.CAPABILITY_HOLD)) {
                        L.i(TAG, "Hold the holdable call: %s", call);
                        call.hold();
                    } else {
                        // TODO: check with UX how to let user know the other call is ended.
                        L.i(TAG, "End the unholdable call %s", call);
                        call.disconnect();
                    }
                }
            }
        }
    }

    /**
     * Local binder only available for Car Dialer package.
     */
    public class LocalBinder extends Binder {

        /**
         * Returns a reference to {@link InCallServiceImpl}. Any process other than Dialer
         * process won't be able to get a reference.
         */
        public InCallServiceImpl getService() {
            if (getCallingPid() == Process.myPid()) {
                return InCallServiceImpl.this;
            }
            return null;
        }
    }
}
