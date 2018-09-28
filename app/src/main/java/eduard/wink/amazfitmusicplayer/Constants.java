package eduard.wink.amazfitmusicplayer;

import android.os.Environment;

/**
 * Created by Eddy on 26.09.2018.
 */

public class Constants {

    public static final String PARENT_DIR = Environment.getExternalStorageDirectory()+"/Music/";
    public static int MAX_VOLUME = 30;
    public static int TIME_BETWEEN_SONG = 100;
    public static int PLAYMODE_DEFAULT = 0;
    public static int PLAYMODE_REPEAT_ALL = 1;
    public static int PLAYMODE_REPEAT_ONE = 2;
    public static int PLAYMODE_RANDOM = 3;
    public static int[] PLAYMODE_LIST = {PLAYMODE_DEFAULT, PLAYMODE_REPEAT_ALL, PLAYMODE_REPEAT_ONE, PLAYMODE_RANDOM};
    public static String SAVE_MY_PREF = "myPref";
    public static String SAVE_LAST_PLAYLIST = "playlist";
    public static String SAVE_LAST_SONG_ID = "lastSong";
    public static String SAVE_LAST_PLAYMODE = "playmode";
    public static String SAVE_LAST_POSITION = "lastPosition";
    public static String SAVE_LAST_VOLUME = "lastVolume";

    public static String ALL_SONGS = "ALL";
    public static String CLOSE_FOLDER_LIST = ">> CLOSE <<";
    public static String NO_MUSIC_FILES_EXISTS = "NO MUSIC EXISTS";

    //After this time without playing music the headset buttons won't work - to safe battery
    //You have to open the app again and press a button (i.e play) in the app
    public static final int SERVICE_TIMEOUT = 300*1000;//5 Minutes

    public static final String HEADSET_NEXT = "headset_next_btn";
    public static final String HEADSET_PREV = "headset_prev_btn";
    public static final String HEADSET_PLAY = "headset_play_btn";
    public static final String HEADSET_PAUSE = "headset_pause_btn";
    public static final String HEADSET_STOP = "headset_stop_btn";
    public static final String HEADSET_VOL_UP = "headset_vol_up";
    public static final String HEADSET_VOL_DOWN = "headset_vol_down";

    //If you want to enable 4x tap on your apple AirPods to set the volume
    public static final boolean AIRPODS_QUAD_VOL = false;
    public static final int AIRPODS_QUAD_DELAY = 600;
    //Airpods defined left touch - this will set the volume down (must be an array)
    public static final String[] AIRPODS_LEFT = {HEADSET_PAUSE, HEADSET_PLAY};
    //Airpods defined right touch - this will set the volume up (must be an array)
    public static final String[] AIRPODS_RIGHT = {HEADSET_NEXT};


}
