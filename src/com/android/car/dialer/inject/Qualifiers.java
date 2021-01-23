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

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/** Dialer annotations for different dependencies. */
public final class Qualifiers {
    /** Annotation for hfp connection state dependency. */
    @Qualifier
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    public @interface Hfp {}

    /** Annotation for bluetooth state dependency. */
    @Qualifier
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
    public @interface Bluetooth {}

    private Qualifiers() {}
}
