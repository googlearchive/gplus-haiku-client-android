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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.plus.samples.haikuplus.Constants;

import android.text.TextUtils;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Volley request type that implements the logic for managing the session and
 * marshalling/unmarshalling the JSON response.
 *
 * @param <T> the type of the returned/sent object.
 * @author ianbarber@google.com (Ian Barber)
 */
public class HaikuApiRequest<T> extends Request<T> {
    public static final String HEADER_HAIKU = "HaikuResult";
    public static final String REQUIRES_CODE = "Code";
    public static final String REQUIRES_RETRY = "Retry";

    private static final String TAG = HaikuApiRequest.class.getSimpleName();
    private static final String HEADER_XOAUTH = "X-OAuth-Code";
    private static final String HEADER_WWWAUTH = "WWW-Authenticate";
    private static final String HEADER_COOKIE = "Cookie";
    private static final String HEADER_AUTH = "Authorization";
    private static final String HEADER_BEARER = "Bearer ";
    private static final String HEADER_SETCOOKIE = "Set-Cookie";
    private static final String HEADER_USER_AGENT = "User-Agent";

    private static final int TIMEOUT_MS = 10 * 1000;
    private static final int MAX_RETRIES = 3;
    private static final int NO_RETRIES = 0;
    private static final float BACKOFF_MULT = 2.0f;

    private final Response.Listener<T> mListener;
    private Gson mGson;
    private TypeToken<T> mType;
    private HaikuSession mSession;
    private byte[] mBody;

    public HaikuApiRequest(TypeToken<T> type, int method, String url, Response.Listener<T> listener,
                           Response.ErrorListener errorListener, boolean backoff) {
        super(method, url, errorListener);
        mListener = listener;
        mGson = new GsonBuilder()
                // Fixes the support for the API date format.
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();
        mType = type;
        mBody = null;

        if (backoff) {
            // Exponential backoff with 10s retry, 2x multiplier, and 3 max retries
            setRetryPolicy(new HaikuRetryPolicy(TIMEOUT_MS, MAX_RETRIES, BACKOFF_MULT));
        } else {
            // No retry
            setRetryPolicy(new HaikuRetryPolicy(TIMEOUT_MS, NO_RETRIES, BACKOFF_MULT));
        }
    }

    public void setSession(HaikuSession session) {
        mSession = session;
    }

    @Override
    protected void deliverResponse(T response) {
        mListener.onResponse(response);
    }

    @Override
    public void deliverError(VolleyError error) {
        if (error.networkResponse != null) {
            extractCookieIfPresent(error.networkResponse);
            if (error.networkResponse.statusCode == 401) {
                String codeError = error.networkResponse.headers.get(HEADER_XOAUTH);
                if (codeError != null) {
                    error.networkResponse.headers.put(HEADER_HAIKU, REQUIRES_CODE);
                }

                String idTokenErr = error.networkResponse.headers.get(HEADER_WWWAUTH);
                if (idTokenErr != null) {
                    if (mSession != null) {
                        // We will blank out the session ID so we send an ID token.
                        mSession.storeSessionId(null);
                    }
                    error.networkResponse.headers.put(HEADER_HAIKU, REQUIRES_RETRY);
                }
            }
        }
        super.deliverError(error);
    }

    private void extractCookieIfPresent(NetworkResponse response) {
        String cookie = response.headers.get(HEADER_SETCOOKIE);
        if (cookie != null
                && cookie.startsWith(HaikuClient.COOKIE_PREFIX)
                && mSession != null
                ) {
            String session = cookie.substring(HaikuClient.COOKIE_PREFIX.length());
            String[] sections = session.split(";");
            mSession.storeSessionId(sections[0]);
        }
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        extractCookieIfPresent(response);

        String string;
        try {
            string = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            string = new String(response.data);
        }

        T parsed = null;
        if (!TextUtils.isEmpty(string)) {
            /* We can be confident in suppressing the unchecked cast here
            as mType is parameterised as T in the constructor. Temp
            variable introduced so we can scope the suppression to just
            the method.
             */
            try {
                @SuppressWarnings("unchecked")
                T tmpParsed = (T) mGson.fromJson(string, mType.getType());
                parsed = tmpParsed;
            } catch (JsonSyntaxException e) {
                Log.e(TAG, "Invalid JSON: " + string, e);
                return Response.error(new VolleyError("Error: could not parse JSON Response"));
            }
        }

        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    public Map<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HEADER_USER_AGENT, Constants.USER_AGENT);
        if (mSession == null) {
            return headers;
        }

        if (mSession.getCode() != null) {
            headers.put(HEADER_XOAUTH, mSession.getCode());
            // Don't want to send the same code twice, nullify after sending once
            mSession.setCode(null);
        }

        if (mSession.getSessionId() != null) {
            headers.put(HEADER_COOKIE, HaikuClient.COOKIE_PREFIX + mSession.getSessionId());
        } else if (mSession.getAccountName() != null) {
            if (mSession.getIdToken() != null) {
                headers.put(HEADER_AUTH, HEADER_BEARER + mSession.getIdToken());
            } else {
                Log.d(TAG, String.format("Bearer token null.  Id %s, Acct %s.",
                        mSession.getSessionId(), mSession.getAccountName()));
            }
        }

        return headers;
    }

    @Override
    public byte[] getBody() {
        return mBody;
    }

    public void setBody(T body) {
        String data = mGson.toJson(body);
        mBody = data.getBytes(Charset.forName("UTF-8"));
    }

    /**
     * Modification of Volley's DefaultRetryPolicy to never retry on a 401 response.
     */
    private static class HaikuRetryPolicy extends DefaultRetryPolicy {

        public HaikuRetryPolicy(int initialTimeoutMs, int maxNumRetries, float backoffMultiplier) {
            super(initialTimeoutMs, maxNumRetries, backoffMultiplier);
        }

        @Override
        public void retry(VolleyError volleyError) throws VolleyError {
            if (volleyError.networkResponse.statusCode == 401) {
                // On 401, raise error immediately
                throw volleyError;
            } else {
                // Otherwise, default to standard retry policy
                super.retry(volleyError);
            }
        }
    }
}
