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

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;

import com.google.plus.samples.haikuplus.api.HaikuClient;
import com.google.plus.samples.haikuplus.models.Haiku;
import com.google.plus.samples.haikuplus.models.User;

import android.app.Activity;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Date;

/**
 * Test {@link com.google.plus.samples.haikuplus.MainActivity} flows.
 *
 * @author samstern
 */
@LargeTest
public class MainTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Haiku haiku;

    @SuppressWarnings("deprecation")
    public MainTest() {
        // This constructor was deprecated - but we want to support lower API levels.
        super("com.google.android.apps.common.testing.ui.testapp", MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Create an example Haiku
        haiku = new Haiku();
        haiku.title = "Hello";
        haiku.lineOne = "Line1";
        haiku.lineTwo = "Line2";
        haiku.lineThree = "Line3";
        haiku.callToActionDeepLinkId = "/1234?action=vote";
        haiku.callToActionUrl = "http://example.com/1234?action=vote";
        haiku.contentDeepLinkId = "/1234";
        haiku.creationTime = new Date();
        haiku.contentUrl = "http://example.com/1234";

        // Create an example user
        User u = new User();
        u.googleDisplayName = "Ted Test";
        u.googlePlusId = "1234";
        u.googlePhotoUrl = "http://example.com/image.jpg";
        u.id = "1";
        u.lastUpdated = new Date();
        haiku.author = u;
    }

    /**
     * Tests that a created Haiku properly displays in the stream.  Other tests cover testing the
     * creation of the Haiku {@link CreateHaikuTest} and the
     * display of the Haiku detail view {@link StreamTest}.
     */
    public void testHaikuCreatedFlow() throws Throwable {
        // Add mock Haiku to intent
        Intent intent = new Intent();
        intent.putExtra(CreateHaikuActivity.EXTRA_HAIKU, haiku);

        // Mock Haiku Client
        HaikuClient client = Mockito.mock(HaikuClient.class);
        HaikuClient.setClientInstance(client);

        // Start Activity
        final MainActivity activity = getActivity();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.onUserRetrieved(haiku.author);
            }
        });
        activity.onActivityResult(MainActivity.REQ_CREATE_HAIKU, Activity.RESULT_OK, intent);

        // Check that attempt was made to write Haiku
        verify(client).writeHaiku(Mockito.any(Haiku.class),
                Mockito.any(HaikuClient.HaikuServiceListener.class));

        // Add Haiku to Fragment's list
        StreamFragment streamFragment = (StreamFragment) activity.getFragmentManager()
                .findFragmentByTag(MainActivity.STREAM_FRAG_TAG);
        ArrayList<Haiku> data = new ArrayList<Haiku>();
        data.add(haiku);
        streamFragment.onHaikusRetrieved(data);

        // Check that the Haiku is shown
        onData(allOf(is(Haiku.class), matchesHaiku(haiku)))
                .check(matches(isDisplayed()));
    }

    /**
     * Tests that clicking on the 'vote' button for a Haiku results in an on-screen update
     * and a network request to the Haiku API.
     * @throws Throwable
     */
    public void testHaikuVoted() throws Throwable {
        // Mock Haiku API
        HaikuClient client = Mockito.mock(HaikuClient.class);
        HaikuClient.setClientInstance(client);

        // Create activity, set user, select Haiku
        final MainActivity activity = getActivity();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.onUserRetrieved(haiku.author);
                activity.onHaikuSelected(haiku);
            }
        });

        // Click vote on Haiku
        int oldHaikuVotes = haiku.votes;
        onView(withId(R.id.button_vote))
                .perform(click());

        // Check that the text changes
        int numVotes = oldHaikuVotes + 1;
        String votesString = numVotes + " Votes";
        onView(withId(R.id.haiku_vote_count))
                .check(matches(withText(votesString)));

        // Check that the writeVote method was called
        verify(client).writeHaikuVote(Mockito.any(Haiku.class),
                Mockito.any(HaikuClient.HaikuServiceListener.class));

    }

    /**
     * Tests the behavior of clicking 'Sign Out'.  This test is also sufficient for 'Disconnect'
     * because the only difference is the HaikuClient method called and the API is mocked-out anyway
     * so the test would perform exactly the same.
     * @throws Throwable
     */
    public void testSignOut() throws Throwable {
        // Mock API Client
        HaikuClient client = Mockito.mock(HaikuClient.class);
        HaikuClient.setClientInstance(client);

        // Start Activity and sign in
        final MainActivity activity = getActivity();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.onUserRetrieved(haiku.author);
            }
        });

        // Click Sign Out
        onView(withId(R.id.button_sign_out))
                .perform(click());

        // Test for network request
        verify(client).signOut(Mockito.any(HaikuClient.HaikuServiceListener.class));

        // Force sign out callback
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.onSignedOut();
            }
        });

        // Test for UI Change (signed out showing, signed in not showing)
        onView(withId(R.id.signed_out_container))
                .check(matches(isDisplayed()));
        onView(withId(R.id.signed_in_container))
                .check(matches(not(isDisplayed())));
    }

    public static HaikuMatcher matchesHaiku(Haiku toMatch) {
        return new HaikuMatcher(toMatch);
    }

    /**
     * Matcher to test if two Haiku objects have the same content.
     */
    private static class HaikuMatcher extends BaseMatcher<Object> {

        private final Haiku toMatch;

        public HaikuMatcher(Haiku toMatch) {
            this.toMatch = toMatch;
        }

        @Override
        public boolean matches(Object o) {
            if (o instanceof Haiku) {
                Haiku haiku = (Haiku) o;
                return (haiku.title == toMatch.title
                    && haiku.lineOne == toMatch.lineOne
                    && haiku.lineTwo == toMatch.lineTwo
                    && haiku.lineThree == toMatch.lineThree);
            } else {
                return false;
            }
        }

        @Override
        public void describeTo(Description description) {

        }
    }
}
