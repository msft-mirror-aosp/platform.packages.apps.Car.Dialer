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

package com.android.car.dialer.livedata;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;

import com.android.car.dialer.log.L;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Provides SharedPreferences.
 */
@AutoFactory
public class SharedPreferencesLiveData extends LiveData<SharedPreferences> {
    private static final String TAG = "CD.PreferenceLiveData";

    private final SharedPreferences mSharedPreferences;
    private final String mKey;

    private final SharedPreferences.OnSharedPreferenceChangeListener
            mOnSharedPreferenceChangeListener;

    SharedPreferencesLiveData(
            @Provided SharedPreferences sharedPreferences,
            String key) {
        mSharedPreferences = sharedPreferences;
        mKey = key;

        mOnSharedPreferenceChangeListener = (preferences, k) -> {
            if (TextUtils.equals(k, mKey)) {
                updateSharedPreferences();
            }
        };
    }

    SharedPreferencesLiveData(
            @Provided @ApplicationContext Context context,
            @Provided SharedPreferences sharedPreferences,
            @StringRes int key) {
        this(sharedPreferences, context.getString(key));
    }

    @Override
    protected void onActive() {
        updateSharedPreferences();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(
                mOnSharedPreferenceChangeListener);
    }

    @Override
    protected void onInactive() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(
                mOnSharedPreferenceChangeListener);
    }

    private void updateSharedPreferences() {
        L.i(TAG, "Update SharedPreferences");
        setValue(mSharedPreferences);
    }

    /**
     * Returns the monitored shared preference key.
     */
    public String getKey() {
        return mKey;
    }
}