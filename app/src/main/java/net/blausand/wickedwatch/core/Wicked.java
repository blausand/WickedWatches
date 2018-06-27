package net.blausand.wickedwatch.core;

public final class Wicked {

    /* Define log tag */
    private static final String LOG_TAG = Wicked.class.getSimpleName();


    /* Main class variables */
    private int mLevel;
    private int mScore;
    private String mCurrentCommand;
    private String mNetworkId;

    public Wicked() {
        mLevel = 1;
        mScore = 0;
        mCurrentCommand = "";
        mNetworkId = "";
    }

    /* Getter for Level */
    public int getLevel() { return mLevel; }

    /* Getter for Score */
    public int getScore() {
        return mScore;
    }

    /* Getter for name of station */
    public String getCurrentCommand() {
        return mCurrentCommand;
    }

    public void setLevel(int level) {
        this.mLevel = level;
    }

    public void setScore(int score) {
        this.mScore = score;
    }

    public void setCurrentCommand(String command) {
        this.mCurrentCommand = command;
    }

    public void setNetworkId(String networkId) {
        this.mNetworkId = networkId;
    }

}
