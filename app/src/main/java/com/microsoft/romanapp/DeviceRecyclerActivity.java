//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.romanapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.microsoft.connecteddevices.ConnectedDevicesException;
import com.microsoft.connecteddevices.IRemoteSystemDiscoveryListener;
import com.microsoft.connecteddevices.Platform;
import com.microsoft.connecteddevices.RemoteSystem;
import com.microsoft.connecteddevices.RemoteSystemDiscovery;
import com.microsoft.connecteddevices.RemoteSystemDiscoveryType;
import com.microsoft.connecteddevices.RemoteSystemDiscoveryTypeFilter;
import com.microsoft.connecteddevices.RemoteSystemKind;
import com.microsoft.connecteddevices.RemoteSystemKindFilter;
import com.microsoft.connecteddevices.RemoteSystemStatusType;

public class DeviceRecyclerActivity extends AppCompatActivity {
    private static final String TAG = DeviceRecyclerActivity.class.getName();

    private DeviceRecyclerAdapter mDeviceRecyclerAdapter;
    private List<Device> mDevices;
    private RecyclerView mRecyclerView;
    private RemoteSystemDiscovery mDiscovery = null;
    private RemoteSystemDiscovery.Builder mDiscoveryBuilder;

    public static final String DEVICE_KEY = "device_key";

