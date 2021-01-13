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

package com.android.car.dialer.framework.testdata;

import android.content.Context;

import com.android.car.apps.common.log.L;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

/**
 * A data parser that reads Json arrays from a json file and returns a list of {@link
 * ContactRawData} or {@link CallLogRawData}.
 */
public class DataParser {
    private static final String TAG = "CD.DataParser";

    private static DataParser sDataParser;

    /** Returns the singleton DataParser for the application. */
    public static DataParser getInstance() {
        if (sDataParser == null) {
            sDataParser = new DataParser();
        }
        return sDataParser;
    }

    /**
     * Parse the contact raw data from the json file. The json file should be one that is stored in
     * the assets folder and consists of keys that are the same as the fields in {@link
     * ContactRawData}.
     */
    public List<ContactRawData> parseContactData(Context context, String file) {
        List<ContactRawData> rawDatas = getRawDataList(context, file,
                new TypeToken<List<ContactRawData>>() {}.getType());
        return rawDatas;
    }

    /**
     * Parse the call log raw data from the json file. The json file should be one that is stored
     * in the assets folder and consists of keys that are the same as the fields in
     * {@link CallLogRawData}.
     */
    public List<CallLogRawData> parseCallLogData(Context context, String file) {
        List<CallLogRawData> rawDatas = getRawDataList(context, file,
                new TypeToken<List<CallLogRawData>>() {}.getType());

        return rawDatas;
    }

    /** Tears down the singleton DataParser for the application */
    public void tearDown() {
        sDataParser = null;
    }

    private <T extends RawData> List<T> getRawDataList(Context context, String file, Type type) {
        List<T> rawDataList = null;
        try {
            InputStream inputStream = context.getAssets().open(file);
            JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream));
            Gson gson = new Gson();
            rawDataList = gson.fromJson(jsonReader, type);

            jsonReader.close();
            inputStream.close();
        } catch (IOException e) {
            L.e(TAG, "Exception caught when open a file" + e);
        }
        return rawDataList;
    }
}
