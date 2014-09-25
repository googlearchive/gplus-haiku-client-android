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

import com.google.gson.annotations.SerializedName;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Model representing a Haiku+ user as stored on the Haiku API server.
 *
 * @author ianbarber@google.com (Ian Barber)
 */
public class User implements Parcelable {

    public String id;
    @SerializedName("google_plus_id")
    public String googlePlusId;
    @SerializedName("google_display_name")
    public String googleDisplayName;
    @SerializedName("google_photo_url")
    public String googlePhotoUrl;
    @SerializedName("google_profile_url")
    public String googleProfileUrl;
    @SerializedName("last_updated")
    public Date lastUpdated;

    public User() {

    }

    public User(Parcel parcel) {
        id = parcel.readString();
        googlePlusId = parcel.readString();
        googleDisplayName = parcel.readString();
        googlePhotoUrl = parcel.readString();
        googleProfileUrl = parcel.readString();
        lastUpdated = (Date) parcel.readSerializable();
    }

    public static final Parcelable.Creator<User> CREATOR
            = new Parcelable.Creator<User>() {
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(googlePlusId);
        parcel.writeString(googleDisplayName);
        parcel.writeString(googlePhotoUrl);
        parcel.writeString(googleProfileUrl);
        parcel.writeSerializable(lastUpdated);
    }
}
