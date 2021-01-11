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

package com.android.car.dialer;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**Dialer modules.*/
public final class DialerModules {

    /** Application level module. */
    @InstallIn(SingletonComponent.class)
    @Module
    public static final class BaseModule {

        @Singleton
        @Provides
        static SharedPreferences provideSharedPreferences(@ApplicationContext Context context) {
            return PreferenceManager.getDefaultSharedPreferences(context);
        }
    }

    /** Do not initialize. */
    private DialerModules() {
    }
}
