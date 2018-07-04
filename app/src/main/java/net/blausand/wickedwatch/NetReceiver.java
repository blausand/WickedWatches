package net.blausand.wickedwatch;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

/*import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;*/
import com.danikula.videocache.HttpProxyCacheServer;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;

import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

//import net.blausand.wickedwatch.helpers.CacheDataSource;
import net.blausand.wickedwatch.core.Wicked;
import net.blausand.wickedwatch.helpers.LogHelper;
import net.blausand.wickedwatch.helpers.TransistorKeys;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

//import static junit.framework.Assert.assertTrue;


public class NetReceiver extends AsyncTask<String, Void, Void> implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = NetReceiver.class.getSimpleName();

    private Context mContext;
    private InetAddress mHostname;
//    private String ;
    //private SoundAdapter mAdapter = null;

    private static HashMap<String, MediaSource> mSamples = new HashMap<String, MediaSource>();
    //FileOutputStream outputStream;

    public SimpleExoPlayer samplePlayer;
    public HttpProxyCacheServer mProxy;
    private OSCPortIn receiver;
    private OSCPortOut sender;
    private Wicked mWicked;
    public InetAddress mGameServer;
    public InetAddress mOurIp;
    //InetAddress server = InetAddress.getByName("M0P3D");

    public NetReceiver() {
    }

    public NetReceiver(Context c, HttpProxyCacheServer proxy, SimpleExoPlayer player, Wicked wicked) {
        samplePlayer = player;
        mContext = c;
        mProxy = proxy;
        mWicked = wicked;


        LogHelper.d(LOG_TAG, "++++ Netreceiver-constructor: Entering addPlayerListeners() ++++");
        addPlayerListeners();
        // excluded from here: CashZeux.java
    }

    @Override
    protected Void doInBackground(String... Ips) {

        //see if the server is available:
        try {
            mGameServer = InetAddress.getByName(Ips[0]);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //find our own IP:
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress.hashCode() >> 24 & 0xff) == 10) { //dirty: choose the one with IP in subnet 10.255.255.255
                        String ip = inetAddress.getHostAddress();
                        LogHelper.i(LOG_TAG, "Our IP is " + ip);
                        mOurIp = inetAddress;
                        mWicked.mNetworkId = ip;
                    }
                }
            }
        } catch (SocketException ex) {
            LogHelper.e(LOG_TAG, ex.toString());
        }
        return null;
    }

    public void onPostExecute(Void v) {
        setupOSCIn();
        setupOSCout();
    }

    public void setupOSCIn () {
        LogHelper.d(LOG_TAG, "++++ Setting up OSC for: "+ mWicked.mNetworkId);

        try {
            receiver = new OSCPortIn(OSCPortIn.defaultSCOSCPort());

            receiver.addListener("/load_samples", new OSCListener() {
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    Object[] arguments = message.getArguments();
                    ArrayList<String> urls = new ArrayList<String>();
                    for(Object arg : arguments){
                        urls.add((String) arg);
                    }
                    /*mIsSamplesLoaded =1*/
                    loadSamples(urls);
                }
            });
            receiver.addListener("/play", new OSCListener() {
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    Object[] arguments = message.getArguments();
                    //TODO: Make this safe, please.
                    if (arguments.length < 3){
                    }
                    String name = (String)arguments[0];
                    Float velocity = (Float)arguments[1];
                    Float panning = (Float)arguments[2];

                    mWicked.mCurrentCommand = name;
                    //MNB: send local broadcast
                    Intent i = new Intent();
                    i.setAction(ACTION_MESSAGE_CHANGED);
                    i.putExtra(EXTRA_WICKED, mWicked);
                    LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(i);

                    fireSample(name, velocity, panning);
                }
            });

            receiver.addListener("/score", new OSCListener() {
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    Object[] arguments = message.getArguments();
                    //TODO: Make this safe, please.
                    mWicked.mLevel = (int)arguments[0];
                    mWicked.mScore = (int)arguments[1];
                    LogHelper.i(LOG_TAG,"Level: "+mWicked.mLevel+" Score: " + mWicked.mScore);

                    //MNB: NO CLUE IF THIS MAKES SENSE AT ALL:  send local broadcast
                    Intent i = new Intent();
                    i.setAction(ACTION_SCORE_CHANGED);
                    i.putExtra(EXTRA_WICKED, mWicked);
                    LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(i);
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

    }

    private void setupOSCout() {
        try {
            sender = new OSCPortOut(mGameServer, 55555); //todo: use settings
            loginClient();
        } catch (Exception e) {
            LogHelper.e(LOG_TAG, "++++ Couldn't setup OSC Out :( ++++\n" + e.toString());
        }
    }

    /****** OSC sender functions ******/

    private void loginClient() { //todo: initial r egisterClient()

        //export this later
        Object arguments[] = new Object[3];
        arguments[0] = mWicked.mNetworkId;
        arguments[1] = mWicked.mNick;
        arguments[2] = mWicked.mAge;

        OSCMessage msg = new OSCMessage("/register", arguments);
        try {
            sender.send(msg);
        } catch(Exception e) {
            Toast.makeText(mContext,"Registration with GameServer failed :(",Toast.LENGTH_LONG);
            LogHelper.e(LOG_TAG, "Registration with GameServer failed :( \n"+ e.toString());
        } //todo: Check Network and try again
    }

    /* mnb make this general sendOSC with more params*/
    private void sendError(String err) {
        //export this later
        Toast.makeText(mContext,"Leider ist jetzt ein Fehler passiert :(\n" + err,Toast.LENGTH_LONG);
        Object arguments[] = new Object[3];
        arguments[0] = mWicked.mNetworkId;
        arguments[1] = err;

        OSCMessage msg = new OSCMessage("/error", arguments);
        try {
            sender.send(msg);
        } catch(Exception e) {
            LogHelper.e(LOG_TAG, "Sending Error via OSC failed :( \n"+ e.toString());
        }
    }

    private void loadSamples(ArrayList<String> sampleNames) {

//        File appDir = Environment.getDataDirectory();

        MediaSource sample;
        //mnb: Maybe i can recycle the factories (since that's why we call them factories)?
        DefaultDataSourceFactory factory = new DefaultDataSourceFactory(mContext, "CommandSampler");
        ExtractorMediaSource.Factory mediaSourceFactory = new ExtractorMediaSource.Factory(factory);

        int perc = 0;
        if (mSamples == null) {
            mSamples = new HashMap<String, MediaSource>();
        }
        Uri uri;
        final String url = "http://elon/";

        samplePlayer.setPlayWhenReady(false);
        for (String smplname : sampleNames) {
            //TODO: Handle publishProgress() and isCancelled() here!
            if (!mSamples.containsKey(smplname)) { //skip existing samples here

                //mnb: Danikula VideoCache
                String proxyUrl = mProxy.getProxyUrl(url+"wickedSounds/"+smplname+".mp3");
                uri = Uri.parse(proxyUrl);

                sample = mediaSourceFactory.createMediaSource(uri);

                if (sample!=null) {
                    //make it download:
                    LogHelper.d(LOG_TAG,"++ downloading sample: "+smplname);
                    perc = 0;
                    samplePlayer.prepare(sample);
//              while (perc < 95){
//                    perc = samplePlayer.getBufferedPercentage();
//                LogHelper.d("Is loading:" + //samplePlayer.isLoading()); //perc+"%");
//              }

                    LogHelper.d(LOG_TAG,"++ done.");

                    mSamples.put(smplname, sample);
                    sample = null;
                } else {
                    LogHelper.e(LOG_TAG,"++++ Failed sample download: "+smplname);
                }
            }

        }
        LogHelper.d(LOG_TAG,"++++ Added "+mSamples.size()+" samples :)");
        //TODO: Load text file for display.
        DownloadFiles();

    }

    public void DownloadFiles(){
        try {
            URL u = new URL("http://elon/ansagen_master_02.xml");
            InputStream is = u.openStream();

            DataInputStream dis = new DataInputStream(is);

            byte[] buffer = new byte[1024];
            int length;

            FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + "/Android/data/net.blausand.wickedwatch/files/ansagen.xml"));
            while ((length = dis.read(buffer))>0) {
                fos.write(buffer, 0, length);
            }
            fos.flush();
            fos.close();

        } catch (MalformedURLException mue) {
            LogHelper.e(LOG_TAG,"SYNC getUpdate | malformed url error:\n" + mue);
        } catch (IOException ioe) {
            LogHelper.e(LOG_TAG,"SYNC getUpdate | io error:\n"+ ioe);
        } catch (SecurityException se) {
            LogHelper.e(LOG_TAG,"SYNC getUpdate | security error:\n"+ se);
        }
    }


    public void fireSample(String name, float velocity, float panning) {
        if (!mSamples.containsKey(name)){
            sendError("Schlimm! Anweisung konnte nicht abgespielt werden: " + name);
            return;
        }
        MediaSource sampl = mSamples.get(name); //sehr interessant: Wird nur bei ungepufferten Audios ausgeführt! Kein Break bei gepufferten.
        if (sampl==null){
            LogHelper.e(LOG_TAG,"++++ sample is null ++++");
            return;
        }
        LogHelper.d(LOG_TAG,"++++ samplePlayer: "+name);

        samplePlayer.prepare(sampl);
        samplePlayer.setVolume(velocity);
//        samplePlayer.setPlaybackParameters(pbP);
        samplePlayer.setPlayWhenReady(true);

    }

    private void addPlayerListeners() {

        samplePlayer.addListener(new Player.DefaultEventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {}

            @Override
            public void onLoadingChanged(boolean isLoading) {

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case Player.STATE_READY:
                        LogHelper.d(LOG_TAG,"onPlayerStateChanged: Playing…");
                        return;
                    case Player.STATE_ENDED:
                        LogHelper.d(LOG_TAG,"onPlayerStateChanged: Ended.");
                        return;
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {

            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {

            }

            @Override
            public void onPositionDiscontinuity(int reason) {

            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
                LogHelper.d(LOG_TAG,"++++ onPlaybackParametersChanged: "+ playbackParameters.toString());
            }

            @Override
            public void onSeekProcessed() {

            }
        });
    }

}
