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

package com.google.plus.samples.haikuplus.api;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.plus.samples.haikuplus.Constants;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

/**
 * Represents a single Haiku+ session, which may or may not be associated with an authenticated
 * user.
 *
 * @author ianbarber@google.com (Ian Barber)
 */
public class HaikuSession {
    private static final String TAG = "HaikuPlus-HaikuSession";

    /**
     * Enum for the authentication state of the session.  UNAUTHENTICATED is the lowest state, where
     * neither the account name nor the session id is known.  HAS_ACCOUNT is the next state, where
     * account name is known but the session id is not.  If the state is HAS_SESSION, both the
     * account name and the session id are known.
     */
    public enum State {
        UNAUTHENTICATED,
        HAS_ACCOUNT,
        HAS_SESSION
    }

    private static HaikuSession mSession;
    private final String mServerClientId;
    private static final String PREFS_NAME = "HaikuPlus-HaikuSession";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String PREF_SESSION_ID = "sessionId";
    private Context mContext;
    private String mAccountName;
    private String mSessionId;
    private String mCode;

    public static HaikuSession getSessionForServer(Context context, String serverClientId) {
        if (mSession != null) {
            return mSession;
        }
        mSession = new HaikuSession(context, serverClientId);
        // Do an an initial check just to pre-populate the values from the
        // shared preferences.
        mSession.fetchAccountName(false);
        return mSession;
    }

    private HaikuSession(Context context, String serverClientId) {
        mServerClientId = serverClientId;
        mContext = context.getApplicationContext();
    }

    public State checkSessionState(Boolean force) {
        fetchAccountName(force);

        if (mSessionId != null) {
            return State.HAS_SESSION;
        } else if (mAccountName != null) {
            return State.HAS_ACCOUNT;
        }
        return State.UNAUTHENTICATED;
    }

    public State checkSessionState() {
        return checkSessionState(false);
    }

    public void storeAccountName(String accountName) {
        mAccountName = accountName;
        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_ACCOUNT_NAME, mAccountName);
        editor.commit();
    }
    
    public void storeSessionId(String sessionId) {
        mSessionId = sessionId;
        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_SESSION_ID, mSessionId);
        editor.commit();
    }

    public void setCode(String code) {
        mCode = code;
    }

    public String getCode() {
        return mCode;
    }

    public String getSessionId() {
        return mSessionId;
    }

    public String getAccountName() {
        return mAccountName;
    }

    public String getIdTokenSynchronous() {
        if (mAccountName == null || mServerClientId == null) {
            return null;
        }
        String scope = "audience:server:client_id:" + mServerClientId;
        try {
            return GoogleAuthUtil.getToken(mContext, mAccountName, scope);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } catch (GoogleAuthException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    public String getCodeSynchronous() throws GoogleAuthException, CodeException {
        StringBuilder scopeString = new StringBuilder("");
        for (String scope : Constants.SCOPES) {
            scopeString.append(" ").append(scope);
        }
        String scope = new StringBuilder("oauth2:server:client_id:")
                .append(Constants.SERVER_CLIENT_ID)
                .append(":api_scope:")
                .append(scopeString.toString())
                .toString();
        Bundle appActivities = new Bundle();
        String types = TextUtils.join(" ", Constants.ACTIONS);
        appActivities.putString(GoogleAuthUtil.KEY_REQUEST_VISIBLE_ACTIVITIES, types);

        String code = null;
        try {
            code = GoogleAuthUtil.getToken(
                    mContext,
                    mAccountName,
                    scope,
                    appActivities);
            // Immediately invalidate so we get a different one if we have to try again.
            GoogleAuthUtil.invalidateToken(mContext, code);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new CodeException("Error: could not establish connection to server.");
        }

        return code;
    }

    /**
     * Retrieve the account name stores in the shared preferences if set.
     *
     * @param force require that the preference be checked.
     */
    private void fetchAccountName(Boolean force) {
        if (mAccountName == null || force) {
            SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
            mAccountName = settings.getString(PREF_ACCOUNT_NAME, null);
            mSessionId = settings.getString(PREF_SESSION_ID, null);
        }
    }

    /**
     * Exception raised while attempting to get code from server.
     */
    public static class CodeException extends Exception {
        public CodeException(String msg) {
            super(msg);
        }
    }
}
