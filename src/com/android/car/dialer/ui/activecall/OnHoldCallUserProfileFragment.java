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
import android.os.SystemClock;
import android.telecom.Call;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.android.car.apps.common.LetterTileDrawable;
import com.android.car.dialer.R;
import com.android.car.dialer.ui.view.ContactAvatarOutputlineProvider;
import com.android.car.telephony.common.CallDetail;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.TelecomUtils;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A fragment that displays information about onhold call.
 */
@AndroidEntryPoint(Fragment.class)
public class OnHoldCallUserProfileFragment extends Hilt_OnHoldCallUserProfileFragment {

    private InCallViewModel mInCallViewModel;

    private TextView mTitle;
    private ImageView mAvatarView;
    private View mSwapCallsView;
    private LiveData<Call> mPrimaryCallLiveData;
    private LiveData<Call> mSecondaryCallLiveData;
    private LetterTileDrawable mDefaultAvatar;
    private Chronometer mTimeTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDefaultAvatar = TelecomUtils.createLetterTile(getContext(), null, null);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.onhold_user_profile, container, false);

        mTitle = fragmentView.findViewById(R.id.title);
        mAvatarView = fragmentView.findViewById(R.id.icon);
        mAvatarView.setOutlineProvider(ContactAvatarOutputlineProvider.get());

        mSwapCallsView = fragmentView.findViewById(R.id.swap_calls_view);
        mSwapCallsView.setOnClickListener(v -> swapCalls());

        mInCallViewModel = new ViewModelProvider(getActivity()).get(InCallViewModel.class);
        mInCallViewModel.getSecondaryCallerInfoLiveData().observe(
                getViewLifecycleOwner(), this::updateProfile);
        mPrimaryCallLiveData = mInCallViewModel.getPrimaryCall();
        mSecondaryCallLiveData = mInCallViewModel.getSecondaryCall();

        mTimeTextView = fragmentView.findViewById(R.id.time);
        mInCallViewModel.getSecondaryCallConnectTime().observe(
                getViewLifecycleOwner(), this::updateConnectTime);

        return fragmentView;
    }

    /** Presents the onhold call duration. */
    protected void updateConnectTime(Long connectTime) {
        if (connectTime == null) {
            mTimeTextView.stop();
            mTimeTextView.setText("");
            return;
        }
        mTimeTextView.setBase(connectTime
                - System.currentTimeMillis() + SystemClock.elapsedRealtime());
        mTimeTextView.start();
    }

    private void updateProfileInfo(@Nullable CallDetail callDetail) {
        if (callDetail == null) {
            return;
        }

        if (callDetail.isConference()) {
            mTitle.setText(getString(R.string.ongoing_conf_title));
            mAvatarView.setImageDrawable(mDefaultAvatar);
            return;
        }
        String callerDisplayName = callDetail.getCallerDisplayName();
        if (TextUtils.isEmpty(callerDisplayName)) {
            mAvatarView.setImageDrawable(mDefaultAvatar);
            String number = callDetail.getNumber();
            mTitle.setText(TelecomUtils.getReadableNumber(getContext(), number));
        } else {
            mTitle.setText(callerDisplayName);
            mAvatarView.setImageDrawable(
                    TelecomUtils.createLetterTile(
                            getContext(),
                            TelecomUtils.getInitials(callerDisplayName),
                            callerDisplayName));
        }
    }

    private void updateProfile(Contact contact) {
        CallDetail callDetail = mInCallViewModel.getSecondaryCallDetail().getValue();
        updateProfileInfo(callDetail);
        if (contact == null) {
            return;
        }

        mTitle.setText(contact.getDisplayName());
        TelecomUtils.setContactBitmapAsync(getContext(), mAvatarView,
                contact.getAvatarUri(), contact.getInitials(), contact.getDisplayName());
    }

    private void swapCalls() {
        Call primaryCall = mPrimaryCallLiveData.getValue();
        Call secondaryCall = mSecondaryCallLiveData.getValue();

        // Hold primary call and the secondary call will automatically come to the foreground
        // for the same phone account handle.
        if (primaryCall.getDetails().getState() != Call.STATE_HOLDING) {
            primaryCall.hold();
        }

        // For different phone account handles, we will unhold the other call.
        if (!Objects.equals(
                primaryCall.getDetails().getAccountHandle(),
                secondaryCall.getDetails().getAccountHandle())) {
            secondaryCall.unhold();
        }
    }
}
