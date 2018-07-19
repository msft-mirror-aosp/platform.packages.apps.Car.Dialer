/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.car.dialer.ui.listitem;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.android.car.apps.common.LetterTileDrawable;
import com.android.car.dialer.R;
import com.android.car.dialer.telecom.ContactBitmapWorker;
import com.android.car.dialer.ui.CircleBitmapDrawable;
import com.android.car.dialer.ui.ContactListFragment;

import androidx.car.widget.TextListItem;

/**
 * ListItem for contact.
 */
public class ContactListItem extends TextListItem {
    private Context mContext;
    private ContactListFragment.ContactItem mContactItem;

    public ContactListItem(Context context, ContactListFragment.ContactItem contactItem) {
        super(context);
        mContext = context;
        mContactItem = contactItem;
    }

    @Override
    public void onBind(ViewHolder viewHolder) {
        super.onBind(viewHolder);
        ContactBitmapWorker.loadBitmap(mContext.getContentResolver(), viewHolder.getPrimaryIcon(),
                mContactItem.mNumber,
                bitmap -> {
                    Resources r = mContext.getResources();
                    viewHolder.getPrimaryIcon().setScaleType(ImageView.ScaleType.CENTER);
                    Drawable avatarDrawable;
                    if (bitmap != null) {
                        avatarDrawable = new CircleBitmapDrawable(r, bitmap);
                    } else {
                        LetterTileDrawable letterTileDrawable = new LetterTileDrawable(r);
                        letterTileDrawable.setContactDetails(mContactItem.mDisplayName,
                                mContactItem.mNumber);
                        letterTileDrawable.setIsCircular(true);
                        avatarDrawable = letterTileDrawable;
                    }

                    int iconSize = mContext.getResources().getDimensionPixelSize(
                            R.dimen.avatar_icon_size);
                    setPrimaryActionIcon(scaleDrawable(avatarDrawable, iconSize), true);
                    super.onBind(viewHolder);

                    // force rebind the view.
                    super.onBind(viewHolder);
                });
        viewHolder.getContainerLayout().setBackgroundColor(
                mContext.getColor(R.color.contact_list_item_color));
    }

    private Drawable scaleDrawable(Drawable targetDrawable, int sizeInPixel) {
        Bitmap bitmap = null;
        if (targetDrawable instanceof CircleBitmapDrawable) {
            bitmap = ((CircleBitmapDrawable) targetDrawable).toBitmap(sizeInPixel);
        } else if (targetDrawable instanceof LetterTileDrawable){
            bitmap = ((LetterTileDrawable) targetDrawable).toBitmap(sizeInPixel);
        }
        return new BitmapDrawable(mContext.getResources(), bitmap);
    }
}
