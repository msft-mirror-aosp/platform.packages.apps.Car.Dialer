/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.car.dialer.framework;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;

/**
 * Activity to display error messages from the fake framework
 */
public class ErrorDialogActivity extends Activity {

    public static final String ERROR_STRING_EXTRA = "error_string_extra";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int resId = getIntent().getIntExtra(ERROR_STRING_EXTRA, -1);
        if (resId == -1) {
            finish();
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(resId)
                .setOnDismissListener(alertDialog -> finish())
                .create();
        dialog.show();
    }

    @Override
    public void finish() {
        super.finish();
        // Remove animation to avoid showing a black screen
        overridePendingTransition(0, 0);
    }
}
