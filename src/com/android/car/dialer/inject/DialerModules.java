/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.car.Car;
import android.car.content.pm.CarPackageManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.telecom.Call;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.android.car.dialer.framework.ConnectToBluetoothButtonDecorator;
import com.android.car.dialer.framework.UxrButtonDecorator;
import com.android.car.dialer.telecom.InCallServiceImpl;
import com.android.car.dialer.ui.common.DialerBaseFragment;
import com.android.car.dialer.ui.common.OnItemClickedListener;
import com.android.car.dialer.ui.contact.ContactDetailsFragment;
import com.android.car.telephony.calling.InCallServiceManager;
import com.android.car.telephony.common.Contact;
import com.android.car.ui.utils.CarUxRestrictionsUtil;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.components.FragmentComponent;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Dialer modules.
 */
public final class DialerModules {

    /**
     * Application level module.
     */
    @InstallIn(SingletonComponent.class)
    @Module
    public static final class AppModule {

        @Singleton
        @Provides
        static BluetoothAdapter provideBluetoothAdapter(@ApplicationContext Context context) {
            return context.getSystemService(BluetoothManager.class).getAdapter();
        }

        @Singleton
        @Provides
        static SharedPreferences provideSharedPreferences(@ApplicationContext Context context) {
            return PreferenceManager.getDefaultSharedPreferences(context);
        }

        @Provides
        static ExecutorService provideSingleThreadExecutor() {
            return Executors.newSingleThreadExecutor();
        }

        /**
         * Singleton executor service to sort contacts to make sure only one thread sorts the
         * contact list at one time.
         */
        @Singleton
        @Provides
        @Named("ContactSort")
        static ExecutorService provideContactSortExecutor() {
            return Executors.newSingleThreadExecutor();
        }

        @Singleton
        @Provides
        static Car provideCar(@ApplicationContext Context context) {
            return Car.createCar(context);
        }

        @Singleton
        @Provides
        static CarUxRestrictionsUtil provideCarUxRestrictionsUtil(
                @ApplicationContext Context context) {
            return CarUxRestrictionsUtil.getInstance(context);
        }

        @Singleton
        @Provides
        static InCallServiceManager provideInCallServiceManager() {
            return new InCallServiceManager();
        }

        @Provides
        static List<Call> provideCallList(InCallServiceManager inCallServiceManager) {
            InCallServiceImpl inCallService =
                    (InCallServiceImpl) inCallServiceManager.getInCallService();
            if (inCallService != null) {
                return inCallService.getCallList();
            }
            return Collections.emptyList();
        }
    }

    /**
     * Module providing dependencies for activities.
     */
    @InstallIn(ActivityComponent.class)
    @Module
    public abstract static class ActivityModule {
        @Provides
        static CarPackageManager provideCarPackageManager(Car car) {
            return (CarPackageManager) car.getCarManager(Car.PACKAGE_SERVICE);
        }

        @Binds
        @Named("ConnectToBluetooth")
        abstract UxrButtonDecorator bindConnectToBluetoothButtonDecorator(
                ConnectToBluetoothButtonDecorator decorator);

    }

    /**
     * Module providing dependencies for fragments.
     */
    @InstallIn(FragmentComponent.class)
    @Module
    public static final class FragmentModule {
        @Provides
        static OnItemClickedListener<Contact> provideShowContactDetailListener(Fragment fragment) {
            return contact -> {
                Fragment contactDetailsFragment = ContactDetailsFragment.newInstance(contact);
                if (fragment instanceof DialerBaseFragment) {
                    ((DialerBaseFragment) fragment).pushContentFragment(contactDetailsFragment,
                            ContactDetailsFragment.FRAGMENT_TAG);
                }
            };
        }
    }

    /**
     * Do not initialize.
     */
    private DialerModules() {
    }
}
