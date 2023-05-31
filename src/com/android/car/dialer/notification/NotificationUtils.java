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
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.core.util.Pair;

import com.android.car.apps.common.UriUtils;
import com.android.car.apps.common.log.L;
import com.android.car.dialer.R;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.InMemoryPhoneBook;
import com.android.car.telephony.common.TelecomUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Util class that shares common functionality for notifications.
 */
final class NotificationUtils {
    private static final String TAG = "CD.NotificationUtils";
    private NotificationUtils() {
    }

    static CompletableFuture<Pair<String, Icon>> getDisplayNameAndRoundedAvatar(
            Context context, String number, String accountName) {
        return CompletableFuture.supplyAsync(() -> {
            Contact contact = InMemoryPhoneBook.get().lookupContactEntryAsync(number, accountName);
            if (contact == null) {
                Icon letterTile = loadAvatar(context, null, null, null);
                return new Pair<>(
                        TelecomUtils.getReadableNumber(context, number), letterTile);
            } else {
                Icon largeIcon = loadAvatar(context, contact.getAvatarUri(), contact.getInitials(),
                        contact.getDisplayName());
                return new Pair<>(contact.getDisplayName(), largeIcon);
            }
        });
    }

    static CompletableFuture<Pair<String, Icon>> getDisplayNameAndRoundedAvatar(
            Context context, String number, String accountName, String callerDisplayName,
            Uri callerImageUri) {
        return CompletableFuture.supplyAsync(() -> {
            String displayName = TextUtils.isEmpty(callerDisplayName)
                    ? TelecomUtils.getReadableNumber(context, number) : callerDisplayName;
            String initials = TelecomUtils.getInitials(displayName);
            String identifier = displayName;
            Uri avatarUri = callerImageUri;

            Contact contact = InMemoryPhoneBook.get().lookupContactEntryAsync(number, accountName);
            if (contact != null) {
                displayName = contact.getDisplayName();
                initials = contact.getInitials();
                identifier = contact.getDisplayName();
                if (contact.getAvatarUri() != null) {
                    avatarUri = contact.getAvatarUri();
                }
            }

            Icon avatar = loadAvatar(context, avatarUri, initials, identifier);
            return new Pair<>(displayName, avatar);
        });
    }

    @NonNull
    static Icon loadAvatar(
            Context context, @Nullable Uri avatarUri, String initials, String identifier) {
        int avatarSize = context.getResources().getDimensionPixelSize(R.dimen.avatar_icon_size);
        float cornerRadiusPercent = context.getResources()
                .getFloat(R.dimen.contact_avatar_corner_radius_percent);
        if (avatarUri != null) {
            try {
                InputStream input = null;
                if (UriUtils.isContentUri(avatarUri)) {
                    input = context.getContentResolver().openInputStream(avatarUri);
                } else if (UriUtils.isWebUri(avatarUri)) {
                    input = new URL(avatarUri.toString()).openStream();
                }
                if (input != null) {
                    RoundedBitmapDrawable roundedBitmapDrawable =
                            RoundedBitmapDrawableFactory.create(context.getResources(), input);
                    return TelecomUtils
                            .createFromRoundedBitmapDrawable(roundedBitmapDrawable, avatarSize,
                                    cornerRadiusPercent);
                }
            } catch (FileNotFoundException e) {
                L.w(TAG, "Failed to load the avatar, file not found.");
            } catch (IOException e) {
                L.w(TAG, "Failed to load the avatar: %s", avatarUri);
            }
        }
        return TelecomUtils.createLetterTile(context, initials, identifier, avatarSize,
                cornerRadiusPercent);
    }
}
