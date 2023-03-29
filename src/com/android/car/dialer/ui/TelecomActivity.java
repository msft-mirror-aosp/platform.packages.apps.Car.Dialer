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

package com.android.car.dialer.ui;

import android.Manifest;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.CallLog;
import android.telecom.Call;
import android.telephony.PhoneNumberUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.android.car.apps.common.log.L;
import com.android.car.apps.common.util.Themes;
import com.android.car.dialer.R;
import com.android.car.dialer.notification.NotificationService;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.ui.activecall.InCallActivity;
import com.android.car.dialer.ui.common.DialerBaseFragment;
import com.android.car.dialer.ui.common.OnItemClickedListener;
import com.android.car.dialer.ui.dialpad.DialpadFragment;
import com.android.car.dialer.ui.search.ContactResultsFragment;
import com.android.car.dialer.ui.settings.DialerSettingsActivity;
import com.android.car.dialer.ui.warning.NoHfpFragment;
import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.baselayout.InsetsChangedListener;
import com.android.car.ui.core.CarUi;
import com.android.car.ui.toolbar.MenuItem;
import com.android.car.ui.toolbar.TabLayout;
import com.android.car.ui.toolbar.ToolbarController;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Main activity for the Dialer app. It hosts most of the fragments for the app.
 *
 * <p>Start {@link InCallActivity} if there are ongoing calls
 *
 * <p>Based on call and connectivity status, it will choose the right page to display.
 */
