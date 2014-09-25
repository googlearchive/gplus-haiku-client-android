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
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.scrollTo;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.widget.EditText;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Test the Haiku creation activity.
 *
 * @author samstern@google.com (Sam Stern)
 * @author ianbarber@google.com (Ian Barber)
 */
@LargeTest
public class CreateHaikuTest extends ActivityInstrumentationTestCase2<CreateHaikuActivity> {

    @SuppressWarnings("deprecation")
    public CreateHaikuTest() {
        // This constructor was deprecated - but we want to support lower API levels.
        super("com.google.android.apps.common.testing.ui.testapp", CreateHaikuActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    /**
     * Test the create form is as expected.
     */
    public void testExpectedFormFieldsPresent() {
        String haikuText = "Hello World";

        onView(withId(R.id.create_field_lineone))
                .check(matches(isDisplayed()));

        onView(withId(R.id.create_field_lineone))
                .perform(scrollTo(), click())
                .perform(typeText(haikuText))
                .perform(click());

        onView(withId(R.id.create_field_lineone))
                .check(matches(withText(haikuText)));

        onView(withId(R.id.button_add))
                .perform(scrollTo(), click());
    }

    /**
     * Tests that the EditText fields show errors when they are empty.
     * @throws Throwable
     */
    public void testErrorWhenHaikuEmpty() throws Throwable {
        // Validate form
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                CreateHaikuActivity activity = getActivity();
                activity.validateForm();
            }
        });

        // Check for errors
        onView(withId(R.id.create_field_title))
                .check(matches(hasError()));
        onView(withId(R.id.create_field_lineone))
                .check(matches(hasError()));
        onView(withId(R.id.create_field_linetwo))
                .check(matches(hasError()));
        onView(withId(R.id.create_field_linethree))
                .check(matches(hasError()));

    }

    /**
     * Tests that the EditText fields do not show errors when they are full.
     * @throws Throwable
     */
    public void testNoErrorWhenHaikuFull() throws Throwable {
        // Fill form
        String[] haiku = new String[]{ "Title", "Line1", "Line2", "Line3" };
        onView(withId(R.id.create_field_title))
                .perform(scrollTo(), click(), typeText(haiku[0]));
        onView(withId(R.id.create_field_lineone))
                .perform(scrollTo(), click(), typeText(haiku[1]));
        onView(withId(R.id.create_field_linetwo))
                .perform(scrollTo(), click(), typeText(haiku[2]));
        onView(withId(R.id.create_field_linethree))
                .perform(scrollTo(), click(), typeText(haiku[3]));

        // Validate form
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                CreateHaikuActivity activity = getActivity();
                activity.validateForm();
            }
        });

        // Check for no errors
        onView(withId(R.id.create_field_title))
                .check(matches(not(hasError())));
        onView(withId(R.id.create_field_lineone))
                .check(matches(not(hasError())));
        onView(withId(R.id.create_field_linetwo))
                .check(matches(not(hasError())));
        onView(withId(R.id.create_field_linethree))
                .check(matches(not(hasError())));
    }

    private static HasErrorMatcher hasError() {
        return new HasErrorMatcher();
    }

    private static class HasErrorMatcher extends TypeSafeMatcher<View> {

        @Override
        public boolean matchesSafely(View view) {
            if (!(view instanceof EditText)) {
                return false;
            }

            EditText editText = (EditText) view;
            CharSequence error = editText.getError();
            if (error != null && !error.toString().isEmpty()) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void describeTo(Description description) {
        }
    }

}
