package no.studiothorsen.dataextractor;


import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class DataExtractor {
    private static final String TAG = "FitDataExtractor";

    private GoogleApiClient mClient;

    private static DataExtractor sInstance = null;

    private DataExtractor() {
    }

    public static DataExtractor getInstance() {
        if (sInstance == null) {
            sInstance = new DataExtractor();
        }
        return sInstance;
    }

    public void onOauth(boolean success) {
        if (success) {
            Log.i(TAG, "connecting again...");
            mClient.connect();
        } else {
            Log.i(TAG, "ERROR");
        }

    }

    public void printFitData(final Activity activity) {
        mClient = new GoogleApiClient.Builder(activity)
                .addApi(Fitness.HISTORY_API)
                .useDefaultAccount()
                .addScope(Fitness.SCOPE_ACTIVITY_READ)
                .addScope(Fitness.SCOPE_BODY_READ)
                .addScope(Fitness.SCOPE_LOCATION_READ)
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {
                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!!!");
                                printFitDataHelper();
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        }
                )
                .addOnConnectionFailedListener(
                        new GoogleApiClient.OnConnectionFailedListener() {
                            // Called whenever the API client fails to connect.
                            @Override
                            public void onConnectionFailed(ConnectionResult result) {
                                Log.i(TAG, "Connection failed. Cause: " + result.toString());
                                if (!result.hasResolution()) {
                                    // Show the localized error dialog
                                    GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),
                                            activity, 0).show();
                                    return;
                                }

                                try {
                                    Log.i(TAG, "Attempting to resolve failed connection");
                                    result.startResolutionForResult(activity, 1);
                                } catch (IntentSender.SendIntentException e) {
                                    Log.e(TAG, "Exception while starting resolution activity", e);
                                }
                            }
                        }
                )
                .build();

        mClient.connect();
    }

    private void printFitDataHelper() {
        Log.i(TAG, "printFitData()");
        Log.i(TAG, "  client: " + mClient);
        Log.i(TAG, "  history api connected: " + mClient.hasConnectedApi(Fitness.HISTORY_API));
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        long startTime = cal.getTimeInMillis();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_HEART_RATE_BPM, DataType.AGGREGATE_HEART_RATE_SUMMARY)
                .aggregate(DataType.TYPE_LOCATION_SAMPLE, DataType.AGGREGATE_LOCATION_BOUNDING_BOX)
                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .bucketByTime(1, TimeUnit.MINUTES)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        Log.i(TAG, "read request: " + readRequest);

        PendingResult<DataReadResult> pendingResult = Fitness.HistoryApi.readData(mClient, readRequest);

        pendingResult.setResultCallback(new ResultCallback<DataReadResult>() {
            @Override
            public void onResult(DataReadResult result) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                Log.i(TAG, "<data>");
                for (Bucket bucket : result.getBuckets()) {
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {
                        for (DataPoint dp : dataSet.getDataPoints()) {
                            Log.i(TAG, "<datapoint name=\"" + dp.getDataType().getName() + "\" " +
                                    "start=\"" + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + "\" " +
                                    "end=\"" + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + "\">");

                            for (Field field : dp.getDataType().getFields()) {
                                Log.i(TAG, "\t<field name=\"" + field.getName() + "\" " +
                                        "value=\"" + dp.getValue(field) + "\"/>");
                            }

                            Log.i(TAG, "</datapoint>");
                        }
                    }
                }
                Log.i(TAG, "</data>");
            }
        });

        Log.i(TAG, "pendingresult is canceled: " + pendingResult.isCanceled());
    }
}
