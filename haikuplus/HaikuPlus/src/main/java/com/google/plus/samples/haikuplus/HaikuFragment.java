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

import com.google.plus.samples.haikuplus.api.VolleyContainer;
import com.google.plus.samples.haikuplus.models.Haiku;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

/**
 * Display Fragment for showing the full content of a Haiku and associated user information.  Used
 * after a Haiku is selected from the stream.
 *
 * @author samstern@google,com (Sam Stern)
 * @author ianbarber@google.com (Ian Barber)
 */
public class HaikuFragment extends Fragment implements View.OnClickListener {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "haiku";
    private Haiku mHaiku;

    private HaikuInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param haiku The haiku to display
     * @return A new instance of fragment HaikuFragment.
     */
    public static HaikuFragment newInstance(Haiku haiku) {
        HaikuFragment fragment = new HaikuFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PARAM1, haiku);
        fragment.setArguments(args);
        return fragment;
    }
    public HaikuFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mHaiku = (Haiku) getArguments().getParcelable(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_haiku, container, false);
        VolleyContainer volley = VolleyContainer.getInstance(getActivity());

        if (mHaiku != null) {
            NetworkImageView profile = ((NetworkImageView) v.findViewById(R.id.haiku_profile_pic));
            profile.setImageUrl(mHaiku.author.googlePhotoUrl, volley.getImageLoader());
            ((TextView) v.findViewById(R.id.haiku_title)).setText(mHaiku.title);
            ((TextView) v.findViewById(R.id.haiku_line_one)).setText(mHaiku.lineOne);
            ((TextView) v.findViewById(R.id.haiku_line_two)).setText(mHaiku.lineTwo);
            ((TextView) v.findViewById(R.id.haiku_line_three)).setText(mHaiku.lineThree);
            ((TextView) v.findViewById(R.id.haiku_user_display_name))
                    .setText(mHaiku.author.googleDisplayName);
            ((TextView) v.findViewById(R.id.haiku_date)).setText(mHaiku.getFormattedDate());
            ((TextView) v.findViewById(R.id.haiku_vote_count)).setText(mHaiku.votes + " votes");
            ((Button) v.findViewById(R.id.button_promote)).setOnClickListener(this);
            ((Button) v.findViewById(R.id.button_vote)).setOnClickListener(this);
        }
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (HaikuInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {
        if (view == getView().findViewById(R.id.button_promote)) {
            mListener.onPromoteCall(mHaiku);
        } else if (view == getView().findViewById(R.id.button_vote)) {
            mListener.onVoteCall(mHaiku);
        }
    }

    /**
     * Add a vote to the Haiku and update the associated views
     */
    protected void incrementVotes() {
        mHaiku.votes++;
        ((TextView) getView().findViewById(R.id.haiku_vote_count))
                .setText(mHaiku.votes + " Votes");
    }
}
