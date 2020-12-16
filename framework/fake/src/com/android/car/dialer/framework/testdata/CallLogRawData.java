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

package com.android.car.dialer.framework.testdata;

/**
 * A class represents the call log raw data.
 */
public class CallLogRawData {

    private String mId;
    private String mNumber;
    private Integer mNumberType;
    /**
     * This is the time interval from when the call occurs to the current time.
     */
    private Integer mTimeStamp;

    /**
     * The constructor for {@link CallLogRawData}.
     */
    public CallLogRawData(String id) {
        mId = id;
    }

    /**
     * Sets the number;
     */
    public void setNumber(String number) {
        mNumber = number;
    }

    /**
     * Sets the number type;
     */
    public void setNumberType(Integer numberType) {
        mNumberType = numberType;
    }

    /**
     * Sets the time stamp;
     */
    public void setTimeStamp(Integer timeStamp) {
        mTimeStamp = timeStamp;
    }

    /**
     * Gets the id;
     */
    public String getId() {
        return mId;
    }

    /**
     * Gets the phone number;
     */
    public String getNumber() {
        return mNumber;
    }

    /**
     * Gets the phone number type;
     */
    public Integer getNumberType() {
        return mNumberType;
    }

    /**
     * Gets the time stamp for the phone call;
     */
    public Integer getTimeStamp() {
        return mTimeStamp;
    }
}
