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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.LevelListDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import fr.mixit.android.R;
import fr.mixit.android.ui.AboutActivity;
import fr.mixit.android.ui.HomeActivity;
import fr.mixit.android.ui.SettingsActivity;

import java.util.List;
import java.util.TimeZone;

public class UIUtils {

	public static TimeZone CONFERENCE_TIME_ZONE = TimeZone.getTimeZone("Europe/Brussels");

	public static final long CONFERENCE_START_MILLIS = ParserUtils.parseTime(
	        "2011-04-05T08:30:00.000+01:00");
	public static final long CONFERENCE_END_MILLIS = ParserUtils.parseTime(
	        "2011-04-05T19:00:00.000+01:00");

	private static final int DAY_FLAGS = DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY;
	private static final int TIME_FLAGS = DateUtils.FORMAT_SHOW_TIME;

	private static final int BRIGHTNESS_THRESHOLD = 150;

	private static Time sTime = new Time();

	private static StyleSpan sBoldSpan = new StyleSpan(Typeface.BOLD);

	public static void setTitleBarColor(View titleBarView, int color) {
	    final ViewGroup titleBar = (ViewGroup) titleBarView;
	    titleBar.setBackgroundColor(color);

	    /*
	     * Calculate the brightness of the titlebar color, based on the commonly known
	     * brightness formula:
	     *
	     * http://en.wikipedia.org/wiki/HSV_color_space%23Lightness
	     */
	    int brColor = (30 * Color.red(color) +
	                   59 * Color.green(color) +
	                   11 * Color.blue(color)) / 100;
	    if (brColor > BRIGHTNESS_THRESHOLD) {
	        ((TextView) titleBar.findViewById(R.id.title_text)).setTextColor(
	                titleBar.getContext().getResources().getColor(R.color.title_text_alt));

	        // Iterate through all children of the titlebar and if they're a LevelListDrawable,
	        // set their level to 1 (alternate).
	        // TODO: find a less hacky way of doing this.
	        titleBar.post(new Runnable() {
	            public void run() {
	                final int childCount = titleBar.getChildCount();
	                for (int i = 0; i < childCount; i++) {
	                    final View child = titleBar.getChildAt(i);
	                    if (child instanceof ImageButton) {
	                        final ImageButton childButton = (ImageButton) child;
	                        if (childButton.getDrawable() != null &&
	                            childButton.getDrawable() instanceof LevelListDrawable) {
	                            ((LevelListDrawable) childButton.getDrawable()).setLevel(1);
	                        }
	                    }
	                }
	            }
	        });
	    }
	}

	public static int lightenColor(int color) {
	    final float [] hsv = new float[3];
	    Color.colorToHSV(color, hsv);
	    hsv[1] = 0.1F;
	    hsv[2] = 1.0F;
	    return Color.HSVToColor(hsv);
	}

	public static int darkenColor(int color) {
	    final float [] hsv = new float[3];
	    Color.colorToHSV(color, hsv);
	    hsv[1] = 1.0F;
	    hsv[2] = 0.5F;
	    return Color.HSVToColor(hsv);
	}

	public static void setHeaderColor(View headerItem, int color) {
		final int lightenColor = lightenColor(color);
		final int darkenColor = darkenColor(color);

		headerItem.setBackgroundColor(lightenColor);
	    ((TextView) headerItem.findViewById(R.id.header_text)).setTextColor(darkenColor);
	}

