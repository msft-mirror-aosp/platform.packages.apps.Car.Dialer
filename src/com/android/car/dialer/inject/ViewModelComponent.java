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

package com.android.car.dialer.inject;

import com.android.car.dialer.ui.TelecomActivityViewModel;
import com.android.car.dialer.ui.activecall.InCallViewModel;
import com.android.car.dialer.ui.calllog.CallHistoryViewModel;
import com.android.car.dialer.ui.common.DialerListViewModel;
import com.android.car.dialer.ui.contact.ContactDetailsViewModel;
import com.android.car.dialer.ui.contact.ContactListViewModel;
import com.android.car.dialer.ui.dialpad.TypeDownResultsViewModel;
import com.android.car.dialer.ui.favorite.FavoriteViewModel;
import com.android.car.dialer.ui.search.ContactResultsViewModel;
import com.android.car.dialer.ui.settings.DialerSettingsViewModel;
import com.android.car.dialer.ui.warning.NoHfpViewModel;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Component for injecting {@link androidx.lifecycle.AndroidViewModel}s.
 * TODO: use @HiltViewModel or androidx.hilt for ViewModel injection.
 */
@EntryPoint
@InstallIn(SingletonComponent.class)
public interface ViewModelComponent {
    /** Inject dependencies to the {@link TelecomActivityViewModel}. */
    void inject(TelecomActivityViewModel telecomActivityViewModel);

    /** Inject dependencies to the {@link InCallViewModel}. */
    void inject(InCallViewModel inCallViewModel);

    /** Inject dependencies to the {@link NoHfpViewModel}. */
    void inject(NoHfpViewModel noHfpViewModel);

    /** Inject dependencies to the {@link DialerSettingsViewModel}. */
    void inject(DialerSettingsViewModel dialerSettingsViewModel);

    /** Inject dependencies to the {@link DialerListViewModel}. */
    void inject(DialerListViewModel dialerListViewModel);

    /** Inject dependencies to the {@link ContactListViewModel}. */
    void inject(ContactListViewModel contactListViewModel);

    /** Inject dependencies to the {@link ContactResultsViewModel}. */
    void inject(CallHistoryViewModel callHistoryViewModel);

    /** Inject dependencies to the {@link FavoriteViewModel}. */
    void inject(FavoriteViewModel favoriteViewModel);

    /** Inject dependencies to the {@link ContactDetailsViewModel}. */
    void inject(ContactDetailsViewModel contactDetailsViewModel);

    /** Inject dependencies to the {@link ContactResultsViewModel}. */
    void inject(ContactResultsViewModel contactResultsViewModel);

    /** Inject dependencies to the {@link TypeDownResultsViewModel}. */
    void inject(TypeDownResultsViewModel typeDownResultsViewModel);
}
