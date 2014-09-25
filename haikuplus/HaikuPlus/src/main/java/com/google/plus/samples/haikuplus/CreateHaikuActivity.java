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

import com.google.plus.samples.haikuplus.models.Haiku;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

/**
 * Activity containing form and logic for creating a new Haiku and posting to the server.
 * Validates form to ensure that all fields are full before submit.
 *
 * @author samstern@google.com (Sam Stern)
 * @author ianbarber@google.com (Ian Barber)
 */
public class CreateHaikuActivity extends Activity implements View.OnClickListener {
    public static final String EXTRA_HAIKU = "haiku";

    private EditText mTitle;
    private EditText mLineOne;
    private EditText mLineTwo;
    private EditText mLineThree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_haiku);
        // Design has no action bar.
        getActionBar().hide();

        mTitle = (EditText) findViewById(R.id.create_field_title);
        mLineOne = (EditText) findViewById(R.id.create_field_lineone);
        mLineTwo = (EditText) findViewById(R.id.create_field_linetwo);
        mLineThree = (EditText) findViewById(R.id.create_field_linethree);

        findViewById(R.id.button_add).setOnClickListener(this);
        findViewById(R.id.button_cancel).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_add) {
            Haiku haiku = new Haiku();
            haiku.title = mTitle.getText().toString();
            haiku.lineOne = mLineOne.getText().toString();
            haiku.lineTwo = mLineTwo.getText().toString();
            haiku.lineThree = mLineThree.getText().toString();

            if (validateForm()) {
                Intent result = new Intent();
                result.putExtra(EXTRA_HAIKU, haiku);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        } else if (view.getId() == R.id.button_cancel) {
            finish();
        }
    }

    protected boolean validateForm() {
        boolean valid = true;

        valid = validateNotEmpty(mTitle, getString(R.string.error_title_empty)) && valid;
        valid = validateNotEmpty(mLineOne, getString(R.string.error_lineone_empty)) && valid;
        valid = validateNotEmpty(mLineTwo, getString(R.string.error_linetwo_empty)) && valid;
        valid = validateNotEmpty(mLineThree, getString(R.string.error_linethree_empty)) && valid;

        return valid;
    }

    /**
     * Validates an EditText to ensure that it is not empty.  If it is empty, return false and add
     * an error message on screen.  Otherwise, return true and clear any existing errors,
     * @param editText the EditText to validate,
     * @param error the error message to display if the EditText is empty.
     * @return false if the EditText was empty, true otherwise.
     */
    private boolean validateNotEmpty(EditText editText, String error) {
        if (editText.getText().toString().isEmpty()) {
            editText.setError(error);
            return false;
        } else {
            editText.setError(null);
            return true;
        }
    }
}
