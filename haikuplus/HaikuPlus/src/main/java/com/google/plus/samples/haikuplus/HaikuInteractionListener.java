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

import com.google.plus.samples.haikuplus.api.HaikuSession;
import com.google.plus.samples.haikuplus.models.Haiku;

/**
 * Interface to describe actions that can be performed on a Haiku in the stream and associated
 * callbacks.  Used for communication between HaikuFragment and its parent Activity, which should
 * implement this interface.
 *
 * @author samstern@google.com (Sam Stern)
 * @author ianbarber@google.com (Ian Barber)
 */
public interface HaikuInteractionListener {
    public void onHaikuSelected(Haiku haiku);
    public void onPromoteCall(Haiku haiku);
    public void onVoteCall(Haiku haiku);
    public HaikuSession getAuthenticator();
}
