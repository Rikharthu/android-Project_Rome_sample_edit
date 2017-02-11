//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.romanapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.connecteddevices.IAuthCodeProvider;
import com.microsoft.connecteddevices.IPlatformInitializationHandler;
import com.microsoft.connecteddevices.Platform;

public class MainActivity extends FragmentActivity {
    private static final String LOG_TAG = MainActivity.class.getName();

    // This is the client ID you were assigned when your app was registered with MSA.
    public static final String CLIENT_ID = "8f2b3218-e741-4f25-b2d3-74a1f02139cc";
    // При успешной авторизации мы будем перенаправлены по этой ссылке, вместе с OAuth ключом
    private static final String REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";

    private TextView mStatusOutput;
    private Button mSignInButton;
    private String mOauthUrl;
    WebView mWebView;
    Dialog mAuthDialog;
    private Platform.IAuthCodeHandler mAuthCodeHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStatusOutput = (TextView) findViewById(R.id.status_output);
        mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mAuthDialog = new Dialog(this);
        mAuthDialog.setContentView(R.layout.auth_dialog);
        mWebView = (WebView) mAuthDialog.findViewById(R.id.webv);
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);

        // Initialize Microsoft Connected Devices Platform
        appendStatus("Initializing Platform");
        appendStatus("Platform will attempt to use previously saved refresh token");
        Platform.initialize(getApplicationContext(), new IAuthCodeProvider() {
                    @Override
                    /**
                     * ConnectedDevices Platform needs the app to fetch a MSA auth_code using the given oauthUrl.
                     * When app has fetched the auth_code, it needs to invoke the authCodeHandler onAuthCodeFetched method.
                     * This will be called the first time the app is run and upon the expiration of refresh token
                     */
                    public void fetchAuthCodeAsync(String oauthUrl, Platform.IAuthCodeHandler authCodeHandler) {
                        Log.d(LOG_TAG, "fetchAuthCodeAsync() at URL: " + oauthUrl);
                        mOauthUrl = oauthUrl;
                        mAuthCodeHandler = authCodeHandler;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                appendStatus("Platform needs an MSA Auth code");
                                // enable login button
                                mSignInButton.setVisibility(View.VISIBLE);
                                mSignInButton.setEnabled(true);
                            }
                        });
                    }

                    @Override
                    /**
                     * ConnectedDevices Platform needs your app's registered client ID.
                     */
                    public String getClientId() {
                        Log.d(LOG_TAG, "getClientId(), returned: " + CLIENT_ID);
                        return CLIENT_ID;
                    }
                },
                new IPlatformInitializationHandler() {
                    @Override
                    public void onDone(boolean succeeded) {
                        if (succeeded) {
                            Log.i(LOG_TAG, "Initialized platform successfully");
                            appendStatus("Platform initialization complete");
                            // start device discovery in another activity
                            Intent intent = new Intent(MainActivity.this, DeviceRecyclerActivity.class);
                            startActivity(intent);
                        } else {
                            Log.e(LOG_TAG, "Error initializing platform");
                            appendStatus("Platform initialization failed");
                        }
                    }
                });
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

    public void onLoginClick(View view) {
        mSignInButton.setEnabled(false);
        // load the given URL for auth code fetching
        mWebView.loadUrl(mOauthUrl);
        // define a WebViewClient to interact with this URL
        WebViewClient webViewClient = new WebViewClient() {
            boolean authComplete = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(LOG_TAG, "onPageFinished(): " + url);

                // check URL for authorization success:
                if (url.startsWith(REDIRECT_URI)) {
                    Log.d(LOG_TAG, "redirect url");

                    // extract the auth code from the url
                    Uri uri = Uri.parse(url);
                    String code = uri.getQueryParameter("code");
                    String error = uri.getQueryParameter("error");
                    if (code != null && !authComplete) {
                        authComplete = true;
                        mAuthDialog.dismiss();
                        Log.i(LOG_TAG, "OAuth sign-in finished successfully, code: "+code);

                        // finally, pass auth code into the onAuthCodeFetched method,
                        // so the platform initialization can continue
                        if (mAuthCodeHandler != null) {
                            mAuthCodeHandler.onAuthCodeFetched(code);
                            // next step is IPlatformInitializationHandler's onDone() callback in platform initializatino
                        }
                    } else if (error != null) {
                        // we had an error
                        authComplete = true;
                        Log.e(LOG_TAG, "OAuth sign-in failed with error: " + error);
                        Intent resultIntent = new Intent();
                        setResult(Activity.RESULT_CANCELED, resultIntent);
                        Toast.makeText(getApplicationContext(), "Error Occurred: " + error, Toast.LENGTH_SHORT).show();

                        mAuthDialog.dismiss();
                    }
                }
            }
        };

        mWebView.setWebViewClient(webViewClient);
        mAuthDialog.show();
        mAuthDialog.setCancelable(true);
    }

    private void appendStatus(final String status) {
        if (mStatusOutput == null) {
            Log.e(LOG_TAG, "StatusOutput field is null");
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String prevStatus = mStatusOutput.getText().toString();
                String newStatus = prevStatus + "\n" + status;

                mStatusOutput.setText(newStatus);
            }
        });
    }
}
