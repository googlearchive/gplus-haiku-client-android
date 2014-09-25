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

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
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

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;

/**
 * Primary activity for the Haiku+ app. Acts as a fragment container for the stream and haiku view
 * and also handles making calls for voting and creating haikus.
 *
 * @author samstern@google.com (Sam Stern)
 * @author ianbarber@google.com (Ian Barber)
 */
public class MainActivity extends Activity implements
        HaikuInteractionListener, GoogleApiClient.ConnectionCallbacks,
        View.OnClickListener, HaikuClient.HaikuServiceListener,
        GoogleApiClient.OnConnectionFailedListener, HaikuClient.HaikuRetrievedListener {

    protected static final String STREAM_FRAG_TAG = "Stream";
    protected static final String HAIKU_FRAG_TAG = "Haiku";
    protected static final int REQ_CREATE_HAIKU = 55334;

    private static final String TAG = "HaikuPlus-MainActivity";
    private static final int REQ_CHOOSE_ACCOUNT = 55333;
    private static final int REQ_CONSENT = 55332;
    private static final int REQ_SHARE = 55331;
    private static final String SAVED_USER = "user";
    private static final String SAVED_DEEPLINK = "deeplink";
    private GoogleApiClient mGoogleApiClient;
    private HaikuSession mHaikuPlusSession;
    private HaikuClient mHaikuApi;
    private User mUser;
    private Boolean mShouldResolve = false;
    private VolleyContainer mVolley;
    private HaikuDeepLink mDeepLink;
    private ProgressDialog mDialog;
    private Runnable mRunAfterSignIn;
    private AlertDialog mSignInDialog;

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

        mHaikuPlusSession = HaikuSession.getSessionForServer(
                getApplicationContext(), Constants.SERVER_CLIENT_ID);
        mHaikuApi = HaikuClient.getInstance(this, mHaikuPlusSession);

        mVolley = VolleyContainer.getInstance(this);

        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .setAccountName(mHaikuPlusSession.getAccountName());
        for (String scope : Constants.SCOPES) {
            builder.addScope(new Scope(scope));
        }
        Plus.PlusOptions plusOptions = new Plus.PlusOptions.Builder()
                .addActivityTypes(Constants.ACTIONS)
                .build();
        builder.addApi(Plus.API, plusOptions);
        mGoogleApiClient = builder.build();

        String deepLinkId = PlusShare.getDeepLinkId(this.getIntent());

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new StreamFragment(), STREAM_FRAG_TAG)
                    .commit();
        }

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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVED_USER, mUser);
        outState.putParcelable(SAVED_DEEPLINK, mDeepLink);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        onUserRetrieved((User) savedInstanceState.getParcelable(SAVED_USER));
        mDeepLink = (HaikuDeepLink) savedInstanceState.getParcelable(SAVED_DEEPLINK);
    }

    @Override
    public void onUserRetrieved(User user) {
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
                frag.setUser(mUser, this);
            }
        } else {
            mUser = null;
            findViewById(R.id.signed_in_container).setVisibility(View.GONE);
            findViewById(R.id.signed_out_container).setVisibility(View.VISIBLE);
            if (frag != null) {
                frag.setUser(null, this);
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
    public void codeSignInRequired() {
        setProgressBarIndeterminateVisibility(false);
        // For now, we're just immediately popping a new consent.
        Log.e(TAG, "We need a new code");
        mHaikuPlusSession.storeSessionId(null);
        mUser = null;
        if (mShouldResolve) {
            new CheckOrRetrieveCodeTask().execute();
        } else if (mSignInDialog == null || !mSignInDialog.isShowing()) {
            mSignInDialog = new AlertDialog.Builder(this)
                    .setNegativeButton(getString(R.string.signin_dialog_no),
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Revert to signed out view
                            onSignedOut();
                        }
                    })
                    .setPositiveButton(getString(R.string.signin_dialog_yes),
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Begin sign in again
                            new CheckOrRetrieveCodeTask().execute();
                        }
                    })
                    .setMessage(getString(R.string.signin_dialog_message))
                    .create();
            mSignInDialog.show();
        }
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
        Log.d(TAG, "Connected");
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
        } else if (state == HaikuSession.State.HAS_SESSION && mUser == null) {
            mHaikuApi.fetchCurrentUser(this);
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
        // We never need to resolve this - either we have stored tokens on the server
        // and can use the id token without further interaction, or we don't and we
        // need to resolve the code.
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
        } else if (view.getId() == R.id.button_disconnect) {
            setProgressBarIndeterminateVisibility(true);
            mHaikuApi.disconnect(this);
        }
    }

    /**
     * Begin the non-immediate sign in flow
     */
    private void beginSignInFlow() {
        setProgressBarIndeterminateVisibility(true);
        HaikuSession.State state = mHaikuPlusSession.checkSessionState(true);
        if (state == HaikuSession.State.UNAUTHENTICATED) {
            Intent intent = AccountPicker.newChooseAccountIntent(
                    null, null, new String[]{"com.google"},
                    false, null, null, null, null);
            startActivityForResult(intent, REQ_CHOOSE_ACCOUNT);
        } else if (state == HaikuSession.State.HAS_ACCOUNT) {
            mShouldResolve = true;
            mHaikuApi.fetchCurrentUser(MainActivity.this);
        } else {
            mHaikuApi.fetchCurrentUser(MainActivity.this);
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
                mHaikuApi.fetchCurrentUser(this);
                mShouldResolve = true;
            }
        } else if (requestCode == REQ_CONSENT) {
            if (resultCode == RESULT_OK) {
                new CheckOrRetrieveCodeTask().execute();
            }
        } else if (requestCode == REQ_SHARE && resultCode == RESULT_OK) {
            // Sharing triggers its own toast so we don't need to feed back to the user.
            Log.d(TAG, "Post shared.");
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

    /**
     * This class will only be instantiated as part of an explicit sign in click,
     * so we need to check the server state immediately.This means testing with the ID
     * token for a session, and if that doesn't work firing off a code retrieve task.
     */
    private class CheckOrRetrieveCodeTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            if (mHaikuPlusSession.getAccountName() == null) {
                return null;
            }

            String code = null;

            try {
                code = mHaikuPlusSession.getCodeSynchronous();
            } catch (UserRecoverableAuthException userEx) {
                startActivityForResult(userEx.getIntent(), REQ_CONSENT);
            } catch (GoogleAuthException gaEx) {
                Log.e(TAG, gaEx.getMessage());
            } catch (HaikuSession.CodeException e) {
                Log.e(TAG, e.getMessage(), e);
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            return code;
        }

        @Override
        protected void onPostExecute(String uoc) {
            super.onPostExecute(uoc);
            if (uoc != null) {
                setProgressBarIndeterminateVisibility(true);
                mHaikuPlusSession.setCode(uoc);
                mHaikuApi.fetchCurrentUser(MainActivity.this);
            } else {
                Log.d(TAG, "Invalid user/code response");
            }
            mShouldResolve = false;
        }
    }
}
