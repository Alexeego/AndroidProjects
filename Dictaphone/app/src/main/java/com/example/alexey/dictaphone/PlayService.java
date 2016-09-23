package com.example.alexey.dictaphone;

import android.app.Service;
import android.content.Intent;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PlayService extends Service {
    public PlayService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    static volatile MediaPlayer mediaPlayer = null;
    volatile ExecutorService service = Executors.newFixedThreadPool(2);
    static volatile boolean playing = false;
    static volatile boolean pause = false;
    final Object lock = new Object();
    volatile int lastId = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (lastId > 0)
            stopSelf(lastId);
        lastId = startId;
        if(intent.getStringExtra(BlankFragment.COMMAND).equals(BlankFragment.PLAY)) {
            synchronized (lock) {
                if (mediaPlayer != null) {
                    playing = false;
                    mediaPlayer.stop();
                    mediaPlayer = null;
                    pause = false;
                }
            }
            mediaPlayer = MediaPlayer.create(this, Uri.parse(intent.getStringExtra(MainActivity.PATH_TO_FILE)));
            mediaPlayer.setLooping(false);
            if (BlankFragment.seekBar != null) {
                BlankFragment.duration = mediaPlayer.getDuration();
                BlankFragment.seekBar.setMax(BlankFragment.duration);
                BlankFragment.seekBar.setProgress(0);
            }
            service.execute(new Runnable() {
                @Override
                public void run() {
                    playing = true;
                    mediaPlayer.start();
                    service.execute(new Runnable() {
                        @Override
                        public void run() {
                            while (!Thread.currentThread().isInterrupted()) {
                                if (playing) {
                                    BlankFragment.seekBar.setProgress(mediaPlayer.getCurrentPosition());
                                } else break;
                                try {
                                    TimeUnit.MILLISECONDS.sleep(50);
                                } catch (InterruptedException ignore) {
                                    break;
                                }
                            }
                            BlankFragment.seekBar.setProgress(0);
                        }
                    });
                    while (!Thread.currentThread().isInterrupted() && playing && mediaPlayer != null && (mediaPlayer.isPlaying() || pause)) {
                        Thread.yield();
                    }
                    synchronized (lock) {
                        if (mediaPlayer != null) {
                            playing = false;
                            mediaPlayer.stop();
                            mediaPlayer = null;
                            pause = false;
                        }
                    }
                }
            });
        } else if(intent.getStringExtra(BlankFragment.COMMAND).equals(BlankFragment.PAUSE)){
            if(mediaPlayer != null){
                pause = true;
                mediaPlayer.pause();
            }
        } else if(intent.getStringExtra(BlankFragment.COMMAND).equals(BlankFragment.CONTINUE)){
            if(mediaPlayer != null){
                mediaPlayer.start();
                pause = false;
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDestroy() {
        synchronized (lock) {
            if (mediaPlayer != null) {
                playing = false;
                mediaPlayer.stop();
                mediaPlayer = null;
                pause = false;
            }
        }
        service.shutdownNow();
    }

    public static class MyOnSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener{

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (playing) {
                mediaPlayer.seekTo(seekBar.getProgress());
            }
        }
    }

}