    /**
     * Invoke "home" action, returning to {@link HomeActivity}.
     */
    public static void goHome(Context context) {
        final Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    /**
     * Invoke "search" action, triggering a default search.
     */
    public static void goSearch(Activity activity) {
        activity.startSearch(null, false, Bundle.EMPTY, false);
    }

	/**
	 * Invoke "settings" action, go to {@link SettingsActivity}.
	 */
	public static void goSettings(Context context) {
	    final Intent intent = new Intent(context, SettingsActivity.class);
	    context.startActivity(intent);
	}

	/**
	 * Invoke "about" action, go to {@link AboutActivity}.
	 */
	public static void goAbout(Context context) {
	    final Intent intent = new Intent(context, AboutActivity.class);
	    context.startActivity(intent);
	}

	/**
	 * Format and return the given {@link fr.mixit.android.provider.MixItContract.Slots} and {@link Session#ROOM} values using
	 * {@link #CONFERENCE_TIME_ZONE}.
	 */
	public static String formatSessionSubtitle(long slotStart, long slotEnd, String roomName, Context context) {
	    TimeZone.setDefault(CONFERENCE_TIME_ZONE);

		CharSequence retour = DateUtils.formatDateRange(context, slotStart, slotEnd, TIME_FLAGS);
		if (roomName != null && roomName.length() > 0) {
			retour = context.getString(R.string.session_subtitle, retour, roomName);
		}

	    return retour.toString();
	}

	public static void setSessionTitleColor(long blockStart, long blockEnd, TextView title,
	        TextView subtitle) {
	    long currentTimeMillis = System.currentTimeMillis();
	    int colorId = android.R.color.primary_text_light;
	    int subColorId = android.R.color.secondary_text_light;

	    if (currentTimeMillis > blockEnd &&
	            currentTimeMillis < CONFERENCE_END_MILLIS) {
	        colorId = subColorId = R.color.session_foreground_past;
	    }

	    final Resources res = title.getResources();
	    title.setTextColor(res.getColor(colorId));
	    subtitle.setTextColor(res.getColor(subColorId));
	}

	/**
	 * Populate the given {@link TextView} with the requested text, formatting
	 * through {@link android.text.Html#fromHtml(String)} when applicable. Also sets
	 * {@link TextView#setMovementMethod} so inline links are handled.
	 */
	public static void setTextMaybeHtml(TextView view, String text) {
	    if (text.contains("<") && text.contains(">")) {
	        view.setText(Html.fromHtml(text));
	        view.setMovementMethod(LinkMovementMethod.getInstance());
	    } else {
	        view.setText(text);
	    }
	}

	/**
	 * Given a snippet string with matching segments surrounded by curly
	 * braces, turn those areas into bold spans, removing the curly braces.
	 */
	public static Spannable buildStyledSnippet(String snippet) {
	    final SpannableStringBuilder builder = new SpannableStringBuilder(snippet);

	    // Walk through string, inserting bold snippet spans
	    int startIndex = -1, endIndex = -1, delta = 0;
	    while ((startIndex = snippet.indexOf('{', endIndex)) != -1) {
	        endIndex = snippet.indexOf('}', startIndex);

	        // Remove braces from both sides
	        builder.delete(startIndex - delta, startIndex - delta + 1);
	        builder.delete(endIndex - delta - 1, endIndex - delta);

	        // Insert bold style
	        builder.setSpan(sBoldSpan, startIndex - delta, endIndex - delta - 1,
	                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

	        delta += 2;
	    }

	    return builder;
	}

	/**
	 * Returns the version name we are currently in
	 * @param appPackageName - full name of the package of an app, 'com.dcg.meneame' for example.
	 */
	public static String getAppVersionName(Context context, String appPackageName) {
	    if (context!= null) {
	        try {
	            return context.getPackageManager().getPackageInfo(appPackageName, 0).versionName;
	        } catch (PackageManager.NameNotFoundException e) {
	        }
	    }
	    return null;
	}

	/**
	 * Based upon {@linkplain http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html}
	 *
	 * Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 *
	 * @param context The application's environment.
	 * @param intent The Intent action to check for availability.
	 *
	 * @return True if an Intent with the specified action can be sent and
	 *         responded to, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, Intent intent) {
	    final PackageManager packageManager = context.getPackageManager();
	    List list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
	    return list.size() > 0;
	}

}
