/**
 * PlayerActivityFragment.java
 * Implements the main fragment of the player activity
 * This fragment is a detail view with the ability to start and stop playback
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.os.EnvironmentCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.y20k.transistor.core.Collection;
import org.y20k.transistor.helpers.DialogDelete;
import org.y20k.transistor.helpers.DialogRename;
import org.y20k.transistor.helpers.ImageHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * PlayerActivityFragment class
 */
public final class PlayerActivityFragment extends Fragment {

    /* Define log tag */
    private static final String LOG_TAG = PlayerActivityFragment.class.getSimpleName();


    /* Keys */
    private static final String STREAM_URI = "streamUri";
    private static final String STATION_NAME = "stationName";
    private static final String STATION_ID = "stationID";
    private static final String STATION_ID_CURRENT = "stationIDCurrent";
    private static final String STATION_ID_LAST = "stationIDLast";
    private static final String PLAYBACK = "playback";
    private static final String ACTION_PLAYBACK_STOPPED = "org.y20k.transistor.action.PLAYBACK_STOPPED";
    private static final int REQUEST_LOAD_IMAGE = 1;
    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 1;


    /* Main class variables */
    private Activity mActivity;
    private View mRootView;
    private String mStationName;
    private String mStreamUri;
    private TextView mStationNameView;
    private ImageView mStationImageView;
    private ImageButton mPlaybackButton;
    private ImageView mPlaybackIndicator;
    private int mStationID;
    private int mStationIDCurrent;
    private int mStationIDLast;
    private boolean mPlayback;
    private Collection mCollection;
    private PlayerService mPlayerService;
//    private SleepTimerService mSleepTimerService;
//    private Snackbar mSleepTimerNotification;
//    private String mSleepTimerNotificationMessage;
//    private boolean mSleepTimerRunning;


    /* Constructor (default) */
    public PlayerActivityFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get activity
        mActivity = getActivity();

        // initiate playback service
        mPlayerService = new PlayerService();

        // load playback state from preferences
        loadPlaybackState(mActivity);

        // get station name, URL and id from intent
        Intent intent = mActivity.getIntent();
        mStationID = intent.getIntExtra(STATION_ID, -1);
        mStationName = intent.getStringExtra(STATION_NAME);
        mStreamUri = intent.getStringExtra(STREAM_URI);

        // set station ID if not in intent
        if (mStationID == -1) {
            mStationID = mStationIDCurrent;
        }

        // get collection from external storage
        mCollection = new Collection(getCollectionDirectory("Collection"));

        // get URL and name for stream
        mStreamUri = mCollection.getStations().get(mStationID).getStreamUri().toString();
        mStationName = mCollection.getStations().get(mStationID).getStationName();

        // fragment has options menu
        setHasOptionsMenu(true);

        // initialize broadcast receivers
        initializeBroadcastReceivers();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // inflate rootview from xml
        mRootView = inflater.inflate(R.layout.fragment_player, container, false);

        // find views for station name and image and playback indicator
        mStationNameView = (TextView) mRootView.findViewById(R.id.player_textview_stationname);
        mStationImageView = (ImageView) mRootView.findViewById(R.id.player_imageview_station_icon);
        mPlaybackIndicator = (ImageView) mRootView.findViewById(R.id.player_playback_indicator);

        // set station image
        Bitmap stationImage = createStationImage();
        if (stationImage != null) {
            mStationImageView.setImageBitmap(stationImage);
        }

