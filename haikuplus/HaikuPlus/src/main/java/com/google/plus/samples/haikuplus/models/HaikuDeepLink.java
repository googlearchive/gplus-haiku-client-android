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

package com.google.plus.samples.haikuplus.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class to represent a deeplink.
 *
 * @author ianbarber@google.com (Ian Barber)
 */
public class HaikuDeepLink implements Parcelable {
    private final String mHaikuId;
    private final String mAction;

    private HaikuDeepLink(String haikuId, String action) {
        mHaikuId = haikuId;
        mAction = action;
    }

    private HaikuDeepLink(Parcel in) {
        mHaikuId = in.readString();
        mAction = in.readString();
    }

    /**
     * Build a new HaikuDeepLink from a standard Haiku+
     * deeplink string.
     *
     * @param deepLink
     * @return HaikuDeepLink
     */
    public static HaikuDeepLink fromString(String deepLink) {
        String[] parts = deepLink.split("\\?action=");
        String action = null;
        if (parts.length == 2) {
            action = parts[1];
        }
        String haikuId = parts[0].replace("/haikus/", "");
        return new HaikuDeepLink(haikuId, action);
    }

    /**
     * Get the ID of the haiku referenced.
     *
     * @return
     */
    public String getHaikuId() {
        return mHaikuId;
    }

    /**
     * Get the action if a call to action was used, null if not.
     * @return
     */
    public String getAction() {
        return mAction;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mHaikuId);
        parcel.writeString(mAction);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public HaikuDeepLink createFromParcel(Parcel in) {
            return new HaikuDeepLink(in);
        }

        public HaikuDeepLink[] newArray(int size) {
            return new HaikuDeepLink[size];
        }
    };
}
