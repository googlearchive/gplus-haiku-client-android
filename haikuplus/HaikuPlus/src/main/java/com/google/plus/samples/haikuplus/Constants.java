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

import com.google.android.gms.common.Scopes;

/**
 * Defines constant values uses throughout the application, mostly relating to interacting with
 * Google APIs and the Haiku+ webserver.
 *
 * @author samstern@google.com (Sam Stern)
 * @author ianbarber@gooogle.com (Ian Barber)
 */
public class Constants {
    public static final String SERVER_URL = "YOUR_PUBLIC_SERVER_URL";
    public static final String SERVER_CLIENT_ID = "YOUR_WEB_CLIENT_ID";
    public static final String[] SCOPES = { Scopes.PLUS_LOGIN, "email" };
    public static final String[] ACTIONS = { "http://schemas.google.com/AddActivity",
                                             "http://schemas.google.com/ReviewActivity" };
    public static final String USER_AGENT = "Haiku+Client-Android";
    public static final String ACTION_VOTE =  "vote";
}
