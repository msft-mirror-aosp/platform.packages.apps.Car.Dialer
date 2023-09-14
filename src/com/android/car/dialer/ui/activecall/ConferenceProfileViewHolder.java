/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.apps.common.util.ViewUtils;
import com.android.car.dialer.R;
import com.android.car.dialer.ui.view.ContactAvatarOutputlineProvider;
import com.android.car.telephony.common.CallDetail;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.InMemoryPhoneBook;
import com.android.car.telephony.common.TelecomUtils;

/**
 * View holder for a user profile of a conference
 */
public class ConferenceProfileViewHolder extends RecyclerView.ViewHolder {

    private ImageView mAvatar;
    private TextView mTitle;
    private TextView mNumber;
    @Nullable
    private TextView mLabel;
    private Context mContext;

    ConferenceProfileViewHolder(View v) {
        super(v);

        mAvatar = v.findViewById(R.id.user_profile_avatar);
        mAvatar.setOutlineProvider(ContactAvatarOutputlineProvider.get());
        mTitle = v.findViewById(R.id.user_profile_title);
        mNumber = v.findViewById(R.id.user_profile_phone_number);
        mLabel = v.findViewById(R.id.user_profile_phone_label);
        mContext = v.getContext();
    }

    /**
     * Binds call details to the profile views
     */
    public void bind(CallDetail callDetail) {
        String number = callDetail.getNumber();
        Contact contact = InMemoryPhoneBook.get().lookupContactEntry(
                number, callDetail.getPhoneAccountName());
        String readableNumber = TelecomUtils.getReadableNumber(mContext, number);
        String callerDisplayName = callDetail.getCallerDisplayName();
        String initials;
        String identifier;
        if (!TextUtils.isEmpty(callerDisplayName)) {
            mTitle.setText(callerDisplayName);
            mNumber.setText(readableNumber);
            mNumber.setVisibility(View.VISIBLE);
            initials = TelecomUtils.getInitials(callerDisplayName);
            identifier = callerDisplayName;
        } else if (contact == null) {
            mTitle.setText(readableNumber);
            mNumber.setVisibility(View.GONE);
            initials = null;
            identifier = null;
        } else {
            mTitle.setText(contact.getDisplayName());
            mNumber.setText(readableNumber);
            mNumber.setVisibility(View.VISIBLE);
            initials = contact.getInitials();
            identifier = contact.getDisplayName();
        }

        if (contact == null) {
            ViewUtils.setVisible(mLabel, false);
        } else {
            CharSequence phoneNumberLabel = contact.getPhoneNumber(number).getReadableLabel(
                    mContext.getResources());
            if (!TextUtils.isEmpty(phoneNumberLabel)) {
                ViewUtils.setText(mLabel, phoneNumberLabel);
                ViewUtils.setVisible(mLabel, true);
            } else {
                ViewUtils.setVisible(mLabel, false);
            }
        }

        Uri callerImageUri = callDetail.getCallerImageUri();
        if (callerImageUri == null && contact != null) {
            callerImageUri = contact.getAvatarUri();
        }
        TelecomUtils.setContactBitmapAsync(mContext, mAvatar, callerImageUri, initials, identifier);
    }
}
