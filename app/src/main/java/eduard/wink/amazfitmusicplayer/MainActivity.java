package eduard.wink.amazfitmusicplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MediaPlayerService.Callbacks {


    Intent mediaPlayerIntent;
    MediaPlayerService mediaPlayerService;
    int currentPlaymode;
    Handler handler;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    List<String> directories;

    //View / UI variables
    ListView folderList;
    ConstraintLayout constraintVolume;
    Button folderBtn;
    Button playBtn;
    Button nextBtn;
    Button prevBtn;
    Button setVolumeBtn;
    Button volDownBtn;
    Button volUpBtn;
    Button closeVolBtn;
    Button playmodeBtn;
    TextView txtSongName;
    TextView txtVolume;
    Boolean folderListOpen = false;



    /*
    * Need a connection to send data to MediaPlayerService
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mediaPlayerService = binder.getServiceInstance(); //Get instance of your service!
            mediaPlayerService.registerClient(MainActivity.this); //Activity register in the service as client for callabcks!
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };





    /*
    *
    * Override state methods
    *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        sharedPref= getSharedPreferences(Constants.SAVE_MY_PREF, Context.MODE_PRIVATE);
        editor=sharedPref.edit();


        directories = getAllDirectories();


        initView();

        initButtons();

        if (musicExists(new File(Constants.PARENT_DIR))) {
            initFolderList();
        } else {
            handleNoMusicExists();
        }

        currentPlaymode = sharedPref.getInt(Constants.SAVE_LAST_PLAYMODE, Constants.PLAYMODE_DEFAULT);
        updatePlaymodeButton();


        if (MediaPlayerService.isRunning()) {
            bindMediaPlayerService();
        } else {
            startMediaPlayerService();
        }


    }


    @Override
    public void onDestroy() {
        unbindService(mConnection);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        //bindService(mediaPlayerIntent, mConnection, Context.BIND_AUTO_CREATE);
        super.onResume();
    }

    @Override
    public void onPause() {
        //unbindService(mConnection);
        super.onPause();
    }
    /*
    * Override state methods end
     */






    /*
    *
    * Init methods
    *
     */
    private void initView() {
        folderList = (ListView) findViewById(R.id.folder_list);
        folderList.setVisibility(View.GONE);


        txtSongName = (TextView) findViewById(R.id.txt_song);
        txtVolume = (TextView) findViewById(R.id.txtVol);

        constraintVolume = (ConstraintLayout) findViewById(R.id.constraintVolume);
        constraintVolume.setVisibility(View.GONE);
    }

    private void initButtons() {
        folderBtn = (Button) findViewById(R.id.btn_folder);
        playBtn = (Button) findViewById(R.id.btn_play);
        nextBtn = (Button) findViewById(R.id.btn_next);
        prevBtn = (Button) findViewById(R.id.btn_prev);

        setVolumeBtn = (Button) findViewById(R.id.btn_set_volume);
        playmodeBtn = (Button) findViewById(R.id.btn_playmode);
        volDownBtn = (Button) findViewById(R.id.btn_vol_down);
        volUpBtn = (Button) findViewById(R.id.btn_vol_up);
        closeVolBtn = (Button) findViewById(R.id.btn_close_vol);


        String lastFolder = sharedPref.getString(Constants.SAVE_LAST_PLAYLIST, "");
        if (lastFolder != "") {
            File folderFile = new File(Constants.PARENT_DIR+lastFolder);
            if (folderFile.exists()) {
                folderBtn.setText(lastFolder.toUpperCase());
            } else {
                folderBtn.setText(Constants.ALL_SONGS);
            }
        }

        folderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCloseFolderList();
            }
        });

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!MediaPlayerService.isRunning()) {
                    startMediaPlayerService();
                    mediaPlayerService.playBtnClicked();
                } else {
                    mediaPlayerService.playBtnClicked();
                }
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MediaPlayerService.isRunning()) {
                    mediaPlayerService.nextBtnClicked();
                }
            }
        });

        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MediaPlayerService.isRunning()) {
                    mediaPlayerService.prevBtnClicked();
                }
            }
        });


        setVolumeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MediaPlayerService.isRunning()) {
                    updateVolumeLabel();
                    constraintVolume.setVisibility(View.VISIBLE);
                }
            }
        });

        playmodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MediaPlayerService.isRunning()) {
                    changePlaymode();
                    mediaPlayerService.changePlaymode(currentPlaymode);
                }
            }
        });


        volDownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MediaPlayerService.isRunning()) {
                    mediaPlayerService.volDownBtnClicked();
                }
            }
        });

        volUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MediaPlayerService.isRunning()) {
                    mediaPlayerService.volUpBtnClicked();
                }
            }
        });

        closeVolBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                constraintVolume.setVisibility(View.GONE);
            }
        });
    }

    private void initFolderList() {
        final ArrayList<String> directorieStrings = new ArrayList<>();

        //init directory List
        directorieStrings.add(Constants.ALL_SONGS);
        for (String s: directories) {
            directorieStrings.add(s);
        }
        directorieStrings.add(Constants.CLOSE_FOLDER_LIST);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this,
                R.layout.list_white_text, R.id.list_text,
                directorieStrings );

        folderList.setAdapter(arrayAdapter);

        folderList.setSelector(R.drawable.folder_list_item);


        folderList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                if (!directorieStrings.get(i).equals(Constants.CLOSE_FOLDER_LIST)) {
                    if (!MediaPlayerService.isRunning()) {
                        startMediaPlayerService();
                    }
                    changePlaylist(directorieStrings.get(i));
                }
                openCloseFolderList();
            }
        });

        folderBtn.setSelected(true);
    }

    private List<String> getAllDirectories() {
        File parentDir = new File(Constants.PARENT_DIR);
        ArrayList<String> directories = new ArrayList<String>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                if (musicExists(file)) {
                    directories.add(file.getName());
                }
            }
        }

        //sort directories by name
        Collections.sort(directories);

        return directories;
    }

    private boolean musicExists(File parentDir) {
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if(file.getName().endsWith(".mp3")){
                return true;
            } else if (file.isDirectory()) {
                if (musicExists(file)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void handleNoMusicExists() {
        editor.remove(Constants.SAVE_LAST_SONG_ID).commit();
        editor.remove(Constants.SAVE_LAST_PLAYLIST).commit();
        editor.remove(Constants.SAVE_LAST_POSITION).commit();
        playBtn.setEnabled(false);
        nextBtn.setEnabled(false);
        prevBtn.setEnabled(false);
        txtSongName.setText(Constants.NO_MUSIC_FILES_EXISTS);
        folderBtn.setText("NO MUSIC");
        folderBtn.setEnabled(false);
    }
    /*
    * Init methods end
     */






    /*
    *
    * MediaPlayer methods
    *
     */
    public void changePlaymode() {
        if (currentPlaymode + 1 < Constants.PLAYMODE_LIST.length) {
            currentPlaymode = Constants.PLAYMODE_LIST[currentPlaymode+1];
        } else {
            currentPlaymode = Constants.PLAYMODE_LIST[0];
        }

        updatePlaymodeButton();


        mediaPlayerService.changePlaymode(currentPlaymode);
    }


    public void bindMediaPlayerService() {
        mediaPlayerIntent = new Intent(this, MediaPlayerService.class);
        bindService(mediaPlayerIntent, mConnection, Context.BIND_AUTO_CREATE);
        updateSongName();
        updatePlayButton();
    }

    public void startMediaPlayerService() {
        bindMediaPlayerService();
        startService(mediaPlayerIntent);
        updateSongName();
        updatePlayButton();
    }
    /*
    * MediaPlayer methods end
     */







    /*
    *
    * UI Methods
    *
     */
    public void updateSongName() {

            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    String songName = mediaPlayerService.getCurrentSongName();
                    txtSongName.setText(songName);
                    txtSongName.setSelected(true);

                }
            });

            String songName = mediaPlayerService.getCurrentSongName();

            //If the songName isn't loaded yet retry in 1 sec
            if (songName == "") {
                handler = new Handler();

                final Runnable r = new Runnable() {
                    public void run() {
                        updateSongName();
                    }
                };

                handler.postDelayed(r, 1000);
            }
    }

    public void updateVolumeLabel() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                txtVolume.setText(Integer.toString(mediaPlayerService.getCurrentVolume()));

            }
        });

    }

    //will play a playlist from first song
    public void changePlaylist(String newPlaylist) {
        mediaPlayerService.changePlaylist(newPlaylist);

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                String playlist = sharedPref.getString(Constants.SAVE_LAST_PLAYLIST, "");
                folderBtn.setText(playlist.toUpperCase());

            }
        });

    }

    public void updatePlayButton() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                if (mediaPlayerService.isMusicPlaying()) {
                    playBtn.setBackgroundResource(R.drawable.pause_button);
                } else {
                    playBtn.setBackgroundResource(R.drawable.play_button);
                }

            }
        });
    }

    //Playmode will not be updated from service, so no need for "runOnUIiThread
    public void updatePlaymodeButton() {
        if (currentPlaymode == Constants.PLAYMODE_DEFAULT) {
            playmodeBtn.setBackgroundResource(R.drawable.default_button);
        } else if (currentPlaymode == Constants.PLAYMODE_REPEAT_ALL) {
            playmodeBtn.setBackgroundResource(R.drawable.repeat_all_button);
        } else if (currentPlaymode == Constants.PLAYMODE_REPEAT_ONE) {
            playmodeBtn.setBackgroundResource(R.drawable.repeat_button);
        } else if (currentPlaymode == Constants.PLAYMODE_RANDOM) {
            playmodeBtn.setBackgroundResource(R.drawable.shuffle_button);
        }
    }


    public void openCloseFolderList() {
        if (folderListOpen) {
            folderList.setVisibility(View.GONE);
            folderBtn.setVisibility(View.VISIBLE);
            folderListOpen = false;
        } else {
            folderList.setVisibility(View.VISIBLE);
            folderBtn.setVisibility(View.GONE);
            folderListOpen = true;
        }
    }
    /*
    * UI Methods end
     */







    /*
    * The MediaPlayer Service sends update to refresh the view (like songname or play/pause button)
     */
    @Override
    public void updateClient() {
        updateSongName();
        updatePlayButton();
        updateVolumeLabel();
    }
}
