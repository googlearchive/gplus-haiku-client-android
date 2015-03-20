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

import com.android.volley.toolbox.RequestFuture;
import com.google.gson.reflect.TypeToken;
import com.google.plus.samples.haikuplus.Constants;
import com.google.plus.samples.haikuplus.models.Haiku;
import com.google.plus.samples.haikuplus.models.User;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * API wrapper class for the Haiku+ API. All interactions with the Haiku+ backend service should
 * take advantage of this class. The general approach is to provide simple method calls with
 * callbacks for success and failure.
 *
 * @author samstern@google.com (Sam Stern)
 * @author ianbarber@google.com (Ian Barber)
 */
public class HaikuClient {

    /**
     * Enum for Haikus visible in the stream. ALL shows all Haikus on the server, FRIENDS shows
     * only Haikus written by friends of the current user.
     */
    public enum StreamMode {
        ALL,
        FRIENDS
    }

    public static final String TAG = "HaikuPlus-Service";
    public static final String COOKIE_PREFIX = "HaikuSessionId=";
    public static final String CODE_ERROR = "code";
    private static final String GET_USER = "/api/users/me";
    private static final String GET_HAIKU = "/api/haikus/{haiku_id}";
    private static final String VOTE_HAIKU = "/api/haikus/{haiku_id}/vote";
    private static final String POST_HAIKU = "/api/haikus";
    private static final String LIST_HAIKUS = "/api/haikus";
    private static final String USER_SIGNOUT = "/api/signout";
    private static final String USER_DISCONNECT = "/api/disconnect";

    private static HaikuClient mInjectableClient;

    private final HaikuSession mHaikuSession;
    private final VolleyContainer mVolley;

    /**
     * Interface for the callback when a haiku is retrieved individually from the API.
     *
     * @author ianbarber@google.com (Ian Barber)
     */
    public interface HaikuRetrievedListener {
        public void onHaikuRetrieved(Haiku data);
    }

    /**
     * Type to represent a listener for callbacks from API requests to the Haiku+ API.
     *
     * @author ianbarber@google.com (Ian Barber)
     */
    public interface HaikuServiceListener {
        public void onUserRetrieved(User user);
        public void onHaikuWritten(Haiku haiku);
        public void onVoteWritten(Haiku haiku);

        public void onSignedOut();
    }

    /**
     * Interface for the callback when an updated list of haikus is retrieved from the service.
     *
     * @author ianbarber@google.com (Ian Barber)
     */
    public interface HaikuStreamListener {
        public void onHaikusRetrieved(List<Haiku> data);
    }

    /**
     * Construct a new client for making calls to the Haiku+ API.
     *
     * @param context application context
     * @param haikuSession authentication object
     */
    private HaikuClient(Context context, HaikuSession haikuSession) {
        mVolley = VolleyContainer.getInstance(context);
        mHaikuSession = haikuSession;
    }

    /**
     * Initialiser method for the haiku client.
     *
     * @param context
     * @param haikuSession
     * @return HaikuClient
     */
    public static HaikuClient getInstance(Context context, HaikuSession haikuSession) {
        if (mInjectableClient != null) {
            return mInjectableClient;
        }
        return new HaikuClient(context, haikuSession);
    }

    /**
     * Allow injecting a static client to be used by all consumers of the HaikuClient
     * - this is primarily to be used for testing.
     *
     * @param client
     */
    public static void setClientInstance(HaikuClient client) {
        mInjectableClient = client;
    }

    /**
     * Retrieve an individual haiku from the API.
     *
     * @param haikuId the opaque identifier for a haiku
     * @param listener the object to be called when the call is complete
     */
    public void fetchHaiku(final String haikuId, final HaikuRetrievedListener listener) {
        RequestQueue rq = mVolley.getRequestQueue();
        String path = GET_HAIKU.replace("{haiku_id}", haikuId);
        HaikuApiRequest<Haiku> haikuGet = new HaikuApiRequest<Haiku>(
                (new TypeToken<Haiku>() {
                }),
                Request.Method.GET,
                Constants.SERVER_URL + path,
                new Response.Listener<Haiku>() {
                    @Override
                    public void onResponse(Haiku data) {
                        listener.onHaikuRetrieved(data);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.d(TAG, "Retrieve haiku error");
                        if (listener != null) {
                            listener.onHaikuRetrieved(null);
                        }
                    }
                },
                true
        );
        haikuGet.setTag(GET_HAIKU);
        if (mHaikuSession != null) {
            haikuGet.setSession(mHaikuSession);
        }
        rq.add(haikuGet);
    }

    /**
     * Retrieve a list of haikus from the API.
     *
     * @param mode whether the haikus should be restricted to circles.
     * @param listener the object to be called when the request completes.
     */
    public void fetchStream(final StreamMode mode, final HaikuStreamListener listener) {
        RequestQueue rq = mVolley.getRequestQueue();
        String path = mode == StreamMode.ALL ? "" : "?filter=circles";
        HaikuApiRequest<List<Haiku>> streamGet = new HaikuApiRequest<List<Haiku>>(
                (new TypeToken<List<Haiku>>() {
                }),
                Request.Method.GET,
                Constants.SERVER_URL + LIST_HAIKUS + path,
                new Response.Listener<List<Haiku>>() {
                    @Override
                    public void onResponse(List<Haiku> data) {
                        listener.onHaikusRetrieved(data);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.d(TAG, "Retrieve haikus error");
                        if (listener != null) {
                            listener.onHaikusRetrieved(null);
                        }
                    }
                },
                true
        );
        streamGet.setTag(LIST_HAIKUS);
        if (mHaikuSession != null) {
            streamGet.setSession(mHaikuSession);
        }
        rq.add(streamGet);
    }

