/*
 * SicMu Player - Lightweight music player for Android
 * Copyright (C) 2015  Mathieu Souchaud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package souch.smp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class Main extends Activity {
    private Rows rows;
    private ListView songView;
    private RowsAdapter songAdt;
    ImageButton playButton;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean serviceBound = false;
    // the app is about to close
    private boolean finishing;

    private Timer timer;
    private final long updateInterval = 260;
    private SeekBar seekbar;
    // tell whether the seekbar is currently touch by a user
    private boolean touchSeekbar;
    private TextView duration;
    private TextView currDuration;

    // true if the user want to disable lockscreen
    private boolean noLock;

    // true if you want to keep the current song played visible
    private boolean followSong;

    private Parameters params;

    private Vibrator vibrator;

    private AnimationDrawable appAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Main", "onCreate");
        setContentView(R.layout.activity_main);
        finishing = false;

        songView = (ListView) findViewById(R.id.song_list);

        playButton = (ImageButton) findViewById(R.id.play_button);
        // useful only for testing
        playButton.setTag(R.drawable.ic_action_play);
        playButton.setOnTouchListener(touchListener);

        ImageButton gotoButton = (ImageButton) findViewById(R.id.goto_button);
        gotoButton.setOnTouchListener(touchListener);
        ImageButton lockButton = (ImageButton) findViewById(R.id.lock_button);
        lockButton.setOnTouchListener(touchListener);

        final int repeatDelta = 260;
        RepeatingImageButton prevButton = (RepeatingImageButton) findViewById(R.id.prev_button);
        prevButton.setRepeatListener(rewindListener, repeatDelta);
        prevButton.setOnTouchListener(touchListener);
        RepeatingImageButton nextButton = (RepeatingImageButton) findViewById(R.id.next_button);
        nextButton.setRepeatListener(forwardListener, repeatDelta);
        nextButton.setOnTouchListener(touchListener);

        playIntent = new Intent(this, MusicService.class);
        startService(playIntent);
        bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);

        duration = (TextView) findViewById(R.id.duration);
        currDuration = (TextView) findViewById(R.id.curr_duration);
        touchSeekbar = false;
        seekbar = (SeekBar) findViewById(R.id.seek_bar);
        seekbar.setOnSeekBarChangeListener(seekBarChangeListener);

        followSong = false;

        params = new ParametersImpl(this);

        vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        // tells the OS that the volume buttons should affect the "media" volume when your application is visible
        setVolumeControlStream(AudioManager.STREAM_MUSIC);


        // set the color statically for speed (don't know another prettier method)
        Row.backgroundColor = getResources().getColor(R.color.RowBackground);
        Row.levelOffset = 14; // todo what?

        RowSong.normalSongTextColor = getResources().getColor(R.color.RowSongTextNormal);
        RowSong.normalSongDurationTextColor = getResources().getColor(R.color.RowSongTextDuration);

        RowGroup.normalTextColor = getResources().getColor(R.color.RowGroupTextNormal);
        RowGroup.playingTextColor = getResources().getColor(R.color.RowGroupTextPlaying);

        ImageView appButton = (ImageView) findViewById(R.id.app_button);
        appButton.setBackgroundResource(R.drawable.ic_actionbar_launcher_anim);
        appAnimation = (AnimationDrawable) appButton.getBackground();

    }


    // connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("Main", "onServiceConnected");

            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            // get service
            musicSrv = binder.getService();

            rows = musicSrv.getRows();
            songAdt = new RowsAdapter(Main.this, rows, Main.this);
            songView.setAdapter(songAdt);
            songView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    if (!serviceBound)
                        return;

                    Row row = rows.get(position);
                    if (row.getClass() == RowGroup.class) {
                        // vibrate when big font choosed
                        if (params.getChoosedTextSize())
                            vibrate();

                        rows.invertFold(position);
                        songAdt.notifyDataSetChanged();
                    } else {
                        vibrate();

                        rows.selectNearestSong(position);
                        musicSrv.playSong();
                        updatePlayButton();
                    }
                    scrollToSong(position);
                }
            });
            songView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view,
                                               int position, long id) {
                    if (!serviceBound)
                        return false;

                    vibrate();

                    rows.selectNearestSong(position);
                    musicSrv.playSong();
                    updatePlayButton();
                    unfoldAndscrollToCurrSong();

                    return true;
                }
            });
            serviceBound = true;

            musicSrv.stopNotification();
            musicSrv.setMainIsVisible(true);

            // listView.getVisiblePosition() is wrong while the listview is not shown.
            // wait a bit that it is visible (should be replace by sthg like onXXX)
            (new Timer()).schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(firstScroll);
                }
            }, 100);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("Main", "onServiceDisconnected");
            serviceBound = false;
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener
            = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekbar.getVisibility() == TextView.VISIBLE) {
                currDuration.setText(RowSong.secondsToMinutes(seekBar.getProgress()));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            touchSeekbar = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            final int states = PlayerState.Prepared |
                    PlayerState.Started |
                    PlayerState.Paused |
                    PlayerState.PlaybackCompleted;
            if (serviceBound && musicSrv.isInState(states)) {
                Log.d("Main", "onStopTrackingTouch setProgress" + RowSong.secondsToMinutes(seekBar.getProgress()));
                seekBar.setProgress(seekBar.getProgress());
                // valid state : {Prepared, Started, Paused, PlaybackCompleted}
                musicSrv.seekTo(seekBar.getProgress());
            }

            touchSeekbar = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("Main", "onStart");

        restore();
        applyLock();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // updateInfo must be run in activity thread
                runOnUiThread(updateInfo);
            }
        }, 10, updateInterval);

        if (serviceBound) {
            // if service not bound stopNotification and setMainIsVisible is called onServiceConnected
            musicSrv.stopNotification();
            musicSrv.setMainIsVisible(true);
        }
    }

/*
    @Override
    protected void onResume(){
        super.onResume();
        Log.d("Main", "onResume");
    }


    @Override
    protected void onPause(){
        super.onPause();
        Log.d("Main", "onPause");
    }
*/

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Main", "onStop");
        timer.cancel();
        save();

        if (!finishing && serviceBound && musicSrv.playingLaunched())
            musicSrv.startNotification();

        musicSrv.setMainIsVisible(false);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("Main", "onDestroy");

        if (serviceBound) {
            // stop the service if not playing music
            if(!musicSrv.playingLaunched()) {
                musicSrv.stopService(playIntent);
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.app_name) + " destroyed.",
                        Toast.LENGTH_SHORT).show();
            }
            unbindService(musicConnection);
            serviceBound = false;
            musicSrv = null;
        }
    }


    final Runnable updateInfo = new Runnable() {
        public void run() {
            if (!serviceBound)
                return;

            //Log.d("Main", "updateInfo");
            if (musicSrv.getChanged()) {
                Log.d("Main", "updateInfo changed");
                vibrate();
                updatePlayButton();
                if(followSong)
                    unfoldAndscrollToCurrSong();
            } else {
                if(musicSrv.playingStopped()) {
                    stopPlayButton();
                }
                else if(!touchSeekbar && musicSrv.getSeekFinished()) {
                    Log.v("Main", "updateInfo setProgress" + RowSong.secondsToMinutes(musicSrv.getCurrentPosition()));
                    // getCurrentPosition {Idle, Initialized, Prepared, Started, Paused, Stopped, PlaybackCompleted}
                    seekbar.setProgress(musicSrv.getCurrentPosition());
                }
            }
        }
    };

    final Runnable firstScroll = new Runnable() {
        public void run() {
            updatePlayButton();
            unfoldAndscrollToCurrSong();
        }
    };


    private void updatePlayButton() {
        if (!serviceBound || musicSrv.playingStopped()) {
            // MediaPlayer has been destroyed or first start
            stopPlayButton();
        } else {
            if (!musicSrv.playingPaused()) {
                playButton.setImageResource(R.drawable.ic_action_pause);
                playButton.setTag(R.drawable.ic_action_pause);
                appAnimation.start();
            } else {
                playButton.setImageResource(R.drawable.ic_action_play);
                playButton.setTag(R.drawable.ic_action_play);
                appAnimation.stop();
            }

            RowSong rowSong = rows.getCurrSong();
            if(rowSong != null) {
                duration.setText(RowSong.secondsToMinutes(rowSong.getDuration()));
                duration.setVisibility(TextView.VISIBLE);
                seekbar.setMax(rowSong.getDuration());
                if (!touchSeekbar && musicSrv.getSeekFinished())
                    seekbar.setProgress(musicSrv.getCurrentPosition());
                seekbar.setVisibility(TextView.VISIBLE);
                currDuration.setText(RowSong.secondsToMinutes(musicSrv.getCurrentPosition()));
            }
        }

        songAdt.notifyDataSetChanged();
    }


    private void stopPlayButton() {
        duration.setVisibility(TextView.INVISIBLE);
        seekbar.setVisibility(TextView.INVISIBLE);
        currDuration.setText(R.string.app_name);
        playButton.setImageResource(R.drawable.ic_action_play);
        playButton.setTag(R.drawable.ic_action_play);
    }


    public void settings(View view) {
        openOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(musicSrv != null) {
            setFilterItem(menu.findItem(R.id.action_sort));
            setShakeItem(menu.findItem(R.id.action_shake));
            setChoosedTextSizeItem(menu.findItem(R.id.action_text_size));
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, Settings.class);
                startActivity(intent);
                return true;
            case R.id.action_fold:
                if(musicSrv != null) {
                    rows.fold();
                    songAdt.notifyDataSetChanged();
                    scrollToCurrSong();
                }
                return true;
            case R.id.action_unfold:
                if(musicSrv != null) {
                    rows.unfold();
                    songAdt.notifyDataSetChanged();
                    scrollToCurrSong();
                }
                return true;
            case R.id.action_shake:
                if(musicSrv != null) {
                    if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
                        musicSrv.setEnableShake(!musicSrv.getEnableShake());
                        // todo: only useful for item.getTitle() as the item is not changed, it just disapear
                        setShakeItem(item);
                        Toast.makeText(getApplicationContext(), item.getTitle(),
                                Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.settings_no_accelerometer),
                                Toast.LENGTH_LONG).show();
                    }
                }
                return true;
            case R.id.action_sort:
                if(musicSrv != null) {
                    if (rows.getFilter() == Filter.FOLDER)
                        rows.setFilter(Filter.ARTIST);
                    else if (rows.getFilter() == Filter.ARTIST)
                        rows.setFilter(Filter.TREE);
                    else
                        rows.setFilter(Filter.FOLDER);
                    setFilterItem(item);
                    Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_LONG).show();
                    songAdt.notifyDataSetChanged();
                    unfoldAndscrollToCurrSong();
                }
                return true;
            case R.id.action_text_size:
                if(musicSrv != null) {
                    params.setChooseTextSize(!params.getChoosedTextSize());
                    setChoosedTextSizeItem(item);
                    applyTextSize(params);
                    Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_LONG).show();
                    songAdt.notifyDataSetChanged();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setFilterItem(MenuItem item) {
        switch (rows.getFilter()) {
            case ARTIST:
                item.setIcon(R.drawable.ic_menu_artist);
                item.setTitle(getString(R.string.action_sort_artist));
                break;
            case FOLDER:
                item.setIcon(R.drawable.ic_menu_folder);
                item.setTitle(getString(R.string.action_sort_folder));
                break;
            case TREE:
                item.setIcon(R.drawable.ic_menu_tree);
                item.setTitle(getString(R.string.action_sort_tree));
                break;
        }
    }

    private void setShakeItem(MenuItem item) {
        if (musicSrv.getEnableShake()) {
            item.setIcon(R.drawable.ic_menu_shake_checked);
            item.setTitle(R.string.action_shake_enabled);
        } else {
            item.setIcon(R.drawable.ic_menu_shake);
            item.setTitle(R.string.action_shake_disabled);
        }
    }

    private void setChoosedTextSizeItem(MenuItem item) {
        if (params.getChoosedTextSize()) {
            item.setIcon(R.drawable.ic_menu_text_big);
            item.setTitle(getString(R.string.settings_text_size_big));
        }
        else {
            item.setIcon(R.drawable.ic_menu_text_regular);
            item.setTitle(getString(R.string.settings_text_size_regular));
        }
    }

    public void playOrPause(View view) {
        if(!serviceBound)
            return;

        if (musicSrv.isInState(PlayerState.Started)) {
            // valid state {Started, Paused, PlaybackCompleted}
            // if the player is between idle and prepared state, it will not be paused!
            musicSrv.pause();
        }
        else {
            if (musicSrv.isInState(PlayerState.Paused)) {
                // previously paused. Valid state {Prepared, Started, Paused, PlaybackCompleted}
                musicSrv.start();
            }
            else {
                musicSrv.playSong();
            }
        }

        updatePlayButton();
    }

    public void playNext(View view){
        if(!serviceBound)
            return;

        musicSrv.playNext();
        updatePlayButton();
        if(followSong)
            unfoldAndscrollToCurrSong();
    }

    public void playPrev(View view){
        if(!serviceBound)
            return;

        musicSrv.playPrev();
        updatePlayButton();
        if(followSong)
            unfoldAndscrollToCurrSong();
    }


    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                vibrate();
            }
            return false;
        }
    };

    private RepeatingImageButton.RepeatListener rewindListener =
        new RepeatingImageButton.RepeatListener() {
            /**
             * This method will be called repeatedly at roughly the interval
             * specified in setRepeatListener(), for as long as the button
             * is pressed.
             *
             * @param v           The button as a View.
             * @param duration    The number of milliseconds the button has been pressed so far.
             * @param repeatcount The number of previous calls in this sequence.
             *                    If this is going to be the last call in this sequence (i.e. the user
             *                    just stopped pressing the button), the value will be -1.
             */
            public void onRepeat(View v, long duration, int repeatcount) {
                Log.d("Main", "-- repeatcount: " + repeatcount + " duration: " + duration);
                if (repeatcount > 0) {
                    if (duration < 5000) {
                        // seek at 10x speed for the first 5 seconds
                        duration = duration * 10;
                    } else {
                        // seek at 40x after that
                        duration = 50000 + (duration - 5000) * 40;
                    }
                    int newPos = musicSrv.getCurrentPosition() - (int) (duration / 1000);
                    Log.d("Main", "<-- currpos: " + musicSrv.getCurrentPosition() + " seekto: " + newPos);
                    newPos = newPos < 0 ? 0 : newPos;
                    musicSrv.seekTo(newPos);
                }
            }
        };

    private RepeatingImageButton.RepeatListener forwardListener =
        new RepeatingImageButton.RepeatListener() {
            public void onRepeat(View v, long duration, int repeatcount) {
                Log.d("Main", "-- repeatcount: " + repeatcount + " duration: " + duration);
                if (repeatcount > 0) {
                    if (duration < 5000) {
                        // seek at 10x speed for the first 5 seconds
                        duration = duration * 10;
                    } else {
                        // seek at 40x after that
                        duration = 50000 + (duration - 5000) * 40;
                    }

                    int newPos = musicSrv.getCurrentPosition() + (int) (duration / 1000);
                    Log.d("Main", "--> currpos: " + musicSrv.getCurrentPosition() + " seekto: " + newPos);
                    if (newPos >= musicSrv.getDuration())
                        playNext(null);
                    else
                        musicSrv.seekTo(newPos);
                }
            }
    };

    public void gotoCurrSong(View view) {
        unfoldAndscrollToCurrSong();
    }

    public void unfoldAndscrollToCurrSong() {
        if(rows.unfoldCurrPos())
            songAdt.notifyDataSetChanged();
        scrollToSong(rows.getCurrPos());
    }

    public void scrollToCurrSong() {
        scrollToSong(rows.getCurrPos());
    }

    // this method could be improved, code is a bit obscure :-)
    public void scrollToSong(int gotoSong) {
        Log.d("Main", "scrollToSong getCurrPos:" + gotoSong);

        if(rows.size() == 0 || gotoSong < 0 || gotoSong >= rows.size())
            return;

        int first = songView.getFirstVisiblePosition();
        int last = songView.getLastVisiblePosition();
        int nbRow = last - first;
        // on ListView startup getVisiblePosition gives strange result
        if (nbRow < 0) {
            nbRow = 1;
            last = first + 1;
        }
        Log.d("Main", "scrollToSong first: " + first + " last: " + last + " nbRow: " + nbRow);

        // to show a bit of songItems before or after the cur song
        int showAroundTop = nbRow / 5;
        showAroundTop = showAroundTop < 1 ? 1 : showAroundTop;
        // show more song after the gotoSong
        int showAroundBottom = nbRow / 2;
        showAroundBottom = showAroundBottom < 1 ? 1 : showAroundBottom;
        Log.d("Main", "scrollToSong showAroundTop: " + showAroundTop + " showAroundBottom: " + showAroundBottom);


        // how far from top or bottom border the song is
        int offset = 0;
        if(gotoSong > last)
            offset = gotoSong - last;
        if(gotoSong < first)
            offset = first - gotoSong;

        // deactivate smooth if too far
        int smoothMaxOffset = 50;
        if(offset > smoothMaxOffset) {
            // setSelection set position at top of the screen
            gotoSong -= showAroundTop;
            if(gotoSong < 0)
                gotoSong = 0;
            songView.setSelection(gotoSong);
        }
        else {
            // smoothScrollToPosition only make position visible
            if(gotoSong + showAroundBottom >= last) {
                gotoSong += showAroundBottom;
                if(gotoSong >= rows.size())
                    gotoSong = rows.size() - 1;
            }
            else {
                gotoSong -= showAroundTop;
                if(gotoSong < 0)
                    gotoSong = 0;
            }
            songView.smoothScrollToPosition(gotoSong);
        }

        Log.d("Main", "scrollToSong position:" + gotoSong);
    }

    public void lockUnlock(View view) {
        noLock = !noLock;
        applyLock();
    }

    public void applyLock() {
        ImageButton lockButton = (ImageButton) findViewById(R.id.lock_button);
        if(noLock) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            lockButton.setImageResource(R.drawable.ic_action_unlocked);
        }
        else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            lockButton.setImageResource(R.drawable.ic_action_locked);
        }
    }


    public MusicService getMusicSrv() {
        return musicSrv;
    }

/*
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                Log.d("Main", "Exit app");
                finishing = true;
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
*/

    static public void applyTextSize(Parameters params) {
        int textSize;
        if (!params.getChoosedTextSize())
            textSize = params.getNormalTextSize();
        else
            textSize = params.getBigTextSize();

        RowSong.textSize = textSize;
        RowGroup.textSize = (int) (textSize * params.getTextSizeRatio());
    }

    private void restore() {
        noLock = params.getNoLock();
        followSong = params.getFollowSong();
        applyTextSize(params);
    }

    private void save() {
        params.setNoLock(noLock);
    }

    private void vibrate() {
        if (params.getVibrate())
            vibrator.vibrate(20);
    }
}


