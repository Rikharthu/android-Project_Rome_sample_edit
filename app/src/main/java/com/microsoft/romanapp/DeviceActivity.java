//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.romanapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.microsoft.connecteddevices.ConnectedDevicesException;
import com.microsoft.connecteddevices.IRemoteLauncherListener;
import com.microsoft.connecteddevices.Platform;
import com.microsoft.connecteddevices.RemoteLaunchUriStatus;
import com.microsoft.connecteddevices.RemoteLauncher;
import com.microsoft.connecteddevices.RemoteSystemConnectionRequest;

import static com.microsoft.romanapp.DeviceRecyclerActivity.DEVICE_KEY;
import static com.microsoft.connecteddevices.RemoteLaunchUriStatus.SUCCESS;

public class DeviceActivity extends Activity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = DeviceActivity.class.getName();
    private Device device;
    private TextView mLaunchLogTv;
    private EditText mLaunchUriEt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_view);

        mLaunchLogTv = (TextView) findViewById(R.id.launch_log);
        mLaunchUriEt = (EditText) findViewById(R.id.launch_uri_edit_text);

        Intent intent = this.getIntent();
        device = intent.getParcelableExtra(DEVICE_KEY);
        if (device == null) {
            Log.e(TAG, "Could not find \"device\" in bundle");
            finish();
            return;
        }

        TextView deviceName = (TextView) findViewById(R.id.device_name);
        deviceName.setText(device.getName());
        TextView deviceType = (TextView) findViewById(R.id.device_type);
        deviceType.setText(device.getType());

        Spinner uriSpinner = (Spinner) findViewById(R.id.launch_uri_spinner);
        // initialize default links
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.uri_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        uriSpinner.setAdapter(adapter);
        uriSpinner.setOnItemSelectedListener(this);
        uriSpinner.setSelection(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        Platform.resume();
    }

    @Override
    public void onPause() {
        Platform.suspend();
        super.onPause();
    }

    public void onLaunchClick(View view) {
        if (device.getSystem() != null) {
            // a RemoteSystemConnectionRequest represents the connection to a single device (RemoteSystem object)
            // the RemoteSystem object 'remoteSystem' has previously been acquired
            launchUri(new RemoteSystemConnectionRequest(device.getSystem()));
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        String url = (String) parent.getItemAtPosition(pos);
        mLaunchUriEt.setText(url);
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    private void launchUri(RemoteSystemConnectionRequest connectionRequest) {
        try {
            // the URI to launch
            String url = mLaunchUriEt.getText().toString();
            logLaunchMessage("Launching URI: " + url + " on " + connectionRequest.getRemoteSystem().getDisplayName());
            // get a RemoteLauncher and use it to launch the URI over this connection
            new RemoteLauncher().LaunchUriAsync(connectionRequest, url,
                    new IRemoteLauncherListener() {
                        @Override
                        public void onCompleted(RemoteLaunchUriStatus status) {
                            String message;
                            if (status == SUCCESS)
                            {
                                message = "Launch succeeded";
                                Log.i(TAG, message);
                            }
                            else
                            {
                                message = "Launch failed with status " + status.toString();
                                Log.e(TAG, message);
                            }
                            logLaunchMessage(message);
                        }
                    });
        } catch (ConnectedDevicesException e) {
            e.printStackTrace();
        }
    }

    private void logLaunchMessage(final String message) {
        DeviceActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mLaunchLogTv == null) {
                    return;
                }
                CharSequence prevText = mLaunchLogTv.getText();
                String newText = prevText + "\n" + message;
                mLaunchLogTv.setText(newText);
            }
        });
    }
}
