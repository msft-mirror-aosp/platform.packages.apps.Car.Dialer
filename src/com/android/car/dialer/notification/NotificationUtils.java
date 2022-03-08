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

import android.content.Context;
import android.graphics.drawable.Icon;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.core.util.Pair;

import com.android.car.dialer.R;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.InMemoryPhoneBook;
import com.android.car.telephony.common.TelecomUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

/** Util class that shares common functionality for notifications. */
final class NotificationUtils {
    private NotificationUtils() {
    }

    static CompletableFuture<Pair<String, Icon>> getDisplayNameAndRoundedAvatar(Context context,
            String number, String accountName) {
        return CompletableFuture.supplyAsync(() -> {
            Contact contact = InMemoryPhoneBook.get().lookupContactEntryAsync(number, accountName);

            int size = context.getResources().getDimensionPixelSize(R.dimen.avatar_icon_size);
            float cornerRadiusPercent = context.getResources()
                    .getFloat(R.dimen.contact_avatar_corner_radius_percent);
            if (contact == null) {
                Icon letterTile = TelecomUtils.createLetterTile(context, null,
                        /* identifier */ null, size, cornerRadiusPercent);
                return new Pair<>(
                        TelecomUtils.getReadableNumber(context, number), letterTile);
            } else {
                Icon largeIcon = loadContactAvatar(context, contact.getAvatarUri(), size);
                if (largeIcon == null) {
                    largeIcon = TelecomUtils.createLetterTile(context, contact.getInitials(),
                            /* identifier */ contact.getDisplayName(), size, cornerRadiusPercent);
                }

                return new Pair<>(contact.getDisplayName(), largeIcon);
            }
        });
    }

    static Icon loadContactAvatar(Context context, @Nullable Uri avatarUri, int avatarSize) {
        if (avatarUri == null) {
            return null;
        }

        try {
            InputStream input = context.getContentResolver().openInputStream(avatarUri);
            if (input == null) {
                return null;
            }
            RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(
                    context.getResources(), input);
            float cornerRadiusPercent = context.getResources()
                    .getFloat(R.dimen.contact_avatar_corner_radius_percent);
            return TelecomUtils
                .createFromRoundedBitmapDrawable(roundedBitmapDrawable, avatarSize,
                    cornerRadiusPercent);
        } catch (FileNotFoundException e) {
            // No-op
        }
        return null;
    }
}
