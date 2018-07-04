package net.blausand.wickedwatch.core;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;

public final class Wicked implements Parcelable {

    /* Define log tag */
    private static final String LOG_TAG = Wicked.class.getSimpleName();


    /* Main class variables */
    public int mLevel;
    public int mScore;

    private int mCurrentCommandId;

    public String mCurrentCommand;

    public String mNetworkId;
    public String mNick;
    public int mAge;

    public Wicked() {
        mLevel = 1;
        mScore = 0;
        mAge = 18;
        mNick = "";
        mCurrentCommandId = 0;
        mCurrentCommand = "";
        mNetworkId = "";
    }

    public Wicked (int level, int score){
        this();
        mLevel = level;
        mScore = score;
    }

    /*** Getter ***//*
    public int getLevel() { return mLevel; }

    public int getScore() {
        return mScore;
    }

    public int getCurrentCommandId() {
        return mCurrentCommandId;
    }

    public String getCurrentCommand() {
        return mCurrentCommand;
    }

    public String getNetworkId() {
        return mNetworkId;
    }

    *//*** Setter ***//*
    public void setLevel(int level) {
        this.mLevel = level;
    }

    public void setScore(int score) {
        this.mScore = score;
    }

    public void setmCurrentCommandId(int commandId) {
        this.mCurrentCommandId = commandId;
    }

    public void setCurrentCommand(String command) {
        this.mCurrentCommand = command;
    }*/

    public void setNetworkId(String networkId) {
        this.mNetworkId = networkId;
    }

    /*** Implementations for iParcelable ***/
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mLevel);
        dest.writeInt(mScore);
        dest.writeString(mNetworkId);
        dest.writeInt(mCurrentCommandId);
    }

    protected Wicked(Parcel in) {
        mLevel = in.readInt();
        mScore = in.readInt();
        mCurrentCommandId = in.readInt();
        mCurrentCommand = in.readString();
        mNetworkId = in.readString();
    }

    public static final Creator<Wicked> CREATOR = new Creator<Wicked>() {
        @Override
        public Wicked createFromParcel(Parcel in) {
            return new Wicked(in);
        }

        @Override
        public Wicked[] newArray(int size) {
            return new Wicked[size];
        }
    };

//    public String getNick() { return mNick; }
}
