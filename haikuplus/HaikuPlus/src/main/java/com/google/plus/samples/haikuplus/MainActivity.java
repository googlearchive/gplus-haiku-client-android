/* Copyright (C) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.plus.samples.haikuplus;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.PlusShare;
import com.google.plus.samples.haikuplus.api.HaikuClient;
import com.google.plus.samples.haikuplus.api.HaikuSession;
import com.google.plus.samples.haikuplus.api.VolleyContainer;
import com.google.plus.samples.haikuplus.models.Haiku;
import com.google.plus.samples.haikuplus.models.HaikuDeepLink;
import com.google.plus.samples.haikuplus.models.User;

import java.util.Set;

/**
 * Primary activity for the Haiku+ app. Acts as a fragment container for the stream and haiku view
 * and also handles making calls for voting and creating haikus.
 *
 * @author samstern@google.com (Sam Stern)
 * @author ianbarber@google.com (Ian Barber)
 */
public class MainActivity extends Activity implements
        HaikuInteractionListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.ServerAuthCodeCallbacks,
        View.OnClickListener, HaikuClient.HaikuServiceListener,
        GoogleApiClient.OnConnectionFailedListener, HaikuClient.HaikuRetrievedListener {

    protected static final String STREAM_FRAG_TAG = "Stream";
    protected static final String HAIKU_FRAG_TAG = "Haiku";
    protected static final int REQ_CREATE_HAIKU = 55334;

    private static final String TAG = "HaikuPlus-MainActivity";
    private static final int REQ_CHOOSE_ACCOUNT = 55333;
    private static final int REQ_SIGN_IN = 55332;
    private static final int REQ_SHARE = 55331;
    private static final String SAVED_USER = "user";
    private static final String SAVED_DEEPLINK = "deeplink";
    private GoogleApiClient mGoogleApiClient;
    private HaikuSession mHaikuPlusSession;
    private HaikuClient mHaikuApi;
    private User mUser;
    private boolean mIsResolving = false;
    private boolean mSignInClicked = false;
    private VolleyContainer mVolley;
    private HaikuDeepLink mDeepLink;
    private ProgressDialog mDialog;
    private Runnable mRunAfterSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);

        mDialog = new ProgressDialog(this);

        SignInButton signIn = (SignInButton) findViewById(R.id.button_sign_in);
        signIn.setOnClickListener(this);
        signIn.setSize(SignInButton.SIZE_WIDE);
        findViewById(R.id.button_sign_out).setOnClickListener(this);
        findViewById(R.id.button_disconnect).setOnClickListener(this);

        mHaikuPlusSession = HaikuSession.getSessionForServer(getApplicationContext());
        mHaikuApi = HaikuClient.getInstance(this, mHaikuPlusSession);

        mVolley = VolleyContainer.getInstance(this);

        // Check the constants
        if (Constants.SERVER_CLIENT_ID.equals("YOUR_WEB_CLIENT_ID") ||
                Constants.SERVER_URL.equals("YOUR_PUBLIC_SERVER_URL")) {
            throw new RuntimeException("Error: please configure SERVER_CLIENT_ID and "
                    + "SERVER_URL in Constants.java");
        }

        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .setAccountName(mHaikuPlusSession.getAccountName())
                .requestServerAuthCode(Constants.SERVER_CLIENT_ID, this);

        // Add scopes
        for (String scope : Constants.SCOPES) {
            builder.addScope(new Scope(scope));
        }

        // Add Google+ API with visible actions
        Plus.PlusOptions plusOptions = new Plus.PlusOptions.Builder()
                .addActivityTypes(Constants.ACTIONS)
                .build();
        builder.addApi(Plus.API, plusOptions);
        mGoogleApiClient = builder.build();

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new StreamFragment(), STREAM_FRAG_TAG)
                    .commit();
        }

        String deepLinkId = PlusShare.getDeepLinkId(this.getIntent());
        if (deepLinkId != null) {
            mDeepLink = HaikuDeepLink.fromString(deepLinkId);
            if (mDeepLink.getHaikuId() == null) {
                // If the ID is bad, just ignore the deeplink.
                mDeepLink = null;
                Log.d(TAG, "Got bad deeplink: " + deepLinkId);
            } else {
                showDialog(getString(R.string.loading_haiku));
                mHaikuApi.fetchHaiku(mDeepLink.getHaikuId(), this);
            }
        }

        // Hide sign in button and load user if we have cached account name and session id
        if (mHaikuPlusSession.checkSessionState() == HaikuSession.State.HAS_SESSION) {
            setProgressBarIndeterminateVisibility(true);
            findViewById(R.id.signed_out_container).setVisibility(View.GONE);
            mHaikuApi.fetchCurrentUser(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVED_USER, mUser);
        outState.putParcelable(SAVED_DEEPLINK, mDeepLink);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        onUserRetrieved((User) savedInstanceState.getParcelable(SAVED_USER));
        mDeepLink = savedInstanceState.getParcelable(SAVED_DEEPLINK);
    }

    @Override
    public CheckResult onCheckServerAuthorization(String idToken, Set<Scope> scopes) {
        Log.d(TAG, "onCheckServerAuthorization");
        mHaikuPlusSession.setIdToken(idToken);

        if (mHaikuPlusSession.checkSessionState(true) == HaikuSession.State.HAS_SESSION) {
            return CheckResult.newAuthNotRequiredResult();
        } else {
            return CheckResult.newAuthRequiredResult(scopes);
        }
    }

    @Override
    public boolean onUploadServerAuthCode(String idToken, String serverAuthCode) {
        Log.d(TAG, "onUploadServerAuthCode");

        // Async fetch current user
        mHaikuPlusSession.setCode(serverAuthCode);
        mHaikuApi.fetchCurrentUser(this);

        // Always return true, because user fetch is asynchronous
        return true;
    }

    @Override
    public void onUserRetrieved(final User user) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StreamFragment frag =
                        (StreamFragment) getFragmentManager().findFragmentByTag(STREAM_FRAG_TAG);
                if (user != null) {
                    mUser = user;
                    ((TextView) findViewById(R.id.user_name)).setText(mUser.googleDisplayName);
                    NetworkImageView profile = ((NetworkImageView) findViewById(R.id.user_profile_pic));
                    profile.setImageUrl(mUser.googlePhotoUrl, mVolley.getImageLoader());
                    findViewById(R.id.signed_in_container).setVisibility(View.VISIBLE);
                    findViewById(R.id.signed_out_container).setVisibility(View.GONE);
                    if (frag != null) {
                        frag.setUser(mUser, MainActivity.this);
                    }
                } else {
                    mUser = null;
                    findViewById(R.id.signed_in_container).setVisibility(View.GONE);
                    findViewById(R.id.signed_out_container).setVisibility(View.VISIBLE);
                    if (frag != null) {
                        frag.setUser(null, MainActivity.this);
                    }
                }
                setProgressBarIndeterminateVisibility(false);

                // Run a queued action
                // NOTE: In some situations, mRunAfterSignIn may be garbage collected while the SignIn
                // process takes place.  Therefore, it is not recommended to use this pattern for
                // critical tasks.
                if (mRunAfterSignIn != null) {
                    mRunAfterSignIn.run();
                    mRunAfterSignIn = null;
                }
            }
        });

    }

    @Override
    public void onHaikuWritten(Haiku haiku) {
        StreamFragment frag =
                (StreamFragment) getFragmentManager().findFragmentByTag(STREAM_FRAG_TAG);
        if (frag != null) {
            frag.refreshStream();
        }
        Toast toast = Toast.makeText(
                this,
                getString(R.string.haiku_written),
                Toast.LENGTH_SHORT
        );
        toast.show();
    }

    @Override
    public void onVoteWritten(Haiku haiku) {
        Toast.makeText(this, getString(R.string.haiku_voted), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSignedOut() {
        // We have signed out or disconnected, so should drop out local state.
        mHaikuPlusSession.setCode(null);
        mHaikuPlusSession.storeSessionId(null);
        mHaikuPlusSession.storeAccountName(null);
        onUserRetrieved(null);
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onHaikuSelected(Haiku haiku) {
        getFragmentManager().beginTransaction()
                .replace(R.id.container, HaikuFragment.newInstance(haiku), HAIKU_FRAG_TAG)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onPromoteCall(Haiku haiku) {
        Intent share = new PlusShare.Builder(this)
                .setType("text/plain")
                .setContentUrl(Uri.parse(haiku.contentUrl))
                .setContentDeepLinkId(haiku.contentDeepLinkId)
                .addCallToAction(
                        getString(R.string.vote_cta),
                        Uri.parse(haiku.callToActionUrl),
                        haiku.callToActionDeepLinkId
                )
                .getIntent();
        startActivityForResult(share, REQ_SHARE);
    }

    @Override
    public void onVoteCall(Haiku haiku) {
        Log.d(TAG, "Haiku vote triggered");
        signInAndVote(haiku);

        // Immediately (optimistically) update view
        HaikuFragment frag = (HaikuFragment) getFragmentManager().findFragmentByTag(HAIKU_FRAG_TAG);
        if (frag != null) {
            frag.incrementVotes();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        HaikuSession.State state = mHaikuPlusSession.checkSessionState();
        if (state == HaikuSession.State.UNAUTHENTICATED) {
            // We think we're signed in, but we don't seem to be!
            // Just bin the current session and start again
            
            // Note: in this case it is possible that we are wrong and the server already has
            // a refresh token for this user.  A more robust method here would be to ask the
            // server for account information rather than disconnecting and connecting.
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
        } else if (state == HaikuSession.State.HAS_SESSION) {
            if (mUser == null) {
                mHaikuApi.fetchCurrentUser(this);
            } else {
                onUserRetrieved(mUser);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Attempt to reconnect.
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed");

        if (mIsResolving) {
            Log.d(TAG, "Already resolving.");
            return;
        }

        // Attempt to resolve the ConnectionResult
        if (connectionResult.hasResolution() && mSignInClicked) {
            mIsResolving = true;
            mSignInClicked = false;

            try {
                connectionResult.startResolutionForResult(this, REQ_SIGN_IN);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Could not resolve.", e);
                mIsResolving = false;
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_sign_in) {
            beginSignInFlow();
        } else if (view.getId() == R.id.button_create_haiku) {
            Intent intent = new Intent(this, CreateHaikuActivity.class);
            startActivityForResult(intent, REQ_CREATE_HAIKU);
        } else if (view.getId() == R.id.button_sign_out) {
            setProgressBarIndeterminateVisibility(true);
            mHaikuApi.signOut(this);
            mGoogleApiClient.disconnect();
        } else if (view.getId() == R.id.button_disconnect) {
            setProgressBarIndeterminateVisibility(true);
            mHaikuApi.disconnect(this);

            if (mGoogleApiClient.isConnected()) {
                Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
                mGoogleApiClient.disconnect();
            }
        }
    }

    /**
     * Begin the non-immediate sign in flow
     */
    private void beginSignInFlow() {
        setProgressBarIndeterminateVisibility(true);
        HaikuSession.State state = mHaikuPlusSession.checkSessionState(true);
        mSignInClicked = true;

        if (state == HaikuSession.State.UNAUTHENTICATED) {
            Intent intent = AccountPicker.newChooseAccountIntent(
                    null, null, new String[]{"com.google"},
                    false, null, null, null, null);
            startActivityForResult(intent, REQ_CHOOSE_ACCOUNT);
        } else {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CREATE_HAIKU && resultCode == RESULT_OK) {
            // Result from creating a Haiku in another activity.
            Haiku haiku = data.getParcelableExtra(CreateHaikuActivity.EXTRA_HAIKU);
            if (haiku != null) {
                if (mUser != null) {
                    haiku.author = mUser;
                    mHaikuApi.writeHaiku(haiku, this);
                } else {
                    Toast.makeText(this, getString(R.string.haiku_not_signedin), Toast.LENGTH_SHORT)
                            .show();
                    Log.e(TAG, "Created a haiku while not bound!");
                }
            }
        } else if (requestCode == REQ_CHOOSE_ACCOUNT) {
            // Result from the account chooser,
            if (resultCode == RESULT_OK) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                mHaikuPlusSession.storeAccountName(accountName);

                mSignInClicked = true;
                mGoogleApiClient.connect();
            }
        } else if (requestCode == REQ_SHARE && resultCode == RESULT_OK) {
            // Sharing triggers its own toast so we don't need to feed back to the user.
            Log.d(TAG, "Post shared.");
        } else if (requestCode == REQ_SIGN_IN) {
            mIsResolving = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public HaikuSession getAuthenticator() {
        return mHaikuPlusSession;
    }

    @Override
    public void onHaikuRetrieved(final Haiku haiku) {
        dismissDialog();
        if (haiku == null) {
            return;
        }
        if (Constants.ACTION_VOTE.equals(mDeepLink.getAction())) {
            onVoteCall(haiku);
        }
        getFragmentManager().beginTransaction()
                .replace(R.id.container, HaikuFragment.newInstance(haiku), HAIKU_FRAG_TAG)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Vote on a Haiku, signing in first if necessary.
     *
     * @param haiku the Haiku to vote on.
     */
    private void signInAndVote(final Haiku haiku) {
        if (mUser != null) {
            mHaikuApi.writeHaikuVote(haiku, this);
        } else {
            // After vote, sign in
            mRunAfterSignIn = new Runnable() {
                @Override
                public void run() {
                    mHaikuApi.writeHaikuVote(haiku, MainActivity.this);
                }
            };

            // Sign in
            beginSignInFlow();
        }
    }

    private void showDialog(String msg) {
        mDialog.setMessage(msg);
        mDialog.show();
    }

    private void dismissDialog() {
        if (mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }
}
