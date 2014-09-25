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

import org.apache.http.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * Static utilities for handling HTTP responses.
 *
 * @author ldenison@google.com (Lee Denison)
 */
public class HttpUtils {

    /** Buffer size used to read HTTP responses. */
    public static final int IO_BUFFER_SIZE = 4 * 1024;

    /**
     * Maximum total size of HTTP response that will be read.
     * Larger responses will be considered an error.
     */
    public static final int MAX_READ_SIZE = 64 * 1024 * 1024; // 64 MiB

    /**
     * Read the content of an HttpResponse.
     *
     * @param response the HttpResponse to be read.
     * @return the content of the HttpResponse as a ByteArrayOutputStream.
     * @throws IOException if the content size exceeds {@link #MAX_READ_SIZE} bytes.
     */
    public static ByteArrayOutputStream getContent(HttpResponse response) throws IOException {
        return getContent(response.getEntity().getContent());
    }

    /**
     * Read the content of an InputStream.
     *
     * @param inputStream the InputStream to be read.
     * @return the content of the InputStream as a ByteArrayOutputStream.
     * @throws IOException if the content size exceeds {@link #MAX_READ_SIZE} bytes.
     */
    public static ByteArrayOutputStream getContent(InputStream inputStream) throws IOException {
        ByteArrayOutputStream content = new ByteArrayOutputStream();

        // Read the response into a buffered stream
        int totalBytes = 0;
        int readBytes = 0;
        byte[] buffer = new byte[IO_BUFFER_SIZE];
        while ((readBytes = inputStream.read(buffer)) != -1) {
            if (totalBytes > MAX_READ_SIZE) {
                throw new IOException("Data download too large.");
            }

            content.write(buffer, 0, readBytes);
            totalBytes += readBytes;
        }

        return content;
    }

    public static String getErrorResponse(HttpURLConnection urlConnection) {
        InputStream errorStream = urlConnection.getErrorStream();

        try {
            if (errorStream != null) {
                byte[] responseBytes = getContent(errorStream).toByteArray();
                return new String(responseBytes, "UTF-8");
            }
        } catch (IOException e) {
            return "Failed to get error response: " + e.getLocalizedMessage();
        }

        return null;
    }
}