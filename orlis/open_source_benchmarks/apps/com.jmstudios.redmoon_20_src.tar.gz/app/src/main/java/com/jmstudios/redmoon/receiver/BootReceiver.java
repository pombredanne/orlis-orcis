/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
 *
 *  This file is free software: you may copy, redistribute and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This file is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *     Copyright (c) 2015 Chris Nguyen
 *
 *     Permission to use, copy, modify, and/or distribute this software
 *     for any purpose with or without fee is hereby granted, provided
 *     that the above copyright notice and this permission notice appear
 *     in all copies.
 *
 *     THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL
 *     WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED
 *     WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE
 *     AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR
 *     CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
 *     OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *     NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 *     CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.jmstudios.redmoon.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Calendar;

import com.jmstudios.redmoon.helper.FilterCommandFactory;
import com.jmstudios.redmoon.helper.FilterCommandSender;
import com.jmstudios.redmoon.model.SettingsModel;
import com.jmstudios.redmoon.service.ScreenFilterService;
import com.jmstudios.redmoon.presenter.ScreenFilterPresenter;
import com.jmstudios.redmoon.receiver.AutomaticFilterChangeReceiver;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final boolean DEBUG = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) Log.i(TAG, "Boot broadcast received!");

        FilterCommandSender commandSender = new FilterCommandSender(context);
        FilterCommandFactory commandFactory = new FilterCommandFactory(context);
        Intent onCommand = commandFactory.createCommand(ScreenFilterService.COMMAND_ON);
        Intent pauseCommand = commandFactory.createCommand(ScreenFilterService.COMMAND_PAUSE);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SettingsModel settingsModel = new SettingsModel(context.getResources(), sharedPreferences);

        boolean poweredOnBeforeReboot = settingsModel.getShadesPowerState();
        boolean pausedBeforeReboot = settingsModel.getShadesPauseState();

        // Handle "Always open on startup" flag
        boolean alwaysOpenOnBoot = false;
        if (alwaysOpenOnBoot) {
            if (DEBUG) Log.i(TAG, "\"Always open on startup\" flag was set; starting now.");

            AutomaticFilterChangeReceiver.scheduleNextOnCommand(context);
            AutomaticFilterChangeReceiver.scheduleNextPauseCommand(context);

            commandSender.send(onCommand);
            return;
        }

        // Handle "Keep running after reboot" flag
        boolean resumeAfterReboot = true;
        if (resumeAfterReboot) {
            if (DEBUG) Log.i(TAG, "\"Keep running after reboot\" flag was set.");

            if (poweredOnBeforeReboot) {
                if (DEBUG) Log.i(TAG, "Shades was on before reboot; resuming state.");

                // If the filter was on when the device was powered down and the
                // automatic brightness setting is on, then it still uses the
                // dimmed brightness and we need to restore the saved brightness
                // before proceeding.
                if (!pausedBeforeReboot && settingsModel.getBrightnessControlFlag()) {
                    ScreenFilterPresenter.setBrightnessState
                        (settingsModel.getBrightnessLevel(),
                         settingsModel.getBrightnessAutomatic(),
                         context);
                }

                AutomaticFilterChangeReceiver.scheduleNextOnCommand(context);
                AutomaticFilterChangeReceiver.scheduleNextPauseCommand(context);

                commandSender.send(getPredictedPauseState(pausedBeforeReboot, settingsModel) ?
                                   pauseCommand : onCommand);
            } else {
                if (DEBUG) Log.i(TAG, "Shades was off before reboot; no state to resume from.");
            }
            return;
        }

        // Allow ScreenFilterService to sync its state and any shared preferences to "off" mode
        Intent offCommand = commandFactory.createCommand(ScreenFilterService.COMMAND_OFF);
        commandSender.send(offCommand);
    }

    private static boolean getPredictedPauseState(boolean pausedBeforeReboot,
                                                  SettingsModel model) {
        if (model.getAutomaticFilterMode().equals("never")) {
            return pausedBeforeReboot;
        } else {
            Calendar now = Calendar.getInstance();

            String onTime = model.getAutomaticTurnOnTime();
            int onHour = Integer.parseInt(onTime.split(":")[0]);
            int onMinute = Integer.parseInt(onTime.split(":")[1]);
            Calendar on = Calendar.getInstance();
            on.set(Calendar.HOUR_OF_DAY, onHour);
            on.set(Calendar.MINUTE, onMinute);

            if (on.after(now))
                on.add(Calendar.DATE, -1);

            String offTime = model.getAutomaticTurnOffTime();
            int offHour = Integer.parseInt(offTime.split(":")[0]);
            int offMinute = Integer.parseInt(offTime.split(":")[1]);
            Calendar off = Calendar.getInstance();
            off.set(Calendar.HOUR_OF_DAY, offHour);
            off.set(Calendar.MINUTE, offMinute);

            while (off.before(on))
                off.add(Calendar.DATE, 1);

            if (DEBUG) {
                Log.d(TAG, "On: " + onTime + ", off: " + offTime);
                Log.d(TAG, "On DAY_OF_MONTH: " + Integer.toString(on.get(Calendar.DAY_OF_MONTH)));
                Log.d(TAG, "Off DAY_OF_MONTH: " + Integer.toString(off.get(Calendar.DAY_OF_MONTH)));
            }

            return !(now.after(on) && now.before(off));
        }
    }
}
