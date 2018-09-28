package eduard.wink.amazfitmusicplayer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

/**
 * Created by Eddy on 26.09.2018.
 */

public class MediaButtonIntentReceiver extends BroadcastReceiver {

    public static final String TAG = "MedBtnIntentReceiver";
    Callbacks mService = null;

    public MediaButtonIntentReceiver() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if (!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            return;
        }
        KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event == null) {
            return;
        }
        int action = event.getAction();
        if (action == KeyEvent.ACTION_DOWN && mService != null) {
            int keycode = event.getKeyCode();

            if(keycode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                mService.headsetButtonClicked(Constants.HEADSET_NEXT);
            } else if(keycode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                mService.headsetButtonClicked(Constants.HEADSET_PREV);
            } else if (keycode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                Log.i(TAG, "Play/Pause pressed");
            } else if (keycode == KeyEvent.KEYCODE_MEDIA_STOP) {
                mService.headsetButtonClicked(Constants.HEADSET_STOP);
            } else if (keycode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                mService.headsetButtonClicked(Constants.HEADSET_PLAY);
            } else if (keycode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                mService.headsetButtonClicked(Constants.HEADSET_PAUSE);
            } else if (keycode == KeyEvent.KEYCODE_VOLUME_UP) {
                mService.headsetButtonClicked(Constants.HEADSET_VOL_UP);
            } else if (keycode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                mService.headsetButtonClicked(Constants.HEADSET_VOL_DOWN);
            } else if(keycode == KeyEvent.KEYCODE_HEADSETHOOK) {
                Log.i(TAG, "Head Set Hook pressed");
            }
        }
        abortBroadcast();
    }


    //Here Activity register to the service as Callbacks client
    public void registerClient(Callbacks service){
        this.mService = (Callbacks)service;
    }

    public interface Callbacks{
        public void headsetButtonClicked(String btn);
    }
}
