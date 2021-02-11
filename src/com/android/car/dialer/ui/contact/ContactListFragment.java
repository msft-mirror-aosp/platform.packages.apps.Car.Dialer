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

package com.android.car.dialer.ui.contact;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.android.car.dialer.Constants;
import com.android.car.dialer.R;
import com.android.car.dialer.ui.common.DialerListBaseFragment;
import com.android.car.telephony.common.Contact;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Contact Fragment.
 */
@AndroidEntryPoint(DialerListBaseFragment.class)
public class ContactListFragment extends Hilt_ContactListFragment {
    @Inject ContactListAdapterFactory mContactListAdapterFactory;
    private ContactListAdapter mContactListAdapter;

    public static ContactListFragment newInstance() {
        return new ContactListFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Don't recreate the adapter if we already have one, so that the list items
        // will display immediately upon the view being recreated. If they're not displayed
        // immediately, we won't remember our scroll position.
        if (mContactListAdapter == null) {
            mContactListAdapter =
                    mContactListAdapterFactory.create(contact->onShowContactDetail(contact));
        }
        getRecyclerView().setAdapter(mContactListAdapter);
        getUxrContentLimiter().setAdapter(mContactListAdapter);

        ContactListViewModel contactListViewModel = ViewModelProviders.of(this).get(
                ContactListViewModel.class);
        contactListViewModel.getAllContacts().observe(this, contacts -> {
            if (contacts.isLoading()) {
                showLoading();
            } else if (contacts.getData() == null) {
                showEmpty(Constants.INVALID_RES_ID, R.string.contact_list_empty,
                        R.string.available_after_sync);
            } else {
                mContactListAdapter.setContactList(contacts.getData());
                showContent();
            }
        });
    }

    private void onShowContactDetail(Contact contact) {
        Fragment contactDetailsFragment = ContactDetailsFragment.newInstance(contact);
        pushContentFragment(contactDetailsFragment, ContactDetailsFragment.FRAGMENT_TAG);
    }
}
