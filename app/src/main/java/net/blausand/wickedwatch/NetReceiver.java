package net.blausand.wickedwatch;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;

import net.blausand.wickedwatch.helpers.LogHelper;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NetReceiver {

    /* Define log tag */
    private static final String LOG_TAG = NetReceiver.class.getSimpleName();

    //private ArrayList<Sound> mSounds = null;
    private Context mContext;
    //private SoundAdapter mAdapter = null;

    private String[] names;
    private static HashMap<String, MediaSource> mSamples;
    //FileOutputStream outputStream;

    private MediaSource sample;
    public SimpleExoPlayer samplePlayer;

    public NetReceiver() {
    }

    public void initSampler(Context c, SimpleExoPlayer player) {
        samplePlayer = player;
        mContext = c;

        Uri uri = Uri.fromFile(new File("/storage/emulated/0/Music/audio.mp3"));   //RawResourceDataSource.buildRawResourceUri(R.raw.snd);
        DataSpec dataSpec = new DataSpec(uri);
        final FileDataSource fileDataSource = new FileDataSource();

        try {
            fileDataSource.open(dataSpec);
        } catch (FileDataSource.FileDataSourceException e) {
            e.printStackTrace();
        }

        /*DataSource.Factory factory = new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return fileDataSource;
            }
        };*/


        sample = new ExtractorMediaSource.Factory(
                        new DefaultDataSourceFactory(mContext, "CommandSampler")).
                        createMediaSource(fileDataSource.getUri());
        //      (uri, DataSource.Factory { dataSource }, Mp3Extractor.FACTORY, null, null) //buildMediaSource(uri);
        LogHelper.d(LOG_TAG, "++++ Created MediaSource: ++++"+sample.toString());
        samplePlayer.prepare(sample);
        //.prepareSource(samplePlayer, false, null);
        LogHelper.d(LOG_TAG, "++++ Prepared Source for: ++++"+samplePlayer.toString());
        loadSamples();
    }

    public boolean setupOSC() {

        LogHelper.d(LOG_TAG, "++++ Setting up OSC ++++");

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

                    fireSample(name, velocity, panning);

                }
            });
            receiver.startListening();

            return true;
        } catch(Exception e) {
            return false;
        }

    }

    private void loadSamples() {

        File appDir = Environment.getDataDirectory();
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(mContext);
        final String url ="http://elon/";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url+"sounds.txt",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        LogHelper.d(LOG_TAG,"Response is: "+ response);
                        names = response.split("\n");
                        LogHelper.d(LOG_TAG,"First is: " + names.length); //[0]);

                        MediaSource sample;

                        mSamples = new HashMap<String, MediaSource>();
                        Uri uri;

                        for(int i = 0; i < names.length; i++) {

//         Request<String> req = new Request<String>();
//         InputStream reader = Url.openStream();
                            uri = Uri.parse(url+"wickedSounds/"+names[i]);

                            sample = new ExtractorMediaSource.Factory(
                                    new DefaultDataSourceFactory(mContext, "CommandSampler")).
                                    createMediaSource(uri);

                            if (sample!=null) {
                                mSamples.put(names[i], sample);
                                sample = null;
                            } else {
                                LogHelper.d(LOG_TAG,"++++ Failed sample download: "+names[i]);
                            }

                        }
                        LogHelper.d(LOG_TAG,"++++ Added samples: "+mSamples.keySet().toString());



                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                LogHelper.d(LOG_TAG,"Download didn't work!");
            }
        });

        queue.add(stringRequest);

        //mkdir, "media");
       /* try {
            mContext.openFileOutput(appDir.getName(), Context.MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/
        //JSONObject paths = (JSONObject)JSONSerializer.toJSON(json);

    }

    public void fireSample(String name, float velocity, float panning) {
        MediaSource sampl = mSamples.get(name);
        if (sampl==null){
            LogHelper.d(LOG_TAG,"++++ sample is null ++++");
        }
        LogHelper.d(LOG_TAG,"++++ samplePlayer: "+sampl.toString());

        samplePlayer.prepare(sampl);
//        samplePlayer.setPlaybackParameters(pbP);
        samplePlayer.setPlayWhenReady(true);

    }

}
