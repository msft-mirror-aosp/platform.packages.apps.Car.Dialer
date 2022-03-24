/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.car.dialer.ui.dialpad;

import static android.car.drivingstate.CarUxRestrictions.UX_RESTRICTIONS_NO_DIALPAD;

import android.car.Car;
import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.android.car.dialer.R;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * A view model to track the current dialpad restriction based.
 *
 * The current dialpad mode derives from the {@code CarUxRestrictionsManager} input, emergency
 * numbers dialing restriction configuration and the digit already dialed.
 */
@HiltViewModel
public class DialpadRestrictionViewModel extends ViewModel {
    /** Current dialpad restriction mode descriptor. */
    public enum DialpadUxrMode {
        /** User can dial any number. */
        UNRESTRICTED,
        /**
         * Dialing is restricted to emergency numbers only and the digit count limit is not yet
         * reached.
         */
        RESTRICTED,
        /**
         * User cannot dial further digits: either the emergency number digit count limit is reached
         * or the dialing is restricted entirely.
         */
        RESTRICTED_LIMIT_REACHED,
        /** Dialing is disallowed. */
        DISABLED
    }

    private final CarUxRestrictionsManager mRestrictionsManager;

    private final MutableLiveData<DialpadUxrMode> mDialPadMode =
            new MutableLiveData<>(DialpadUxrMode.UNRESTRICTED);

    private final LiveData<DialpadUxrMode> mObservableDialPadMode =
            Transformations.distinctUntilChanged(mDialPadMode);

    private final int mRestrictedModeMaxDigitCount;

    @NonNull
    private String mCurrentPhoneNumber = "";

    @Inject
    DialpadRestrictionViewModel(@ApplicationContext Context context, Car car) {
        mRestrictedModeMaxDigitCount = getNoDialpadUxrMaxAllowedDigits(context);

        if (shouldEnforceNoDialpadRestriction(mRestrictedModeMaxDigitCount)) {
            mRestrictionsManager =
                    (CarUxRestrictionsManager) car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE);

            onUxRestrictionsChanged(mRestrictionsManager.getCurrentCarUxRestrictions());
            mRestrictionsManager.registerListener(this::onUxRestrictionsChanged);
        } else {
            mRestrictionsManager = null;
        }
    }

    @Override
    protected void onCleared() {
        if (mRestrictionsManager != null) {
            mRestrictionsManager.unregisterListener();
        }
    }

    private static int getNoDialpadUxrMaxAllowedDigits(@NonNull Context context) {
        return context.getResources().getInteger(R.integer.config_no_dialpad_uxr_max_allow_digits);
    }

    private static boolean shouldEnforceNoDialpadRestriction(int restrictedModeMaxDigitCount) {
        return restrictedModeMaxDigitCount >= 0;
    }

    /**
     * Return {@code true} if the current configuration implies enforcement of "no dialpad" UXR
     * restriction.
     */
    public static boolean shouldEnforceNoDialpadRestriction(@NonNull Context context) {
        return shouldEnforceNoDialpadRestriction(getNoDialpadUxrMaxAllowedDigits(context));
    }

    /**
     * Returns a {@code LiveData} to observe the current dialpad mode.
     *
     * Note: only state changes are propagated to observers.
     */
    public LiveData<DialpadUxrMode> getDialpadMode() {
        return mObservableDialPadMode;
    }

    /**
     * Updates the currently dialed part of the phone number, which is immediately used to evaluate
     * the restriction.
     */
    public void setCurrentPhoneNumber(@NonNull String phoneNumber) {
        mCurrentPhoneNumber = phoneNumber;

        if (mDialPadMode.getValue() != DialpadUxrMode.UNRESTRICTED) {
            mDialPadMode.setValue(evaluateDialpadRestrictionMode(mCurrentPhoneNumber));
        }
    }

    private DialpadUxrMode evaluateDialpadMode(CarUxRestrictions restrictions, String phoneNumber) {
        return (restrictions.getActiveRestrictions() & UX_RESTRICTIONS_NO_DIALPAD) == 0
                ? DialpadUxrMode.UNRESTRICTED : evaluateDialpadRestrictionMode(phoneNumber);
    }

    /**
     * Evaluates the dialpad mode for a given phone number, assuming the dialpad restrictions are
     * enforced by the configuration.
     */
    private DialpadUxrMode evaluateDialpadRestrictionMode(String phoneNumber) {
        if (mRestrictedModeMaxDigitCount == 0) {
            return DialpadUxrMode.DISABLED;
        } else if (phoneNumber.length() < mRestrictedModeMaxDigitCount) {
            return DialpadUxrMode.RESTRICTED;
        } else {
            return DialpadUxrMode.RESTRICTED_LIMIT_REACHED;
        }
    }

    private void onUxRestrictionsChanged(CarUxRestrictions restrictions) {
        mDialPadMode.setValue(evaluateDialpadMode(restrictions, mCurrentPhoneNumber));
    }
}