@AndroidEntryPoint(FragmentActivity.class)
public class TelecomActivity extends Hilt_TelecomActivity implements
        DialerBaseFragment.DialerFragmentParent, InsetsChangedListener {
    private static final String TAG = "CD.TelecomActivity";

    @Inject SharedPreferences mSharedPreferences;
    @Inject UiCallManager mUiCallManager;
    private LiveData<List<Call>> mOngoingCallListLiveData;
    private LiveData<Boolean> mRefreshUiLiveData;
    // View objects for this activity.
    private TelecomPageTab.Factory mTabFactory;
    private boolean mTabsShown = true;
    private ToolbarController mCarUiToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        L.d(TAG, "onCreate");

        setContentView(R.layout.telecom_activity);

        mCarUiToolbar = CarUi.requireToolbar(this);

        setupTabLayout();

        TelecomActivityViewModel viewModel = new ViewModelProvider(this).get(
                TelecomActivityViewModel.class);

        mRefreshUiLiveData = viewModel.getRefreshTabsLiveData();
        mRefreshUiLiveData.observe(this, v -> refreshUi());

        LiveData<Boolean> hasHfpDeviceConnectedLiveData = viewModel.hasHfpDeviceConnected();
        hasHfpDeviceConnectedLiveData.observe(this, hasHfpDeviceConnected -> {
            if (!Boolean.TRUE.equals(hasHfpDeviceConnected)) {
                setContentFragment(new NoHfpFragment(), NoHfpFragment.class.getName());
            } else {
                getSupportFragmentManager().executePendingTransactions();
                if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
                    // Retain the backstack when day/night mode change.
                    return;
                }
                int tabIndex = mTabFactory.getSelectedTabIndex();
                showTabPage(tabIndex);
            }
        });

        MutableLiveData<Integer> toolbarTitleMode = viewModel.getToolbarTitleMode();
        toolbarTitleMode.setValue(Themes.getAttrInteger(this, R.attr.toolbarTitleMode));

        mOngoingCallListLiveData = viewModel.getOngoingCallListLiveData();
        mOngoingCallListLiveData.observe(this, list -> maybeStartInCallActivity(list));

        handleIntent();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        handleIntent();
    }

    private void handleIntent() {
        Intent intent = getIntent();
        String action = intent != null ? intent.getAction() : null;
        L.i(TAG, "handleIntent, intent: %s, action: %s", intent, action);
        if (action == null || action.length() == 0) {
            return;
        }

        String number;
        switch (action) {
            case Intent.ACTION_DIAL:
                number = PhoneNumberUtils.getNumberFromIntent(intent, this);
                showDialPadFragment(number);
                break;

            case Intent.ACTION_CALL:
                if (checkCallingPermission(Manifest.permission.CALL_PHONE)
                        == PackageManager.PERMISSION_GRANTED) {
                    number = PhoneNumberUtils.getNumberFromIntent(intent, this);
                    mUiCallManager.placeCall(number);
                } else {
                    L.w(TAG, "The calling app doesn't have phone call permission");
                }
                break;

            case Intent.ACTION_SEARCH:
                String searchQuery = intent.getStringExtra(SearchManager.QUERY);
                navigateToContactResultsFragment(searchQuery);
                break;
            case Intent.ACTION_VIEW:
                if (CallLog.Calls.CONTENT_TYPE.equals(intent.getType())) {
                    showTabPage(TelecomPageTab.Page.CALL_HISTORY);
                    if (checkCallingPermission(Manifest.permission.WRITE_CALL_LOG)
                            == PackageManager.PERMISSION_GRANTED) {
                        NotificationService.readAllMissedCall(this);
                    } else {
                        L.w(TAG, "The calling app doesn't have write call log permission");
                    }
                }
                break;
            default:
                // Do nothing.
        }

        setIntent(null);

        // This is to start the incall activity when user taps on the dialer launch icon rapidly
        maybeStartInCallActivity(mOngoingCallListLiveData.getValue());
    }

    /**
     * Set the tabs only, hasHfpDeviceConnectedLiveData will show the fragment later.
     */
    private void setupTabLayout() {
        OnItemClickedListener<TelecomPageTab> onTabSelected = tab -> {
            Fragment fragment = tab.getFragment();
            setContentFragment(fragment, tab.getFragmentTag());
        };

        mTabFactory = new TelecomPageTab.Factory(this, mSharedPreferences, onTabSelected,
                getSupportFragmentManager());
        List<TelecomPageTab> tabs = mTabFactory.getTabs();
        mCarUiToolbar.setTabs(tabs.stream()
                .map(TelecomPageTab::getToolbarTab)
                .collect(Collectors.toList()), mTabFactory.getSelectedTabIndex());
    }

    private void refreshUi() {
        L.i(TAG, "Refresh ui");

        mTabFactory.recreateFragments();
        int startTab = mTabFactory.getStartTabIndex();
        showTabPage(startTab);
    }

    /**
     * Switch to {@link DialpadFragment} and set the given number as dialed number.
     */
    private void showDialPadFragment(String number) {
        int dialpadTabIndex = showTabPage(TelecomPageTab.Page.DIAL_PAD);

        if (dialpadTabIndex == -1) {
            return;
        }

        TelecomPageTab dialpadTab = mTabFactory.getTabs().get(dialpadTabIndex);
        Fragment fragment = dialpadTab.getFragment();
        if (fragment instanceof DialpadFragment) {
            ((DialpadFragment) fragment).setDialedNumber(number);
        } else {
            L.w(TAG, "Current tab is not a dialpad fragment!");
        }
    }

    /**
     * Sets if the toolbar tabs should be shown or hidden. Used to hide the tabs
     * on pages like search or contact details.
     */
    public void setTabsShown(boolean shown) {
        if (mTabsShown == shown) {
            // We don't want the tabs to be redisplayed if they already are displayed,
            // as that would interrupt the ripple animation.
            return;
        }
        TabLayout tabLayout = requireViewById(R.id.car_ui_toolbar_tabs);
        int childCount = tabLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            tabLayout.getChildAt(i).setVisibility(shown ?  View.VISIBLE : View.GONE);
        }
        mTabsShown = shown;
    }

    private int showTabPage(@TelecomPageTab.Page String tabPage) {
        int tabIndex = mTabFactory.getTabIndex(tabPage);
        if (tabIndex == -1) {
            L.w(TAG, "Page %s is not a tab.", tabPage);
            return -1;
        }

        showTabPage(tabIndex);
        return tabIndex;
    }

    private void showTabPage(int tabIndex) {
        // Compare the current selection and new selection. If they are the same, selectTab(int)
        // won't trigger the listener to update content fragment.
        int selectedTab = mCarUiToolbar.getSelectedTab();
        if (selectedTab == tabIndex) {
            Fragment fragment =
                    getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            TelecomPageTab currentTab = mTabFactory.getTabs().get(selectedTab);
            if (fragment != currentTab.getFragment()) {
                setContentFragment(currentTab.getFragment(), currentTab.getFragmentTag());
            }
        }
        mCarUiToolbar.selectTab(tabIndex);
    }

    private void setContentFragment(Fragment fragment, String fragmentTag) {
        L.i(TAG, "setContentFragment: %s", fragment);

        getSupportFragmentManager().executePendingTransactions();
        while (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStackImmediate();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, fragmentTag)
                .addToBackStack(fragmentTag)
                .commit();
    }

    @Override
    public void pushContentFragment(@NonNull Fragment topContentFragment, String fragmentTag) {
        L.i(TAG, "pushContentFragment: %s", topContentFragment);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, topContentFragment, fragmentTag)
                .addToBackStack(fragmentTag)
                .commit();
    }

    @Override
    public boolean onNavigateUp() {
        if (isBackNavigationAvailable()) {
            onBackPressed();
            return true;
        }
        return super.onNavigateUp();
    }

    @Override
    public void onBackPressed() {
        // By default onBackPressed will pop all the fragments off the backstack and then finish
        // the activity. We want to finish the activity while there is still one fragment on the
        // backstack, because we use onBackStackChanged() to set up our fragments.
        if (isBackNavigationAvailable()) {
            super.onBackPressed();
        } else {
            finishAfterTransition();
        }
    }

    /**
     * Handles the click action on the menu items.
     */
    public void onMenuItemClicked(MenuItem item) {
        switch (item.getId()) {
            case R.id.menu_item_search:
                Intent searchIntent = new Intent(getApplicationContext(), TelecomActivity.class);
                searchIntent.setAction(Intent.ACTION_SEARCH);
                startActivity(searchIntent);
                break;
            case R.id.menu_item_setting:
                Intent settingsIntent = new Intent(getApplicationContext(),
                        DialerSettingsActivity.class);
                startActivity(settingsIntent);
                break;
        }
    }

    private void navigateToContactResultsFragment(String query) {
        Fragment topFragment = getSupportFragmentManager().findFragmentById(
                R.id.fragment_container);

        // Top fragment is ContactResultsFragment, update search query
        if (topFragment instanceof ContactResultsFragment) {
            ((ContactResultsFragment) topFragment).setSearchQuery(query);
            return;
        }

        ContactResultsFragment fragment = ContactResultsFragment.newInstance(query);
        pushContentFragment(fragment, ContactResultsFragment.FRAGMENT_TAG);
    }

    private void maybeStartInCallActivity(List<Call> callList) {
        if (callList == null || callList.isEmpty()) {
            return;
        }

        L.d(TAG, "Start InCallActivity");
        Intent launchIntent = new Intent(getApplicationContext(), InCallActivity.class);
        startActivity(launchIntent);
    }

    /**
     * If the back button on action bar is available to navigate up.
     */
    private boolean isBackNavigationAvailable() {
        return getSupportFragmentManager().getBackStackEntryCount() > 1;
    }

    @Override
    public void onCarUiInsetsChanged(Insets insets) {
        // Do nothing, this is just a marker that we will handle the insets in fragments.
        // This is only necessary because the fragments are not immediately added to the
        // activity when calling .commit()
    }
}
