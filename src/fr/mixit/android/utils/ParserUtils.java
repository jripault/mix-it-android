/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Modified by Peter Kuterna to support the Devoxx conference.
 */
package fr.mixit.android.utils;

import android.text.format.Time;

import java.util.regex.Pattern;


/**
 * Various utility methods used by {@link fr.mixit.android.io.JSONHandler} implementations.
 */
public class ParserUtils {

    /** Used to sanitize a string to be {@link android.net.Uri} safe. */
    private static final Pattern sSanitizePattern = Pattern.compile("[^a-z0-9-_]");
    private static final Pattern sParenPattern = Pattern.compile("\\(.*?\\)");

	private static Time sTime = new Time();

    /**
     * Sanitize the given string to be {@link android.net.Uri} safe for building
     * {@link android.content.ContentProvider} paths.
     */
    public static String sanitizeId(String input) {
        return sanitizeId(input, false);
    }

    /**
     * Sanitize the given string to be {@link android.net.Uri} safe for building
     * {@link android.content.ContentProvider} paths.
     */
    public static String sanitizeId(String input, boolean stripParen) {
        if (input == null) return null;
        if (stripParen) {
            // Strip out all parenthetical statements when requested.
            input = sParenPattern.matcher(input).replaceAll("");
        }
        return sSanitizePattern.matcher(input.toLowerCase()).replaceAll("");
    }

	/**
	 * Parse the given string as a RFC 3339 timestamp, returning the value as
	 * milliseconds since the epoch.
	 */
	public static long parseTime(String time) {
	    sTime.parse3339(time);
	    return sTime.toMillis(false);
	}

}
