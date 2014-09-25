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

import com.google.plus.samples.haikuplus.api.HaikuClient;
import com.google.plus.samples.haikuplus.api.VolleyContainer;
import com.google.plus.samples.haikuplus.models.Haiku;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import java.util.List;

/**
 * Array Adapter wrapper for the stream. As well as managing a list of haikus will also handle
 * the empty list case, and append a header view which appears in either case.
 *
 * @author sanstern@google,com (Sam Stern)
 * @author ianbarber@google.com (Ian Barber)
 */
public class HaikuArrayAdapter extends ArrayAdapter<Haiku> {
    private enum ViewTypes {
        LIST_ITEM,
        EMPTY_ITEM,
        FILTER_ITEM
    }

    private LayoutInflater mInflater;
    private VolleyContainer mVolley;
    private boolean mDisplayHeader = false;
    private View.OnClickListener mListener;
    private HaikuClient.StreamMode mCurrentMode = HaikuClient.StreamMode.ALL;

    public HaikuArrayAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_2);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mVolley = VolleyContainer.getInstance(context);

    }

    public void setData(List<Haiku> data, HaikuClient.StreamMode mode) {
        clear();
        if (data != null) {
            for (Haiku entry : data) {
                add(entry);
            }
        }
        mCurrentMode = mode;
    }

    /**
     * Set the listener to be used by the header views.
     *
     * @param listener
     */
    public void setOnClickListener(View.OnClickListener listener) {
        mListener = listener;
    }

    /**
     * Set the header view to appear.
     */
    public void enableHeaderView() {
        mDisplayHeader = true;
        notifyDataSetChanged();
    }

    /**
     * Hide the header view.
     */
    public void disableHeaderView() {
        mDisplayHeader = false;
        notifyDataSetChanged();
    }


    @Override
    public int getViewTypeCount() {
        return ViewTypes.values().length;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && mDisplayHeader) {
            return ViewTypes.FILTER_ITEM.ordinal();
        }

        if (super.getCount() == 0) {
            return ViewTypes.EMPTY_ITEM.ordinal();
        }

        return ViewTypes.LIST_ITEM.ordinal();
    }

    @Override
    public int getCount() {
        // We want to show at least one row, for the empty display.
        int count = Math.max(super.getCount(), 1);
        if (mDisplayHeader) {
            // If we're displaying the header, we need an extra row.
            count += 1;
        }
        return count;
    }

    @Override
    public Haiku getItem(int position) {
        // getCount returns (numItems + 1) when we display header
        if (position >= super.getCount()) {
            return null;
        } else {
            return super.getItem(position);
        }
    }

    /**
     * Get the Haiku from the list at a certain position.
     * @param position the selected position
     * @return a Haiku, or null if out of bounds
     */
    public Haiku getHaikuAt(int position) {
        if (mDisplayHeader) {
            return getItem(position - 1);
        } else if (super.getCount() == 0) {
            return null;
        } else {
            return getItem(position);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int type = getItemViewType(position);
        if (type == ViewTypes.EMPTY_ITEM.ordinal()) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.haiku_empty, parent, false);
            }
        } else if (type == ViewTypes.FILTER_ITEM.ordinal()) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_header, parent, false);
            }
            if (mListener != null) {
                convertView.findViewById(R.id.button_create_haiku).setOnClickListener(mListener);
                convertView.findViewById(R.id.radio_everyone).setOnClickListener(mListener);
                convertView.findViewById(R.id.radio_friends).setOnClickListener(mListener);
            }
            ((RadioButton) convertView.findViewById(R.id.radio_everyone))
                    .setChecked(HaikuClient.StreamMode.ALL.equals(mCurrentMode));
            ((RadioButton) convertView.findViewById(R.id.radio_friends))
                    .setChecked(HaikuClient.StreamMode.FRIENDS.equals(mCurrentMode));
        } else {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.haiku_item, parent, false);
            }

            Haiku item = getItem(position - (mDisplayHeader ? 1 : 0));

            NetworkImageView profile =
                    ((NetworkImageView) convertView.findViewById(R.id.haiku_profile_pic));
            profile.setImageUrl(item.author.googlePhotoUrl, mVolley.getImageLoader());
            ((TextView) convertView.findViewById(R.id.haiku_title))
                    .setText(item.title);
            ((TextView) convertView.findViewById(R.id.haiku_user_display_name))
                    .setText(item.author.googleDisplayName);
            ((TextView) convertView.findViewById(R.id.haiku_date)).setText(item.getFormattedDate());
            ((TextView) convertView.findViewById(R.id.haiku_vote_count))
                    .setText(item.votes + " Votes");
        }

        return convertView;
    }
}
