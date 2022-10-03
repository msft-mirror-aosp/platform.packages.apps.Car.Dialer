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

package com.android.car.dialer.ui.activecall;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.android.car.apps.common.BackgroundImageView;
import com.android.car.dialer.R;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A fragment that displays information about an on-going call with options to hang up.
 */
@AndroidEntryPoint(InCallFragment.class)
public class OngoingCallFragment extends Hilt_OngoingCallFragment {
    private Fragment mDialpadFragment;
    private Fragment mOnholdCallFragment;
    private View mUserProfileContainerView;
    private BackgroundImageView mBackgroundImage;
    private MutableLiveData<Boolean> mDialpadState;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.ongoing_call_fragment, container, false);

        mUserProfileContainerView = fragmentView.findViewById(R.id.user_profile_container);
        mBackgroundImage = fragmentView.findViewById(R.id.background_image);
        mOnholdCallFragment = getChildFragmentManager().findFragmentById(R.id.onhold_user_profile);
        mDialpadFragment = getChildFragmentManager().findFragmentById(R.id.incall_dialpad_fragment);

        mInCallViewModel.getPrimaryCallDetail().observe(
                getViewLifecycleOwner(), callDetail -> presentCallDetail(callDetail));
        mInCallViewModel.getPrimaryCallerInfoLiveData().observe(getViewLifecycleOwner(),
                contact -> presentCallerInfo(
                        contact, mInCallViewModel.getPrimaryCallDetail().getValue()));
        mInCallViewModel.getCallStateAndConnectTime().observe(getViewLifecycleOwner(),
                this::updateCallDescription);

        mDialpadState = mInCallViewModel.getDialpadOpenState();

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                if (mDialpadState.getValue()) {
                    mDialpadState.setValue(Boolean.FALSE);
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), callback);
        mDialpadState.observe(getViewLifecycleOwner(), isDialpadOpen -> {
            callback.setEnabled(isDialpadOpen);
            if (isDialpadOpen) {
                onOpenDialpad();
            } else {
                onCloseDialpad();
            }
        });

        mInCallViewModel.shouldShowOnholdCall().observe(getViewLifecycleOwner(),
                this::maybeShowOnholdCallFragment);

        return fragmentView;
    }

    @VisibleForTesting
    void onOpenDialpad() {
        getChildFragmentManager().beginTransaction()
                .show(mDialpadFragment)
                .commit();
        mUserProfileContainerView.setVisibility(View.GONE);
        mBackgroundImage.setDimmed(true);
    }

    @VisibleForTesting
    void onCloseDialpad() {
        getChildFragmentManager().beginTransaction()
                .hide(mDialpadFragment)
                .commit();
        mUserProfileContainerView.setVisibility(View.VISIBLE);
        mBackgroundImage.setDimmed(false);
    }

    private void maybeShowOnholdCallFragment(Boolean showOnholdCall) {
        if (showOnholdCall) {
            getChildFragmentManager().beginTransaction().show(mOnholdCallFragment).commit();
        } else {
            getChildFragmentManager().beginTransaction().hide(mOnholdCallFragment).commit();
        }
    }
}
