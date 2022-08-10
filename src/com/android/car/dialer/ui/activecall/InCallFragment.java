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

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.telecom.Call;
import android.telecom.PhoneAccountHandle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.car.apps.common.BackgroundImageView;
import com.android.car.apps.common.LetterTileDrawable;
import com.android.car.apps.common.log.L;
import com.android.car.apps.common.util.ViewUtils;
import com.android.car.dialer.R;
import com.android.car.dialer.ui.view.ContactAvatarOutputlineProvider;
import com.android.car.telephony.common.CallDetail;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.PhoneNumber;
import com.android.car.telephony.common.TelecomUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A fragment that displays information about a call with actions.
 */
@AndroidEntryPoint(Fragment.class)
public abstract class InCallFragment extends Hilt_InCallFragment {
    private static final String TAG = "CD.InCallFragment";

    protected InCallViewModel mInCallViewModel;

    private View mUserProfileContainerView;
    @Nullable
    private TextView mSelfManagedCallAppInfo;
    private TextView mPhoneNumberView;
    @Nullable
    private TextView mPhoneLabelView;
    private Chronometer mUserProfileCallStateText;
    private TextView mNameView;
    private ImageView mAvatarView;
    private BackgroundImageView mBackgroundImage;
    private LetterTileDrawable mDefaultAvatar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDefaultAvatar = TelecomUtils.createLetterTile(getContext(), null, null);
        mInCallViewModel = new ViewModelProvider(getActivity()).get(InCallViewModel.class);
    }

    /**
     * Shared UI elements between ongoing call and incoming call page: {@link BackgroundImageView}
     * and {@link R.layout#user_profile_large}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mUserProfileContainerView = view.findViewById(R.id.user_profile_container);
        mNameView = mUserProfileContainerView.findViewById(R.id.user_profile_title);
        mAvatarView = mUserProfileContainerView.findViewById(R.id.user_profile_avatar);
        mAvatarView.setOutlineProvider(ContactAvatarOutputlineProvider.get());
        mSelfManagedCallAppInfo = mUserProfileContainerView.findViewById(
                R.id.self_managed_call_app_info);
        mPhoneNumberView = mUserProfileContainerView.findViewById(R.id.user_profile_phone_number);
        mPhoneLabelView = mUserProfileContainerView.findViewById(R.id.user_profile_phone_label);
        mUserProfileCallStateText = mUserProfileContainerView.findViewById(
                R.id.user_profile_call_state);
        mBackgroundImage = view.findViewById(R.id.background_image);
    }

    /**
     * Presents the user profile.
     */
    protected void presentCallDetail(@Nullable CallDetail callDetail) {
        L.i(TAG, "presentCallDetail: %s", callDetail);
        if (callDetail == null) {
            return;
        }

        ViewUtils.setVisible(mSelfManagedCallAppInfo, false);
        if (callDetail.isSelfManaged()) {
            PhoneAccountHandle phoneAccountHandle = callDetail.getPhoneAccountHandle();
            if (phoneAccountHandle != null) {
                String packageName = phoneAccountHandle.getComponentName().getPackageName();
                try {
                    PackageManager packageManager = getContext().getPackageManager();
                    ApplicationInfo applicationInfo = packageManager.getApplicationInfo(
                            packageName, PackageManager.GET_META_DATA);
                    Drawable appIcon = packageManager.getApplicationIcon(applicationInfo);
                    CharSequence appName = packageManager.getApplicationLabel(applicationInfo);

                    SpannableString spannableString = new SpannableString("  " + appName);
                    int size = getResources().getDimensionPixelSize(R.dimen.inline_icon_size);
                    appIcon.setBounds(0, 0, size, size);
                    ImageSpan imageSpan = new ImageSpan(appIcon, ImageSpan.ALIGN_BASELINE);
                    spannableString.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    mSelfManagedCallAppInfo.setText(spannableString);

                    ViewUtils.setVisible(mSelfManagedCallAppInfo, true);
                } catch (PackageManager.NameNotFoundException e) {
                    L.e(TAG, e, "Failed to get self managed call app info.");
                }
            }
        }

        String callerDisplayName = callDetail.getCallerDisplayName();
        String number = callDetail.getNumber();
        mPhoneNumberView.setVisibility(View.GONE);
        if (!TextUtils.isEmpty(callerDisplayName)) {
            mNameView.setText(callerDisplayName);
            mAvatarView.setImageDrawable(TelecomUtils.createLetterTile(
                    getContext(), TelecomUtils.getInitials(callerDisplayName), callerDisplayName));
            if (!TextUtils.isEmpty(number)) {
                mPhoneNumberView.setText(TelecomUtils.getReadableNumber(getContext(), number));
                mPhoneNumberView.setVisibility(View.VISIBLE);
            }
        } else {
            mNameView.setText(TelecomUtils.getReadableNumber(getContext(), number));
            mAvatarView.setImageDrawable(mDefaultAvatar);
        }

        ViewUtils.setVisible(mPhoneLabelView, false);
    }

    protected void presentCallerInfo(Contact contact, CallDetail callDetail) {
        presentCallDetail(callDetail);

        L.i(TAG, "presentCallerInfo: %s", contact);
        if (contact == null) {
            return;
        }

        String number = callDetail.getNumber();
        String nameViewText = contact.getDisplayName();
        String formattedNumber = TelecomUtils.getReadableNumber(getContext(), number);
        mNameView.setText(nameViewText);

        if (TextUtils.equals(nameViewText, formattedNumber)) {
            ViewUtils.setVisible(mPhoneLabelView, false);
            mPhoneNumberView.setVisibility(View.GONE);
        } else {
            PhoneNumber phoneNumber = contact.getPhoneNumber(number);
            CharSequence phoneNumberLabel = phoneNumber == null
                    ? null : phoneNumber.getReadableLabel(getResources());
            if (!TextUtils.isEmpty(phoneNumberLabel)) {
                ViewUtils.setText(mPhoneLabelView, phoneNumberLabel);
                ViewUtils.setVisible(mPhoneLabelView, true);
            } else {
                ViewUtils.setVisible(mPhoneLabelView, false);
            }
            mPhoneNumberView.setText(formattedNumber);
            mPhoneNumberView.setVisibility(View.VISIBLE);
        }

        LetterTileDrawable letterTile = TelecomUtils.createLetterTile(
                getContext(), contact.getInitials(), contact.getDisplayName());

        Glide.with(this)
                .load(contact.getAvatarUri())
                .apply(new RequestOptions().centerCrop().error(letterTile))
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        mBackgroundImage.setAlpha(getResources().getFloat(
                                R.dimen.config_background_image_error_alpha));
                        mBackgroundImage.setBackgroundColor(letterTile.getColor());
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        mBackgroundImage.setAlpha(getResources().getFloat(
                                R.dimen.config_background_image_alpha));
                        mBackgroundImage.setBackgroundDrawable(resource, false);
                        return false;
                    }
                }).into(mAvatarView);
    }

    /**
     * Presents the call state and call duration.
     */
    protected void updateCallDescription(@Nullable Pair<Integer, Long> callStateAndConnectTime) {
        if (callStateAndConnectTime == null || callStateAndConnectTime.first == null) {
            mUserProfileCallStateText.stop();
            mUserProfileCallStateText.setText("");
            return;
        }
        if (callStateAndConnectTime.first == Call.STATE_ACTIVE) {
            mUserProfileCallStateText.setBase(callStateAndConnectTime.second
                    - System.currentTimeMillis() + SystemClock.elapsedRealtime());
            mUserProfileCallStateText.start();
        } else {
            mUserProfileCallStateText.stop();
            mUserProfileCallStateText.setText(TelecomUtils.callStateToUiString(getContext(),
                    callStateAndConnectTime.first));
        }
    }
}
