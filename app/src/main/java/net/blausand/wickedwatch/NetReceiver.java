package net.blausand.wickedwatch;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;

import net.blausand.wickedwatch.helpers.LogHelper;

import java.io.File;
import java.io.IOException;


public class NetReceiver {

    /* Define log tag */
    private static final String LOG_TAG = NetReceiver.class.getSimpleName();

    //private ArrayList<Sound> mSounds = null;
    //public Context context;
    //private SoundAdapter mAdapter = null;
    // create MediaSource
    private MediaSource mediaSource;

    public NetReceiver() {
        //mnb: inherit a cntext. Just guessing so that we have something for mMediaplayer.create()
        //context = ;
        LogHelper.d(LOG_TAG,"++++ Constructor of NetReceiver. Why never going into onCreate() and OSClistener?");
    }

    /*
    public int[] downloadAudioSet(){

        //int free =
        File appDir = Environment.getDataDirectory();
        //mkdir, "media");
        openFileOutput(appDir, int)
        return new int[]{0};
    }
    */

    public ExtractorMediaSource setupOSC(Context context, SimpleExoPlayer thePlayer) {

        Uri uri = Uri.fromFile(new File("/storage/emulated/0/Music/audio.mp3"));   //RawResourceDataSource.buildRawResourceUri(R.raw.snd);


        DataSpec dataSpec = new DataSpec(uri);
        LogHelper.d(LOG_TAG, "++++ Setting up OSC with ++++"+ dataSpec.toString());
        final FileDataSource fileDataSource = new FileDataSource();
        try {
            fileDataSource.open(dataSpec);
        } catch (FileDataSource.FileDataSourceException e) {
            e.printStackTrace();
        }

        DataSource.Factory factory = new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return fileDataSource;
            }
        };

        ExtractorMediaSource mediaSource =
                new ExtractorMediaSource.Factory(
                new DefaultDataSourceFactory(context, "tzu")).
                        createMediaSource(fileDataSource.getUri());
          //      (uri, DataSource.Factory { dataSource }, Mp3Extractor.FACTORY, null, null) //buildMediaSource(uri);
        LogHelper.d(LOG_TAG, "++++ Created MediaSource: ++++"+mediaSource.toString());

//        mediaSource.prepareSource(thePlayer,false,null);
        LogHelper.d(LOG_TAG, "++++ Prepared Source ++++");

        try {
            OSCPortIn receiver = new OSCPortIn(OSCPortIn.defaultSCOSCPort());

            receiver.addListener("/load_samples", new OSCListener() {
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    Object[] arguments = message.getArguments();

                    String url = (String)arguments[0];

                    //samples = loadSamples(loadStringFromUrl(url));

                    LogHelper.d(LOG_TAG,"Finished loading!");
                }
            });
            receiver.addListener("/play", new OSCListener() {
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    Object[] arguments = message.getArguments();

                    String name = (String)arguments[0];
                    Float velocity = (Float)arguments[1];
                    Float panning = (Float)arguments[2];

                    LogHelper.d(LOG_TAG,name);

//                    mMediaPlayer.seekTo(0); //create(context, R.raw.snd);
//                    mMediaPlayer.start();
                    //play(name, velocity, panning);
                }
            });
            receiver.startListening();

            return mediaSource;
        } catch(Exception e) {
            return null;
        }

    }
}