    /**
     * Retrieve the currently signed in user from the API, or an error indicating which
     * type of authentication should be tried next.
     *
     * @param listener the object to be called when the call completes.
     */
    public void fetchCurrentUser(final HaikuServiceListener listener) {
        RequestQueue rq = mVolley.getRequestQueue();
        HaikuApiRequest<User> userGet = new HaikuApiRequest<User>(
                (new TypeToken<User>() {
                }),
                Request.Method.GET,
                Constants.SERVER_URL + GET_USER,
                new Response.Listener<User>() {
                    @Override
                    public void onResponse(User data) {
                        listener.onUserRetrieved(data);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.d(TAG, "Retrieve user error: " + volleyError.getMessage());
                        if (listener != null) {
                            String err = getVolleyErrorCode(volleyError);
                            listener.onUserRetrieved(null);
                        }
                    }
                },
                true
        );
        userGet.setSession(mHaikuSession);
        userGet.setTag(GET_USER);
        rq.add(userGet);
    }

    /**
     * Write a haiku to the API. Requires that the user is authenticated.
     *
     * @param haiku the haiku to write
     * @param listener and object to call when the request is complete.
     */
    public void writeHaiku(final Haiku haiku, final HaikuServiceListener listener) {
        RequestQueue rq = mVolley.getRequestQueue();
        HaikuApiRequest<Haiku> haikuPost = new HaikuApiRequest<Haiku>(
                (new TypeToken<Haiku>() {
                }),
                Request.Method.POST,
                Constants.SERVER_URL + POST_HAIKU,
                new Response.Listener<Haiku>() {
                    @Override
                    public void onResponse(Haiku data) {
                        listener.onHaikuWritten(data);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.d(TAG, "Write Haiku error: " + volleyError.getMessage());
                        if (listener != null) {
                            listener.onHaikuWritten(null);
                        }
                    }
                },
                true
        );
        haikuPost.setSession(mHaikuSession);
        haikuPost.setTag(POST_HAIKU);
        haikuPost.setBody(haiku);
        rq.add(haikuPost);
    }

    /**
     * Add a vote for a haiku. Requires that the user is authenticated.
     *
     * @param haiku the haiku to vote for.
     * @param listener and object to call when the request is complete.
     */
    public void writeHaikuVote(final Haiku haiku, final HaikuServiceListener listener) {
        String path = VOTE_HAIKU.replace("{haiku_id}", haiku.id);
        RequestQueue rq = mVolley.getRequestQueue();
        HaikuApiRequest<Haiku> haikuPost = new HaikuApiRequest<Haiku>(
                (new TypeToken<Haiku>() {
                }),
                Request.Method.POST,
                Constants.SERVER_URL + path,
                new Response.Listener<Haiku>() {
                    @Override
                    public void onResponse(Haiku data) {
                        listener.onVoteWritten(data);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.d(TAG, "Write Haiku error: " + volleyError.getMessage());
                        if (listener != null) {
                            listener.onVoteWritten(haiku);
                        }
                    }
                },
                true
        );
        haikuPost.setSession(mHaikuSession);
        haikuPost.setTag(POST_HAIKU);
        rq.add(haikuPost);
    }

    /**
     * End the user session with the server.
     *
     * @param listener and object to call when the request is complete.
     */
    public void signOut(final HaikuServiceListener listener) {
        RequestQueue rq = mVolley.getRequestQueue();
        HaikuApiRequest<Object> signoutPost = new HaikuApiRequest<Object>(
                (new TypeToken<Object>() {
                }),
                Request.Method.POST,
                Constants.SERVER_URL + USER_SIGNOUT,
                new Response.Listener<Object>() {
                    @Override
                    public void onResponse(Object data) {
                        listener.onSignedOut();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        listener.onSignedOut();
                    }
                },
                false
        );
        signoutPost.setTag(USER_SIGNOUT);
        signoutPost.setSession(mHaikuSession);
        rq.add(signoutPost);
    }

    /**
     * Sign the user out on the server, but also revoke access.
     *
     * @param listener and object to call when the request is complete.
     */
    public void disconnect(final HaikuServiceListener listener) {
        RequestQueue rq = mVolley.getRequestQueue();
        HaikuApiRequest<Object> disconnectPost = new HaikuApiRequest<Object>(
                (new TypeToken<Object>() {
                }),
                Request.Method.POST,
                Constants.SERVER_URL + USER_DISCONNECT,
                new Response.Listener<Object>() {
                    @Override
                    public void onResponse(Object data) {
                        listener.onSignedOut();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        listener.onSignedOut();
                    }
                },
                false
        );
        disconnectPost.setTag(USER_DISCONNECT);
        disconnectPost.setSession(mHaikuSession);
        rq.add(disconnectPost);
    }

    /**
     * Helper method that checks the header fields for a usable error.
     *
     * @param error the raw error.
     * @return error constant.
     */
    private String getVolleyErrorCode(VolleyError error) {
        if (error == null || error.networkResponse == null
                || error.networkResponse.headers.get(HaikuApiRequest.HEADER_HAIKU) == null) {
            return null;
        }
        return error.networkResponse.headers.get(HaikuApiRequest.HEADER_HAIKU);
    }
}
