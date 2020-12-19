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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dagger.hilt.internal.GeneratedComponentManager;

/** Util class to retrieve dagger components from context. */
public final class ComponentFetcher {

    /** Fetches the component {@code T} from context. */
    public static <T> T from(Context context, Class<T> componentClass) {
        T component = componentClass.cast(from(context));
        if (component == null) {
            throw new IllegalArgumentException(
                    "Given context doesn't have the component installed: "
                            + componentClass.getName());
        }
        return component;
    }

    @Nullable
    private static <T> T from(@NonNull Context context) {
        if (context instanceof GeneratedComponentManager<?>) {
            return (T) ((GeneratedComponentManager<?>) context).generatedComponent();
        }
        return null;
    }

    private ComponentFetcher() {
    }
}
