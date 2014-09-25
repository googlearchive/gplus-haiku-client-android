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

package com.google.plus.samples.haikuplus.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

/**
 * A singleton container class for the Volley request queue
 * object.
 *
 * @author ianbarber@google.com (Ian Barber)
 */
public class VolleyContainer {
    private static final int CACHE_SIZE = 20;
    private static VolleyContainer mInstance = null;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    private VolleyContainer(Context context) {
        mRequestQueue = Volley.newRequestQueue(context);
        mImageLoader = new ImageLoader(mRequestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> mCache =
                    new LruCache<String, Bitmap>(CACHE_SIZE);
            @Override
            public Bitmap getBitmap(String s) {
                return mCache.get(s);
            }

            @Override
            public void putBitmap(String s, Bitmap bitmap) {
                mCache.put(s, bitmap);
            }
        });
    }

    public static VolleyContainer getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new VolleyContainer(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }
}
