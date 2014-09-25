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
import com.google.plus.samples.haikuplus.models.Haiku;
import com.google.plus.samples.haikuplus.models.User;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.RadioButton;

import java.util.List;

/**
 * A fragment representing the Haiku stream.
 *
 * @author ianbarber@google.com (Ian Barber)
 */
public class StreamFragment extends Fragment
        implements HaikuClient.HaikuStreamListener, View.OnClickListener,
        AdapterView.OnItemClickListener {
    private static final String STATE_MODE = "filter";
    private static final String STATE_USER = "user";
    private List<Haiku> mData;

    private HaikuInteractionListener mListener;
    private View.OnClickListener mCreateHaikuListener;

    private User mUser;
    private HaikuClient.StreamMode mCurrentMode = HaikuClient.StreamMode.ALL;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private HaikuArrayAdapter mAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public StreamFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new HaikuArrayAdapter(getActivity());
        mAdapter.setOnClickListener(this);
        if (savedInstanceState != null) {
            if (savedInstanceState.getInt(STATE_MODE) == HaikuClient.StreamMode.FRIENDS.ordinal()) {
                mCurrentMode = HaikuClient.StreamMode.FRIENDS;
            }
            if (savedInstanceState.getParcelable(STATE_USER) != null) {
                mUser = savedInstanceState.getParcelable(STATE_USER);
            }
        }
        refreshStream();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_MODE, mCurrentMode.ordinal());
        outState.putParcelable(STATE_USER, mUser);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_streamfragment, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AbsListView list = getListView();
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(this);
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

    /**
     * Trigger a refresh of the stream of Haikus displayed.
     */
    public void refreshStream() {
        HaikuClient api = HaikuClient.getInstance(getActivity(), mListener.getAuthenticator());
        api.fetchStream(mCurrentMode, this);
    }

    /**
     * Allow setting a user header.
     *
     * @param user User - the currently signed in user.
     */
    public void setUser(User user, View.OnClickListener parent) {
        mUser = user;
        mCreateHaikuListener = parent;
        if (mUser == null) {
            mAdapter.disableHeaderView();
        } else {
            mAdapter.enableHeaderView();
        }
    }

    @Override
    public void onHaikusRetrieved(List<Haiku> data) {
        mAdapter.setData(data, mCurrentMode);
        mData = data;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.radio_everyone && ((RadioButton) view).isChecked()) {
            view.setActivated(true);
            mCurrentMode = HaikuClient.StreamMode.ALL;
            refreshStream();
        } else if (view.getId() == R.id.radio_friends && ((RadioButton) view).isChecked()) {
            view.setActivated(true);
            mCurrentMode = HaikuClient.StreamMode.FRIENDS;
            refreshStream();
        } else if (view.getId() == R.id.button_create_haiku) {
            if (mCreateHaikuListener != null) {
                mCreateHaikuListener.onClick(view);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mListener != null) {
            Haiku clicked = mAdapter.getHaikuAt(position);
            if (clicked != null) {
                mListener.onHaikuSelected(clicked);
            }
        }
    }

    /**
     * Retrieve the list view in use by this fragment.
     *
     * @return primary list view
     */
    private AbsListView getListView() {
        if (getView() == null) {
            return null;
        }
        return (AbsListView) getView().findViewById(android.R.id.list);
    }
}
