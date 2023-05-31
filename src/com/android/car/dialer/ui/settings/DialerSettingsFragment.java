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

package com.android.car.dialer.ui.settings;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;

import com.android.car.dialer.R;
import com.android.car.dialer.ui.TelecomActivity;
import com.android.car.dialer.ui.TelecomActivityViewModel;
import com.android.car.ui.preference.PreferenceFragment;
import com.android.car.ui.toolbar.NavButtonMode;
import com.android.car.ui.toolbar.SearchMode;
import com.android.car.ui.toolbar.ToolbarController;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A fragment that displays the settings page
 */
@AndroidEntryPoint(PreferenceFragment.class)
public class DialerSettingsFragment extends Hilt_DialerSettingsFragment {

    private boolean mShowSettingsAsToolbarTab;

    /**
     *  Creates a new DialerSettingsFragment.
     */
    public static DialerSettingsFragment newInstance() {
        return new DialerSettingsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LiveData<String> connectedDeviceName = new ViewModelProvider(this)
                .get(DialerSettingsViewModel.class)
                .getCurrentHfpConnectedDeviceName();
        connectedDeviceName.observe(this, (name) -> {
            Preference preference = findPreference(getString((R.string.pref_connected_phone_key)));
            if (preference != null) {
                preference.setSummary(name);
            }
        });

        mShowSettingsAsToolbarTab = getResources().getBoolean(
            R.bool.config_show_settings_as_toolbar_tab);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_page, rootKey);
    }

    @Override
    protected void setupToolbar(@NonNull ToolbarController toolbar) {
        if (mShowSettingsAsToolbarTab && (requireActivity() instanceof TelecomActivity)) {
            TelecomActivityViewModel viewModel = new ViewModelProvider(requireActivity()).get(
                    TelecomActivityViewModel.class);
            LiveData<String> toolbarTitleLiveData = viewModel.getToolbarTitle();
            toolbarTitleLiveData.observe(getViewLifecycleOwner(), toolbar::setTitle);

            toolbar.setNavButtonMode(NavButtonMode.DISABLED);
            toolbar.setSearchMode(SearchMode.DISABLED);
            toolbar.setLogo(requireActivity().getDrawable(R.drawable.ic_app_icon));
            ((TelecomActivity) requireActivity()).setTabsShown(true, this);

            toolbar.setMenuItems(R.xml.menuitems);
        } else {
            super.setupToolbar(toolbar);
        }
    }

    @Override
    protected void setupChildFragmentToolbar(@NonNull Preference preference) {
        if (mShowSettingsAsToolbarTab) {
            ToolbarController toolbar = getPreferenceToolbar(this);
            if (toolbar != null) {
                toolbar.setNavButtonMode(NavButtonMode.BACK);
            }
        } else {
            super.setupChildFragmentToolbar(preference);
        }
    }
}
