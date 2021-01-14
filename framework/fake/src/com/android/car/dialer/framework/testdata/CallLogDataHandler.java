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

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.provider.CallLog;

import com.android.car.apps.common.log.L;

import java.util.ArrayList;
import java.util.List;

/**
 * A handler for adding call log data
 */
public class CallLogDataHandler {
    private static final String TAG = "CD.CallLogDataHandler";

    private static CallLogDataHandler sCallLogDataHandler;

    private Context mContext;
    private WorkerExecutor mWorkerExecutor;

    /**
     * Initialized a globally accessible {@link CallLogDataHandler} which can be retrieved by {@link
     * #get}.
     *
     * @param applicationContext Application context.
     */
    public static void init(Context applicationContext) {
        if (sCallLogDataHandler == null) {
            sCallLogDataHandler = new CallLogDataHandler(applicationContext);
        }
    }

    /**
     * Returns the single instance of {@link CallLogDataHandler}.
     */
    public static CallLogDataHandler get() {
        if (sCallLogDataHandler == null) {
            throw new IllegalStateException(
                    "Call CallLogDataHandler.init(Context) before calling this function");
        }
        return sCallLogDataHandler;
    }

    private CallLogDataHandler(Context applicationContext) {
        mContext = applicationContext;
        mWorkerExecutor = WorkerExecutor.getInstance();
    }

    /**
     * Adds call log data in a json file to call log database calllog.db in Contacts Provider.
     */
    public void addCallLogsAsync(String file) {
        // TODO: add thread here later.
        List<CallLogRawData> list = DataParser.getInstance().parseCallLogData(mContext, file);
        addCallLogsAsync(list);
    }

    /**
     * Adds a list of {@link CallLogRawData} that contains call log data to call log database
     * calllog.db in Contacts Provider.
     */
    public void addCallLogsAsync(List<CallLogRawData> list) {

        Runnable runnable = () -> {
            for (CallLogRawData rawData : list) {
                addOneCallLog(rawData);
            }
        };

        mWorkerExecutor.run(runnable);
    }

    /**
     * Adds a single call log to the database.
     */
    public void addOneCallLog(CallLogRawData callLogRawData) {

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        String id = callLogRawData.getId();
        String number = callLogRawData.getNumber();
        Integer type = callLogRawData.getNumberType();
        Integer timeInterval = callLogRawData.getInterval();

        ops.add(ContentProviderOperation.newInsert(CallLog.Calls.CONTENT_URI)
                .withValue(CallLog.Calls._ID, id)
                .withValue(CallLog.Calls.NUMBER, number)
                .withValue(CallLog.Calls.TYPE, type)
                .withValue(CallLog.Calls.DATE, System.currentTimeMillis() - timeInterval)
                .build());

        try {
            mContext.getContentResolver().applyBatch(CallLog.AUTHORITY, ops);
        } catch (RemoteException e) {
            L.e(TAG,
                    "thrown if a RemoteException is encountered while attempting to communicate "
                            + "with a remote provider.");
        } catch (OperationApplicationException e) {
            L.e(TAG,
                    "thrown if a OperationApplicationException is encountered when an operation "
                            + "fails.");
        }
    }

    /**
     * Tears down the instance.
     */
    public void tearDown() {
        removeAddedCalllogsAsync();
        sCallLogDataHandler = null;
    }

    /**
     * Removes call logs that are added after this instance is created.
     */
    public void removeAddedCalllogsAsync() {
        // to be implemented.
    }

    /**
     * Remove every data piece in the call log database.
     */
    public void removeAllCalllogs() {
        // to be implemented.
    }

    /**
     * Removes the call log
     */
    public void removeCalllog(String id) {

        // TODO: to be updated.
        Runnable runnable = () -> {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            ops.add(ContentProviderOperation.newDelete(CallLog.Calls.CONTENT_URI)
                    .withSelection(CallLog.Calls._ID + "=?", new String[]{id})
                    .build());

            try {
                mContext.getContentResolver().applyBatch(CallLog.AUTHORITY, ops);
            } catch (RemoteException e) {
                L.e(TAG,
                        "thrown if a RemoteException is encountered while attempting to "
                                + "communicate with a remote provider.");
            } catch (OperationApplicationException e) {
                L.e(TAG,
                        "thrown if a OperationApplicationException is encountered when a delete "
                                + "operation fails.");
            }
        };

        mWorkerExecutor.run(runnable);
    }
}
