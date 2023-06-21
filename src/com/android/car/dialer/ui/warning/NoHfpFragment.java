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

package com.android.car.dialer.ui.warning;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.android.car.apps.common.UxrButton;
import com.android.car.apps.common.util.ViewUtils;
import com.android.car.dialer.R;
import com.android.car.dialer.framework.UxrButtonDecorator;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.ui.TelecomActivity;
import com.android.car.dialer.ui.common.DialerBaseFragment;
import com.android.car.dialer.ui.dialpad.DialpadFragment;
import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.toolbar.NavButtonMode;
import com.android.car.ui.toolbar.SearchMode;
import com.android.car.ui.toolbar.ToolbarController;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A fragment that informs the user that there is no bluetooth device attached that can make
 * phone calls.
 */
@AndroidEntryPoint(DialerBaseFragment.class)
public class NoHfpFragment extends Hilt_NoHfpFragment {
    @Inject UiCallManager mUiCallManager;
    @Inject @Named("ConnectToBluetooth") UxrButtonDecorator mConnectToBluetoothButtonDecorator;
    private TextView mErrorMessageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.no_hfp, container, false);
        mErrorMessageView = view.findViewById(R.id.error_string);

        LiveData<String> errorStringLiveData = new ViewModelProvider(getActivity())
                .get(NoHfpViewModel.class)
                .getBluetoothErrorStringLiveData();

        errorStringLiveData.observe(getViewLifecycleOwner(), error -> {
            // Do not set the NO_BT_ERROR message to avoid UI jankiness
            if (!BluetoothErrorStringLiveData.NO_BT_ERROR.equals(error)) {
                mErrorMessageView.setText(error);
            }
        });

        View emergencyButton = view.findViewById(R.id.emergency_call_button);
        ViewUtils.setVisible(emergencyButton, mUiCallManager.isEmergencyCallSupported());
        emergencyButton.setOnClickListener(v ->
                ((TelecomActivity) requireActivity()).pushContentFragment(
                        DialpadFragment.newEmergencyDialpad(), null));

        UxrButton bluetoothButton = view.findViewById(R.id.connect_bluetooth_button);
        mConnectToBluetoothButtonDecorator.decorate(bluetoothButton);

        return view;
    }

    @Override
    public void onCarUiInsetsChanged(Insets insets) {
        requireView().setPadding(0, 0, 0, 0);
    }

    @Override
    protected void setupToolbar(@NonNull ToolbarController toolbar) {
        ((TelecomActivity) requireActivity()).setTabsShown(false, this);
        toolbar.setLogo(null);
        toolbar.setTitle((CharSequence) null);
        toolbar.setMenuItems(null);
        toolbar.setNavButtonMode(NavButtonMode.DISABLED);
        toolbar.setSearchMode(SearchMode.DISABLED);
    }
}
