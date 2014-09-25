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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class representing a single Haiku in Haiku+, as returned from the API.
 *
 * @author ianbarber@google.com (Ian Barber)
 */
public class Haiku implements Parcelable{

    private static SimpleDateFormat sDateFormat = new SimpleDateFormat("'on' MMM d yyyy");

    public String id;
    public User author;
    public String title;
    @SerializedName("line_one")
    public String lineOne;
    @SerializedName("line_two")
    public String lineTwo;
    @SerializedName("line_three")
    public String lineThree;
    public int votes;
    @SerializedName("creation_time")
    public Date creationTime;
    @SerializedName("content_url")
    public String contentUrl;
    @SerializedName("content_deep_link_id")
    public String contentDeepLinkId;
    @SerializedName("call_to_action_url")
    public String callToActionUrl;
    @SerializedName("call_to_action_deep_link_id")
    public String callToActionDeepLinkId;

    public Haiku() {

    }

    public Haiku(Parcel parcel) {
        id = parcel.readString();
        author = parcel.readParcelable(User.class.getClassLoader());
        title = parcel.readString();
        lineOne = parcel.readString();
        lineTwo = parcel.readString();
        lineThree = parcel.readString();
        votes = parcel.readInt();
        creationTime = (Date) parcel.readSerializable();
        contentUrl = parcel.readString();
        contentDeepLinkId = parcel.readString();
        callToActionUrl = parcel.readString();
        callToActionDeepLinkId = parcel.readString();
    }

    public static final Parcelable.Creator<Haiku> CREATOR
            = new Parcelable.Creator<Haiku>() {
        public Haiku createFromParcel(Parcel in) {
            return new Haiku(in);
        }

        public Haiku[] newArray(int size) {
            return new Haiku[size];
        }
    };

    public String getFormattedDate() {
        return sDateFormat.format(creationTime);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(id);
        parcel.writeParcelable(author, flags);
        parcel.writeString(title);
        parcel.writeString(lineOne);
        parcel.writeString(lineTwo);
        parcel.writeString(lineThree);
        parcel.writeInt(votes);
        parcel.writeSerializable(creationTime);
        parcel.writeString(contentUrl);
        parcel.writeString(contentDeepLinkId);
        parcel.writeString(callToActionUrl);
        parcel.writeString(callToActionDeepLinkId);
    }
}
