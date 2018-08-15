/*
 * Copyright (c) 2016 Carmen Alvarez
 *
 * This file is part of Poet Assistant.
 *
 * Poet Assistant is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Poet Assistant is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Poet Assistant.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.rmen.android.poetassistant;

import android.content.Context;
import android.support.v7.app.AppCompatDelegate;

import ca.rmen.android.poetassistant.settings.Settings;
import ca.rmen.android.poetassistant.settings.SettingsPrefs;

public class Theme {

    public static void setThemeFromSettings(Context context) {
        String theme = SettingsPrefs.get(context).getTheme();
        AppCompatDelegate.setDefaultNightMode(Settings.THEME_DARK.equals(theme) ? AppCompatDelegate.MODE_NIGHT_YES: AppCompatDelegate.MODE_NIGHT_NO);
    }
}
