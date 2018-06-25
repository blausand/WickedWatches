/**
 * DialogDelete.java
 * Implements the DialogDelete class
 * A DialogDelete deletes a station after asking the user for permission
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-18 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package net.blausand.wickedwatch.helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import net.blausand.wickedwatch.MainActivity;
import net.blausand.wickedwatch.PlayerService;
import net.blausand.wickedwatch.R;
import net.blausand.wickedwatch.core.Station;


/**
 * DialogDelete class
 */
public final class DialogDelete implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = DialogDelete.class.getSimpleName();


    /* Construct and show dialog */
    public static void show(final Activity activity, final Station station) {
        // stop player service using intent
        Intent intent = new Intent(activity, PlayerService.class);
        intent.setAction(ACTION_DISMISS);
        activity.startService(intent);
        LogHelper.v(LOG_TAG, "Opening delete dialog. Stopping player service.");

        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(activity);

        // add message to dialog
        deleteDialog.setMessage(R.string.dialog_delete_station_message);

        // add delete button
        deleteDialog.setPositiveButton(R.string.dialog_button_delete, new DialogInterface.OnClickListener() {
            // listen for click on delete button
            public void onClick(DialogInterface arg0, int arg1) {
                // hand station over to main activity
                ((MainActivity)activity).handleStationDelete(station);
            }
        });

        // add cancel button
        deleteDialog.setNegativeButton(R.string.dialog_generic_button_cancel, new DialogInterface.OnClickListener() {
            // listen for click on cancel button
            public void onClick(DialogInterface arg0, int arg1) {
                // do nothing
            }
        });

        // display delete dialog
        deleteDialog.show();
    }

}