    // Device types filter parameters:
    // Discover all types of connections
    private DiscoveryType mDiscoveryType = DiscoveryType.ALL;
    // Discover all types of systems (Holographic, Desktops, Phones and etc)
    private SystemKind mSystemKind = SystemKind.ALL;
    // for multiple specific types/kinds use List

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.device_recycler);

        createFilterSpinners();

        mRecyclerView = (RecyclerView) findViewById(R.id.device_recycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);

        initializeData();
    }

    private void createFilterSpinners() {
        // Create Discovery Type filter spinner
        Spinner discoveryTypeSpinner = (Spinner) findViewById(R.id.discovery_type_filter_spinner);
        ArrayAdapter<CharSequence> discoveryTypeAdapter = ArrayAdapter.createFromResource(this, R.array.discovery_type_filter_array, android.R.layout.simple_spinner_item);
        discoveryTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        discoveryTypeSpinner.setAdapter(discoveryTypeAdapter);
        discoveryTypeSpinner.setOnItemSelectedListener(new DiscoveryTypeFilter());
        discoveryTypeSpinner.setSelection(0);

        // Create System Kind filter spinner
        Spinner systemKindSpinner = (Spinner) findViewById(R.id.system_kind_filter_spinner);
        ArrayAdapter<CharSequence> systemKindAdapter = ArrayAdapter.createFromResource(this, R.array.system_kind_filter_array, android.R.layout.simple_spinner_item);
        systemKindAdapter .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        systemKindSpinner.setAdapter(systemKindAdapter );
        systemKindSpinner.setOnItemSelectedListener(new SystemKindFilter());
        systemKindSpinner.setSelection(0);
    }

    private enum DiscoveryType {
        ALL("All"),
        CLOUD("Cloud"),
        PROXIMAL("Proximal");

        private final String value;

        DiscoveryType(String value) { this.value = value; }

        static DiscoveryType fromString(String value) {
            switch (value) {
                case "All"         : return ALL;
                case "Cloud"       : return CLOUD;
                case "Proximal"    : return PROXIMAL;
            }
            return ALL;
        }

        public String getValue() {
            return value;
        }
    }

    private enum SystemKind {
        ALL("All"),
        UNKNOWN("Unknown"),
        DESKTOP("Desktop"),
        HOLOGRAPHIC("Holographic"),
        PHONE("Phone"),
        XBOX("Xbox");

        private final String value;

        SystemKind(String value) { this.value = value; }

        static SystemKind fromString(String value) {
            switch (value) {
                case "All"         : return ALL;
                case "Desktop"     : return DESKTOP;
                case "Holographic" : return HOLOGRAPHIC;
                case "Phone"       : return PHONE;
                case "Xbox"        : return XBOX;
            }
            return UNKNOWN;
        }

        public String getValue() {
            return value;
        }
    }

    private enum StatusType{
        ANY("Any"),
        AVAILABLE("Available");

        private final String value;

        StatusType(String value){
            this.value=value;
        }

        public String getValue(){
            return value;
        }
    }

    private class DiscoveryTypeFilter implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            String filterType = (String) parent.getItemAtPosition(pos);
            mDiscoveryType = DiscoveryType.fromString(filterType);
            initializeData();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    private class SystemKindFilter implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            String filterType = (String) parent.getItemAtPosition(pos);
            mSystemKind = SystemKind.fromString(filterType);
            initializeData();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
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

    public void onDiscoverClicked(View view) {
        initializeData();
    }

    private void initializeData(){
        initializeAdapter();
        mDiscoveryBuilder = new RemoteSystemDiscovery.Builder().setListener(new IRemoteSystemDiscoveryListener() {
            @Override
            public void onRemoteSystemAdded(RemoteSystem remoteSystem) {
                Log.d(TAG, "RemoteSystemAdded = " + remoteSystem.getDisplayName());

                mDevices.add(new Device(remoteSystem));
                // Sort devices in alphabetic order
                Collections.sort(mDevices, new Comparator<Device>() {
                    @Override
                    public int compare(Device d1, Device d2)
                    {
                        return d1.getName().compareTo(d2.getName());
                    }
                });

                // Update the RecyclerView
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDeviceRecyclerAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onRemoteSystemUpdated(RemoteSystem remoteSystem) {
                Log.d(TAG, "RemoteSystemUpdated = " + remoteSystem.getDisplayName());
            }

            @Override
            public void onRemoteSystemRemoved(String remoteSystemId) {
                Log.d(TAG, "RemoteSystemRemoved = id:" + remoteSystemId);
            }
        })
                .filter(generateDiscoveryTypeFilter())  // add Discovery type filter
                .filter(generateSystemKindFilter());    // add device kind filter

        startDiscovery();
    }

    private RemoteSystemDiscoveryTypeFilter generateDiscoveryTypeFilter() {
        RemoteSystemDiscoveryType kind = RemoteSystemDiscoveryType.ANY;

        switch (mDiscoveryType) {
            case CLOUD:
                kind = RemoteSystemDiscoveryType.CLOUD;
                break;
            case PROXIMAL:
                kind = RemoteSystemDiscoveryType.PROXIMAL;
                break;
            case ALL:
                kind = RemoteSystemDiscoveryType.ANY;
                break;
        }
        return new RemoteSystemDiscoveryTypeFilter(kind);
    }

    private RemoteSystemKindFilter generateSystemKindFilter() {
        ArrayList<RemoteSystemKind> kinds = new ArrayList<>(Arrays.asList(RemoteSystemKind.UNKNOWN));

        switch (mSystemKind) {
            case ALL:
                kinds = new ArrayList<>(Arrays.asList(RemoteSystemKind.UNKNOWN,
                        RemoteSystemKind.DESKTOP,
                        RemoteSystemKind.HOLOGRAPHIC,
                        RemoteSystemKind.PHONE,
                        RemoteSystemKind.XBOX));
                break;
            case UNKNOWN:
                kinds = new ArrayList<>(Arrays.asList(RemoteSystemKind.UNKNOWN));
                break;
            case DESKTOP:
                kinds = new ArrayList<>(Arrays.asList(RemoteSystemKind.DESKTOP));
                break;
            case HOLOGRAPHIC:
                kinds = new ArrayList<>(Arrays.asList(RemoteSystemKind.HOLOGRAPHIC));
                break;
            case PHONE:
                kinds = new ArrayList<>(Arrays.asList(RemoteSystemKind.PHONE));
                break;
            case XBOX:
                kinds = new ArrayList<>(Arrays.asList(RemoteSystemKind.XBOX));
                break;
        }

        return new RemoteSystemKindFilter(kinds);
    }

    private void startDiscovery() {
        Log.v(TAG, "Starting Discovery");
        if (mDiscovery != null) {
            // before starting new discovery, stop ongoing one
            try {
                mDiscovery.stop();
            } catch (ConnectedDevicesException e) {
                e.printStackTrace();
            }
        }
        mDiscovery = createDiscovery();
        // begin watching for remote devices
        try {
            Log.d(TAG, "Starting device discovery");
//            mDiscovery.findByHostName("Fukken-Uber-PC");
            mDiscovery.start();
        } catch (ConnectedDevicesException e) {
            Log.d(TAG, "Could not start device discovery: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private RemoteSystemDiscovery createDiscovery() {
        return mDiscoveryBuilder.getResult();
    }

    private void initializeAdapter(){
        mDevices = new ArrayList<>();
        mDeviceRecyclerAdapter = new DeviceRecyclerAdapter(mDevices);
        mRecyclerView.setAdapter(mDeviceRecyclerAdapter);
        mRecyclerView.invalidate();

        mDeviceRecyclerAdapter.setOnItemClickListener(new DeviceRecyclerAdapter.ClickListener() {
            @Override
            public void onItemClick(int position, View v) {
                // our parcelable wrapper for RemoteDevice
                Device selectedDevice = mDevices.get(position);
                Intent intent = new Intent(v.getContext(), DeviceActivity.class);
                intent.putExtra(DEVICE_KEY, selectedDevice);
                startActivity(intent);
            }
        });
    }
}
