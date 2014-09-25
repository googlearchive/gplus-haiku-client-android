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

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.plus.samples.haikuplus.api.HaikuClient;
import com.google.plus.samples.haikuplus.models.Haiku;
import com.google.plus.samples.haikuplus.models.User;

import android.content.Intent;
import android.net.Uri;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import org.mockito.Mockito;

import java.util.Date;

/**
 * Test the Haiku stream.
 *
 * @author samstern@google.com (Sam Stern)
 * @author ianbarber@google.com (Ian Barber)
 */
@LargeTest
public class StreamTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Haiku haiku;

    @SuppressWarnings("deprecation")
    public StreamTest() {
        // This constructor was deprecated - but we want to support lower API levels.
        super("com.google.android.apps.common.testing.ui.testapp", MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
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
     * Tests the behavior when loading a deep link to a specific Haiku
     */
    public void testLoadingDeeplinkHaikuIsDisplayed() throws Throwable {
        HaikuClient mockClient = mock(HaikuClient.class);
        HaikuClient.setClientInstance(mockClient);

        // Simulate a deep link to a specific Haiku
        Intent intent = new Intent();
        intent.setAction("com.google.android.apps.plus.VIEW_DEEP_LINK");
        intent.setData(Uri.parse("vnd.google.deeplink://link/?deep_link_id=/haikus/53358b1949b3d?action=vote&gplus_source=stream_interactive_post"));
        setActivityIntent(intent);

        // Start the activity, set a user to simulate logged in
        final MainActivity activity = getActivity();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.onUserRetrieved(haiku.author);
            }
        });

        // Check that the Haiku is properly displayed
        activity.onHaikuRetrieved(haiku);
        onView(withId(R.id.haiku_title))
                .check(matches(withText(haiku.title)));

        // Verify that HaikuClient.fetchHaiku was called as a result of the deep link
        verify(mockClient).fetchHaiku(
                anyString(),
                Mockito.<HaikuClient.HaikuRetrievedListener>anyObject());
    }

    /**
     * Tests selecting a Haiku, ensures that the full content of the Haiku is displayed on screen.
     */
    public void testSelectHaiku() {
        // Start activity and select a Haiku
        MainActivity activity = getActivity();
        activity.onHaikuSelected(haiku);

        // Verify that we can see the Haiku content
        onView(withId(R.id.haiku_title))
                .check(matches(withText(haiku.title)));
        onView(withId(R.id.haiku_line_one))
                .check(matches(withText(haiku.lineOne)));
        onView(withId(R.id.haiku_line_two))
                .check(matches(withText(haiku.lineTwo)));
        onView(withId(R.id.haiku_line_three))
                .check(matches(withText(haiku.lineThree)));
        onView(withId(R.id.haiku_user_display_name))
                .check(matches(withText(haiku.author.googleDisplayName)));

    }
}
