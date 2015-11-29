package no.studiothorsen.fitdataextractor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import no.studiothorsen.dataextractor.DataExtractor;

public class MainActivity extends Activity {
    private static final String TAG = "FitDataExtractor";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DataExtractor.getInstance().printFitData(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult(" + requestCode + ")");
        if (requestCode == 1) {
            DataExtractor.getInstance().onOauth(resultCode == RESULT_OK);
        }
    }
}