        // set text view to station name and add listener for clipboard copy
        mStationNameView.setText(mStationName);
        mStationNameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyStationToClipboard();
            }
        });

        // construct big playback button
        mPlaybackButton = (ImageButton) mRootView.findViewById(R.id.player_playback_button);
        mPlaybackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // handle tap on big playback button
                handlePlaybackButtonClick();
            }
        });

        return mRootView;
    }


    @Override
    public void onResume() {
        super.onResume();
        // refresh playback state
        loadPlaybackState(mActivity);
        // set up button symbol and playback indicator
        setVisualState();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            // CASE ICON
            case R.id.menu_icon:
                // get system picker for images
                selectFromImagePicker();
                return true;

            // CASE RENAME
            case R.id.menu_rename:
                // construct rename dialog
                final DialogRename dialogRename = new DialogRename(mActivity, mCollection, mStationName, mStationID);
                dialogRename.setStationRenamedListener(new DialogRename.StationRenamedListener() {
                    @Override
                    public void stationRenamed() {
                        mStationNameView.setText(dialogRename.getStationName());
                    }
                });
                // run dialog
                dialogRename.show();
                return true;

            // CASE DELETE
            case R.id.menu_delete:
                // stop playback
                mPlayerService.startActionStop(mActivity);
                // construct delete dialog
                DialogDelete dialogDelete = new DialogDelete(mActivity, mCollection, mStationID);
                dialogDelete.setStationDeletedListener(new DialogDelete.StationDeletedListener() {
                    @Override
                    public void stationDeleted() {
                        // start main activity
                        Intent mainActivityStartIntent = new Intent(mActivity, MainActivity.class);
                        startActivity(mainActivityStartIntent);
                        // finish player activity
                        mActivity.finish();
                    }
                });
                // run dialog
                dialogDelete.show();
                return true;

            // CASE DEFAULT
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted - get system picker for images
                    Intent pickImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickImageIntent, REQUEST_LOAD_IMAGE);
                } else {
                    // permission denied
                    Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_permission_denied) + " READ_EXTERNAL_STORAGE", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data) {
            // retrieve selected image from image picker
            processNewImage(data.getData());
        }
    }


    /* Starts player service */
    private void startPlayback() {
        // set playback true
        mPlayback = true;
        // rotate playback button
        changeVisualState(mActivity);
        // start player
        Log.v(LOG_TAG, "Starting player service.");
        mPlayerService.startActionPlay(mActivity, mStreamUri, mStationName);
    }


    /* Stops player service */
    private void stopPlayback() {
        // set playback false
        mPlayback = false;
        // rotate playback button
        changeVisualState(mActivity);
        // stop player
        Log.v(LOG_TAG, "Stopping player service.");
        mPlayerService.startActionStop(mActivity);
    }


    /* Handles tap on the big playback button */
    private void handlePlaybackButtonClick() {
        // playback stopped or new station - start playback
        if (!mPlayback || mStationID != mStationIDCurrent) {
            startPlayback();
        }
        // playback active - stop playback
        else {
            stopPlayback();
        }
        // update and save currently and last played station
        setStationState();
        savePlaybackState(mActivity);
    }


    /* Create Bitmap image for station */
    private Bitmap createStationImage () {
        Bitmap stationImageSmall;
        ImageHelper imageHelper;
        if (mCollection.getStations().get(mStationID).getStationImageFile().exists()) {
            // get image from collection
            stationImageSmall = BitmapFactory.decodeFile(mCollection.getStations().get(mStationID).getStationImageFile().toString());
        } else {
            // get default image
            stationImageSmall = BitmapFactory.decodeResource(getResources(), R.drawable.ic_notesymbol);
        }
        imageHelper = new ImageHelper(stationImageSmall, mActivity);
        imageHelper.setBackgroundColor(R.color.transistor_grey_lighter);

        return imageHelper.createCircularFramedImage(192);
    }


    /* Copy station date to system clipboard */
    private void copyStationToClipboard() {
        if (mStreamUri != null && mStationName != null) {
            // prepare clip
            String stationData = mStationName + " - " + mStreamUri;
            ClipData clip = ClipData.newPlainText("simple text",stationData);


            // copy clip to clipboard
            ClipboardManager cm = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(clip);

            // notify user
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_station_copied), Toast.LENGTH_SHORT).show();
        }
    }


    /* Processes new image and saves it to storage */
    private void processNewImage(Uri newImageUri) {

        ImageHelper imageHelper = new ImageHelper(newImageUri, mActivity);
        Bitmap newImage = imageHelper.getInputImage();

        if (newImage != null) {
            // write image to storage
            File stationImageFile = mCollection.getStations().get(mStationID).getStationImageFile();
            try (FileOutputStream out = new FileOutputStream(stationImageFile)) {
                newImage.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Unable to save: " + newImage.toString());
            }
            // change mStationImageView
            imageHelper.setBackgroundColor(R.color.transistor_grey_lighter);
            Bitmap stationImage = imageHelper.createCircularFramedImage(192);
            mStationImageView.setImageBitmap(stationImage);
        } else {
            Log.e(LOG_TAG, "Unable to get image from media picker: " + newImageUri.toString());
        }
    }


    /* Animate button and then set visual state */
    private void changeVisualState(Context context) {
        // get rotate animation from xml
        Animation rotate;
        if (mPlayback) {
            // if playback has been started get start animation
            rotate = AnimationUtils.loadAnimation(context, R.anim.rotate_clockwise_slow);
        } else {
            // if playback has been stopped get stop animation
            rotate = AnimationUtils.loadAnimation(context, R.anim.rotate_counterclockwise_fast);
        }

        // attach listener for animation end
        rotate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // set up button symbol and playback indicator afterwards
                setVisualState();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        // start animation of button
        mPlaybackButton.startAnimation(rotate);

    }


    /* Set button symbol and playback indicator */
    private void setVisualState() {

        // this station is running
        if (mPlayback && mStationID == mStationIDCurrent) {
            // change playback button image to stop
            mPlaybackButton.setImageResource(R.drawable.smbl_stop);
            // change playback indicator
            mPlaybackIndicator.setBackgroundResource(R.drawable.ic_playback_indicator_started_24dp);
        }
        // playback stopped
        else {
            // change playback button image to play
            mPlaybackButton.setImageResource(R.drawable.smbl_play);
            // change playback indicator
            mPlaybackIndicator.setBackgroundResource(R.drawable.ic_playback_indicator_stopped_24dp);
        }
    }


    /* set state of currently playing station */
    private void setStationState() {
        // playback started
        if (mPlayback) {
            mStationIDLast = mStationIDCurrent;
            mStationIDCurrent = mStationID;
        }
        // playback stopped
        else {
            mStationIDLast = mStationIDCurrent;
        }
    }


    /* Saves playback state to SharedPreferences */
    private void savePlaybackState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(STATION_ID_CURRENT, mStationIDCurrent);
        editor.putInt(STATION_ID_LAST, mStationIDLast);
        editor.putBoolean(PLAYBACK, mPlayback);
        editor.apply();
        Log.v(LOG_TAG, "Saving state.");
    }


    /* Loads playback state from preferences */
    private void loadPlaybackState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mStationIDCurrent = settings.getInt(STATION_ID_CURRENT, -1);
        mStationIDLast = settings.getInt(STATION_ID_LAST, -1);
        mPlayback = settings.getBoolean(PLAYBACK, false);
        Log.v(LOG_TAG, "Loading state.");
    }


    /* Check permissions and start image picker */
    private void selectFromImagePicker() {
        // permission to read external storage granted
        if (ActivityCompat.checkSelfPermission(mActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {

            // get system picker for images
            Intent pickImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickImageIntent, REQUEST_LOAD_IMAGE);
        }
        // permission to read external storage granted
        else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // ask for permission and explain why
                Snackbar snackbar = Snackbar.make(mRootView, mActivity.getString(R.string.snackbar_request_storage_access), Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction(R.string.dialog_generic_button_okay, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                });
                snackbar.show();

            } else {
                // ask for permission without explanation
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }
    }


    /* Initializes broadcast receivers for onCreate */
    private void initializeBroadcastReceivers() {

        // broadcast receiver: player service stopped playback
        BroadcastReceiver playbackStoppedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // set playback false
                mPlayback = false;
                // rotate playback button
                changeVisualState(context);
                // update currently and last played station
                setStationState();
                // save state of playback to settings
                savePlaybackState(context);
            }
        };
        IntentFilter playbackIntentFilter = new IntentFilter(ACTION_PLAYBACK_STOPPED);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(playbackStoppedReceiver, playbackIntentFilter);

    }


    /* Return a writeable sub-directory from external storage  */
    private File getCollectionDirectory(String subDirectory) {
        File[] storage = mActivity.getExternalFilesDirs(subDirectory);
        for (File file : storage) {
            String state = EnvironmentCompat.getStorageState(file);
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                Log.i(LOG_TAG, "External storage: " + file.toString());
                return file;
            }
        }
        Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_no_external_storage), Toast.LENGTH_LONG).show();
        Log.e(LOG_TAG, "Unable to access external storage.");
        // finish activity
        mActivity.finish();

        return null;
    }



}