package net.blausand.wickedwatch;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

/*import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;*/
import com.danikula.videocache.HttpProxyCacheServer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;

import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

//import net.blausand.wickedwatch.helpers.CacheDataSource;
import net.blausand.wickedwatch.core.Wicked;
import net.blausand.wickedwatch.helpers.LogHelper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

//import static junit.framework.Assert.assertTrue;


public class NetReceiver extends AsyncTask<String, Void, InetAddress> {

    /* Define log tag */
    private static final String LOG_TAG = NetReceiver.class.getSimpleName();

    private Context mContext;
    private InetAddress mHostname;
//    private String mNetworkId;
    //private SoundAdapter mAdapter = null;

    private static HashMap<String, MediaSource> mSamples;
    //FileOutputStream outputStream;

    public SimpleExoPlayer samplePlayer;
    public HttpProxyCacheServer mProxy;
    private OSCPortIn receiver;
    private OSCPortOut sender;
    private Wicked mWicked;
    private InetAddress mGameServer;
    //InetAddress server = InetAddress.getByName("M0P3D");

    public NetReceiver() {
    }

    public NetReceiver(Context c, HttpProxyCacheServer proxy, SimpleExoPlayer player, Wicked wicked) {
        samplePlayer = player;
        mContext = c;
        mProxy = proxy;
        mWicked = wicked;
        LogHelper.d(LOG_TAG, "++++ Entering doinBackgroudnd from Netreceiver-constructor ++++");
//        mSamplesLoaded = null;
        // excluded from here: CashZeux.java
    }

    @Override
    protected InetAddress doInBackground(String... Ips) {
        try {
            mGameServer = InetAddress.getByName(Ips[0]);
            mHostname = InetAddress.getByName("localhost");
            return mHostname;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void onPostExecute(InetAddress hostname) {

//        mHostname = hostname;
        LogHelper.d(LOG_TAG, "++++ Setting up OSC for: "+ mHostname.getHostAddress());

        try {
            receiver = new OSCPortIn(OSCPortIn.defaultSCOSCPort());

            receiver.addListener("/load_samples", new OSCListener() {
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    Object[] arguments = message.getArguments();
                    ArrayList<String> urls = new ArrayList<String>();
                    for(Object arg : arguments){
                        urls.add((String) arg);
                    }
                    /*mSamplesLoaded =*/
                    loadSamples(urls);
                }
            });
            receiver.addListener("/play", new OSCListener() {
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    Object[] arguments = message.getArguments();
                    //TODO: Make this safe, please.
                    String name = (String)arguments[0];
                    Float velocity = (Float)arguments[1];
                    Float panning = (Float)arguments[2];

                    LogHelper.d(LOG_TAG,name);

                    fireSample(name, velocity, panning);
                }
            });

            receiver.addListener("/score", new OSCListener() {
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    Object[] arguments = message.getArguments();
                    //TODO: Make this safe, please.
                    mWicked.setLevel((int)arguments[0]);
                    mWicked.setScore((int)arguments[1]);

                    LogHelper.i(LOG_TAG,"Level: "+mWicked.getLevel()+" Score: " + mWicked.getScore());
                }
            });

            receiver.addListener("/server_ip", new OSCListener() {
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    Object[] arguments = message.getArguments();
                    //TODO: Make this safe, please.
                    String serverIp = (String)arguments[0];
                    try {
                        mGameServer = InetAddress.getByName(serverIp);
                        setupOSCout();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    LogHelper.i(LOG_TAG,"new GameServer Address: "+ mGameServer.toString());
                }
            });
            receiver.startListening();

        } catch(Exception e) {
            LogHelper.e(LOG_TAG, "++++ Couldn't setup OSC In :( ++++\n"+ e.getMessage());
        }
        //moved out SetupOSCout

    }

    private void setupOSCout() {
        try {
            sender = new OSCPortOut(mGameServer, 55555); //todo: use settings
            registerClient();

        } catch (Exception e) {
            LogHelper.e(LOG_TAG, "++++ Couldn't setup OSC Out :( ++++\n" + e.toString());
        }
    }

    private void registerClient() {
        //export this later
        Object arguments[] = new Object[3];
        arguments[0] = mWicked.mNetworkId;
        arguments[1] = "Lieselotte";
        arguments[2] = 42;

        OSCMessage msg = new OSCMessage("/register", arguments);
        try {
            sender.send(msg);
        } catch(Exception e) {
            Toast.makeText(mContext,"Registration with GameServer failed :(",Toast.LENGTH_LONG);
            LogHelper.e(LOG_TAG, "Registration with GameServer failed :( \n"+ e.toString());
        }
    }

    private void loadSamples(ArrayList<String> sampleNames) {

//        File appDir = Environment.getDataDirectory();

        MediaSource sample;
        //mnb: Maybe i can recycle the factories (since that's why we call them factories)?
        DefaultDataSourceFactory factory = new DefaultDataSourceFactory(mContext, "CommandSampler");
        ExtractorMediaSource.Factory mediaSourceFactory = new ExtractorMediaSource.Factory(factory);

        mSamples = new HashMap<String, MediaSource>();
        Uri uri;
        final String url = "http://elon/";

        for (String smplname : sampleNames) {
            //TODO: Handle publishProgress() and isCancelled() here!

            //mnb: Danikula VideoCache
            String proxyUrl = mProxy.getProxyUrl(url+"wickedSounds/"+smplname);
            uri = Uri.parse(proxyUrl);

            sample = mediaSourceFactory.createMediaSource(uri);

            if (sample!=null) {
                //make it download:
                LogHelper.d(LOG_TAG,"++ downloading sample: "+smplname);
                samplePlayer.prepare(sample);
                LogHelper.d(LOG_TAG,"++ done.");

                mSamples.put(smplname, sample);
                sample = null;
            } else {
                LogHelper.e(LOG_TAG,"++++ Failed sample download: "+smplname);
            }

        }
        LogHelper.d(LOG_TAG,"++++ Added "+mSamples.size()+" samples :)");

    }

    public void fireSample(String name, float velocity, float panning) {
        MediaSource sampl = mSamples.get(name);
        if (sampl==null){
            LogHelper.e(LOG_TAG,"++++ sample is null ++++");
            return;
        }
        LogHelper.d(LOG_TAG,"++++ samplePlayer: "+sampl.toString());

        samplePlayer.prepare(sampl);
        samplePlayer.setVolume(velocity);
//        samplePlayer.setPlaybackParameters(pbP);
        samplePlayer.setPlayWhenReady(true);

    }


}
