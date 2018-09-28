package eduard.wink.amazfitmusicplayer;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Eddy on 26.09.2018.
 */

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaButtonIntentReceiver.Callbacks {

    private static final String TAG = "MediaPlayerService";

    private final IBinder mBinder = new LocalBinder();
    MediaPlayer mediaPlayer;
    //Timer handles automatically play next song
    Timer timer;
    //Handler checks for Timeout
    Handler handler = null;
    //Timer for AirPods 4x Tap
    Timer airPodTimer;
    //Callback for MainActivity
    Callbacks activity = null;


    MediaButtonIntentReceiver mMediaButtonReceiver = null;
    private static MediaButtonIntentReceiver.Callbacks mediaButtonsCallback;

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    //Static variables which MainActivity reads
    private static boolean isRunning = false;
    private static boolean isMusicPlaying = false;
    private static int currentVolume;
    private static String currentSongName = "";


    Boolean isTimerRunning;
    int currentSongId;
    int playmode;
    List<File> playlist;

    //For Airpods 4x Tap
    private String lastHeadphonesTouch = "";


    /*
    * Check if headset disconnect - if so pause the player and service
     */
    private BroadcastReceiver mNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Headset disconnects");
            if( mediaPlayer.isPlaying() ) {
                pauseSong();
                stopMediaPlayerService();
            }
        }
    };



    /*
    *
    * Override and listener methods
    *
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onCreate() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

        sharedPref= getSharedPreferences(Constants.SAVE_MY_PREF, Context.MODE_PRIVATE);
        editor=sharedPref.edit();

        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mNoisyReceiver, filter);

        if (Constants.AIRPODS_QUAD_VOL) {
            airPodTimer = new Timer();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;

        restartInactivityTimer();

        initVolume();

        initMediaButtonIntentReceiver();

        initLastSong();


        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopMediaPlayerService();
        pauseSong();
        try {
            unregisterReceiver(mNoisyReceiver);
        } catch (IllegalArgumentException e) {
            //e.printStackTrace();
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.release();
    }

    public void onCompletion(MediaPlayer _mediaPlayer) {
        stopSelf();
    }

    //returns the instance of the service
    public class LocalBinder extends Binder {
        public MediaPlayerService getServiceInstance(){
            return MediaPlayerService.this;
        }
    }

    public boolean checkForInactivity() {
        if (isMusicPlaying) {
            return false;
        } else {
            Log.i(TAG, "stopped (timeout)");
            stopMediaPlayerService();
            return true;
        }
    }

    public void restartInactivityTimer() {
        if (handler == null) {
            handler = new Handler();
        } else {
            handler.removeCallbacksAndMessages(null);
        }

        //If there is no music playing for SERVICE_TIMEOUT seconds the service will stop
        final Runnable r = new Runnable() {
            public void run() {
                if (!checkForInactivity()) {
                    handler.postDelayed(this, Constants.SERVICE_TIMEOUT);
                }
            }
        };

        handler.postDelayed(r, Constants.SERVICE_TIMEOUT);
    }

    //This Timer is for playing automatically next Song when current is over
    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            isTimerRunning = false;
        }
    }

    //This Timer is for playing automatically next Song when current is over
    private void newTimer() {
        timer = new Timer();
        isTimerRunning = true;
    }
    /*
    * Override and listener methods end
     */













    /*
    *
    * Init methods
    *
     */
    public void initVolume() {
        int lastVol = sharedPref.getInt(Constants.SAVE_LAST_VOLUME, 3);
        currentVolume = lastVol;
        setVolume(currentVolume);
    }


    public void initMediaButtonIntentReceiver() {
        if (mMediaButtonReceiver == null) {
            mMediaButtonReceiver = new MediaButtonIntentReceiver();
            IntentFilter mediaFilter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
            registerReceiver(mMediaButtonReceiver, mediaFilter);
            mediaButtonsCallback = this;
            mMediaButtonReceiver.registerClient(mediaButtonsCallback);
        }
    }


    public void initLastSong() {
        String lastPlaylist = sharedPref.getString(Constants.SAVE_LAST_PLAYLIST, "all");
        int lastPlaymode = sharedPref.getInt(Constants.SAVE_LAST_PLAYMODE, Constants.PLAYMODE_DEFAULT);
        int lastSong = sharedPref.getInt(Constants.SAVE_LAST_SONG_ID, 0);

        changePlaymode(lastPlaymode);

        handlePlaylistChange(lastPlaylist);

        if (playlist.size() > lastSong) {
            currentSongId = lastSong;
        } else {
            currentSongId = 0;
        }

        if (playlist.size() <= currentSongId) {
            currentSongName = Constants.NO_MUSIC_FILES_EXISTS;
            currentSongId = -1;
        } else {
            currentSongName = playlist.get(currentSongId).getName();
        }
    }
    /*
    * Init methods end
     */








    /*
    *
    * SONG PLAYER HELPER FUNCTIONS
    *
     */
    public void stopMediaPlayerService() {
        cancelTimer();
        isRunning = false;
        try {
            unregisterReceiver(mMediaButtonReceiver);
        } catch (IllegalArgumentException e) {
            //e.printStackTrace();
        }
        mMediaButtonReceiver = null;
        stopSelf();

    }


    //Try to set the playlist - if folder not exist it will play all songs
    public void handlePlaylistChange(String directory) {
        if (directory.toLowerCase().equals("all")) {
            playlist = getAllSongsInDirectory(new File(Constants.PARENT_DIR));
        } else {
            File lastDir = new File(Constants.PARENT_DIR+directory);
            if (lastDir.exists()) {
                playlist = getAllSongsInDirectory(lastDir);
            } else {
                playlist = getAllSongsInDirectory(new File(Constants.PARENT_DIR));
            }
        }
    }


    //Change or restart a playlist
    public void changePlaylist(String directory) {
        handlePlaylistChange(directory);
        // Save your string in SharedPref
        editor.putString(Constants.SAVE_LAST_PLAYLIST, directory);
        editor.commit();

        currentSongId = 0;
        playSong(currentSongId, 0);
    }


    public void changePlaymode(int newMode) {
        editor.putInt(Constants.SAVE_LAST_PLAYMODE, newMode);
        editor.commit();
        playmode = newMode;
        if (mediaPlayer.isPlaying()) {
            checkForNextSong();
        }
    }

    //This method will start playing the given song
    private void playSong(int songId, int seek) {
        if (!playlist.isEmpty()) {
            if (playlist.size() <= songId) {
                songId = 0;
            }

            editor.putInt(Constants.SAVE_LAST_SONG_ID, songId);
            editor.commit();

            currentSongName = playlist.get(songId).getName();
            File song = playlist.get(songId);
            isMusicPlaying = true;
            mediaPlayer.reset();
            try {
                mediaPlayer.setDataSource(song.getPath());
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mediaPlayer.seekTo(seek);

            mediaPlayer.start();
            checkForNextSong();

            sendCallbackToActivity();
        } else {
            Log.e(TAG, "Folder/Playlist is empty");
        }
    }

    public void pauseSong() {
        if (isMusicPlaying) {
            editor.putInt(Constants.SAVE_LAST_POSITION, mediaPlayer.getCurrentPosition());
            editor.commit();
            cancelTimer();
            isMusicPlaying = false;
            mediaPlayer.pause();
            sendCallbackToActivity();
        }
    }

    public void resumeSong() {
        if (!isMusicPlaying) {
            int lastPos = sharedPref.getInt(Constants.SAVE_LAST_POSITION, 0);
            int lastSong = sharedPref.getInt(Constants.SAVE_LAST_SONG_ID, 0);
            playSong(lastSong, lastPos);
        }
    }

    public void setVolume(int volume) {
        if (volume < 0) {
            volume = 0;
        } else if (volume > Constants.MAX_VOLUME) {
            volume = Constants.MAX_VOLUME;
        }
        currentVolume = volume;
        editor.putInt(Constants.SAVE_LAST_VOLUME, currentVolume);
        editor.commit();
        float log1=(float)(Math.log(Constants.MAX_VOLUME-volume)/Math.log(Constants.MAX_VOLUME));
        mediaPlayer.setVolume(1-log1, 1-log1);
        sendCallbackToActivity();
    }


    //returns -1 if the playlist is over or unknown playmode
    private int getNextSong() {
        int nextSong = -1;
        if (playmode == Constants.PLAYMODE_DEFAULT) {
            if (playlist.size() > currentSongId+1) {
                nextSong = currentSongId+1;
            }
        } else if (playmode == Constants.PLAYMODE_REPEAT_ALL) {
            if (playlist.size() > currentSongId+1) {
                nextSong = currentSongId+1;
            } else {
                nextSong = 0;
            }
        } else if (playmode == Constants.PLAYMODE_REPEAT_ONE) {
            nextSong = currentSongId;
        } else if (playmode == Constants.PLAYMODE_RANDOM) {
            nextSong = new Random().nextInt(playlist.size());
        }
        return nextSong;
    }

    private int getPrevSong() {
        int prevSong = 0;
        if (playmode == Constants.PLAYMODE_REPEAT_ONE) {
            prevSong = currentSongId;
        } else if (playmode == Constants.PLAYMODE_RANDOM) {
            prevSong = new Random().nextInt(playlist.size());
        } else {
            if (currentSongId == 0) {
                prevSong = playlist.size()-1;
            }
            if (currentSongId-1 > 0 ) {
                prevSong = currentSongId-1;
            }
        }
        return prevSong;
    }

    //if playmode is default the mediaplayer will stop after the current song
    private void checkForNextSong() {
        cancelTimer();
        newTimer();
        if (getNextSong() != -1) {
            playNext();
        } else {
            stopPlaylist();
        }
    }

    //play next song if the current one is over
    //Will automatically be called if there is a next song in playlist
    public void playNext() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                currentSongId = getNextSong();
                if (currentSongId < 0) {
                    currentSongId = 0;
                }
                playSong(currentSongId, 0);
            }
        },mediaPlayer.getDuration()-mediaPlayer.getCurrentPosition()+Constants.TIME_BETWEEN_SONG);
    }

    // Playlist is finished (in default mode)
    private void stopPlaylist() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                currentSongId = 0;
                currentSongName = playlist.get(currentSongId).getName();
                mediaPlayer.seekTo(0);
                editor.putInt(Constants.SAVE_LAST_SONG_ID, currentSongId);
                editor.commit();
                pauseSong();
            }
        },mediaPlayer.getDuration()-mediaPlayer.getCurrentPosition());
    }

    //Collects all mp3 files in given folder and it's subfolder
    private List<File> getAllSongsInDirectory(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getAllSongsInDirectory(file));
            } else {
                if(file.getName().endsWith(".mp3")){
                    inFiles.add(file);
                }
            }
        }

        //Sort songs by name
        if (inFiles != null && inFiles.size() > 1) {
            Collections.sort(inFiles, new FileNameComparator());
        }
        return inFiles;
    }
    /*
    * SONG PLAYER HELPER FUNCTIONS END
     */






    /*
    *
    * Buttons methods
    *
     */
    public void nextBtnClicked() {
        restartInactivityTimer();
        if (!playlist.isEmpty()) {
            currentSongId = getNextSong();
            if (currentSongId == -1) {
                currentSongId = 0;
                Log.i(TAG, "restart playlist");
            }
            playSong(currentSongId, 0);
        }
    }

    public void prevBtnClicked() {
        restartInactivityTimer();
        if (!playlist.isEmpty()) {
            currentSongId = getPrevSong();
            playSong(currentSongId, 0);
        }
    }

    public void playBtnClicked() {
        restartInactivityTimer();
        if (isMusicPlaying) {
            pauseSong();
        } else {
            resumeSong();
        }
    }

    public void volUpBtnClicked() {
        setVolume(currentVolume+1);
    }

    public void volDownBtnClicked() {
        setVolume(currentVolume-1);
    }


    @Override
    public void headsetButtonClicked(String btn) {
        if (Constants.AIRPODS_QUAD_VOL) {
            if (Arrays.asList(Constants.AIRPODS_LEFT).contains(btn) || Arrays.asList(Constants.AIRPODS_RIGHT).contains(btn)) {
                checkAirPods4Tap(btn);
            } else {
                handleHeadsetButton(btn);
            }
        } else {
            handleHeadsetButton(btn);
        }
    }

    public void handleHeadsetButton(String btn) {
        if (btn.equals(Constants.HEADSET_NEXT)) {
            nextBtnClicked();
        } else if (btn.equals(Constants.HEADSET_PREV)) {
            prevBtnClicked();
        } else if (btn.equals(Constants.HEADSET_PAUSE) || btn.equals(Constants.HEADSET_STOP)) {
            pauseSong();
        } else if (btn.equals(Constants.HEADSET_PLAY)) {
            resumeSong();
        } else if (btn.equals(Constants.HEADSET_VOL_UP)) {
            volUpBtnClicked();
        } else if (btn.equals(Constants.HEADSET_VOL_DOWN)) {
            volDownBtnClicked();
        }
    }
    /*
    * Buttons methods end
     */


    /*
    * AirPods method
     */
    public void checkAirPods4Tap(String btn) {
        if (btn.equals(lastHeadphonesTouch)) {
            //4x AirPod tap detected
            cancelAirPodTimer();
            if (Arrays.asList(Constants.AIRPODS_LEFT).contains(btn)) {
                volDownBtnClicked();
            } else if (Arrays.asList(Constants.AIRPODS_RIGHT).contains(btn)) {
                volUpBtnClicked();
            }
        } else {
            //It's a fast tap which is not the same as the first one - so the first tap will be ignored
            if (lastHeadphonesTouch != "") {
                cancelAirPodTimer();
            }
            lastHeadphonesTouch = btn;
            airPodTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    //Time for 4x Tap is over do normal double tap action
                    handleHeadsetButton(lastHeadphonesTouch);
                    cancelAirPodTimer();
                }
            }, Constants.AIRPODS_QUAD_DELAY);
        }
    }
    public void cancelAirPodTimer() {
        airPodTimer.cancel();
        lastHeadphonesTouch = "";
        airPodTimer = new Timer();
    }
    /*
    * AirPods method end
     */




    /*
    *
    * Methods for MainActivity
    *
     */
    public static boolean isRunning()
    {
        return isRunning;
    }

    public static boolean isMusicPlaying() {
        return isMusicPlaying;
    }

    public static String getCurrentSongName() {
        return currentSongName;
    }

    public static int getCurrentVolume() { return currentVolume; }

    //Here Activity register to the service as Callbacks client
    public void registerClient(Activity activity){
        this.activity = (Callbacks)activity;
    }

    public void sendCallbackToActivity() {
        if (activity != null) {
            activity.updateClient();
        }
    }


    public interface Callbacks{
        public void updateClient();
    }
    /*
    * Methods for MainActivity end
     */

}
