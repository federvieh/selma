/**
 *
 */
package com.github.federvieh.selma.assimillib;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.github.federvieh.selma.MainActivity;

import java.io.File;

/**
 * @author frank
 */
public class LessonPlayerMP extends LessonPlayer implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, OnPreparedListener, OnAudioFocusChangeListener {
    private static MediaPlayer mediaPlayer;

    @Override
    protected void play(String path) {
        Log.d("LT", "play(): doCont=" + doCont + "; remWaitTime=" + remWaitTime);
        File f = new File(path);
        Uri contentUri = Uri.fromFile(f);
//		Uri contentUri = ContentUris.withAppendedId(
//				android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        if (doCont && (remWaitTime > 0)) {
            if (delayService != null) {
                delayService.cancel(true); //FIXME: What does this do
            }
            delayService = new DelayService(this);
            //FIXME: Make this configurable
            delayService.execute(remWaitTime);
            //FIXME: Does the following work here?
            playing = true;
            Intent currTrackIntent = new Intent(LessonPlayer.PLAY_UPDATE_INTENT);
            currTrackIntent.putExtra(AssimilOnClickListener.EXTRA_LESSON_ID, currentLesson.getHeader().getId());
            currTrackIntent.putExtra(AssimilOnClickListener.EXTRA_TRACK_INDEX, getTrackNumber(getApplicationContext()));
            currTrackIntent.putExtra(EXTRA_IS_PLAYING, playing);
            LocalBroadcastManager.getInstance(this).sendBroadcast(currTrackIntent);
            Intent resultIntent = new Intent(this, MainActivity.class);//(this, ShowLesson.class);
            resultIntent.putExtra(AssimilOnClickListener.EXTRA_LESSON_ID, currentLesson.getHeader().getId());

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            // Adds the back stack
            stackBuilder.addParentStack(MainActivity.class);//(ShowLesson.class);
            // Adds the Intent to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            // Gets a PendingIntent containing the entire back stack
            PendingIntent pi = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            notifyBuilder
                    .setContentText("Playing: " + getLessonTitle(getApplicationContext()) + " " + getTrackNumberText(getApplicationContext()))
                    .setContentIntent(pi);

            startForeground(NOTIFICATION_ID, notifyBuilder.getNotification());
        } else {
            if (mediaPlayer == null) {
                synchronized (lock) {
                    if (mediaPlayer == null) {
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mediaPlayer.setOnCompletionListener(this);
                        mediaPlayer.setOnPreparedListener(this);
                        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                        //TODO: Move to extra function, add error listener
                    }
                }
            } else {
                mediaPlayer.reset();
            }
            try {
                mediaPlayer.setDataSource(this, contentUri);
                mediaPlayer.prepareAsync();
            } catch (Exception e) {
                Log.w("LT", "Could not set data source or prepare media player " + contentUri, e);
                return;
            }
        }
    }

    @Override
    protected void stop(boolean savePos) {
        stopForeground(true);
        stopSelf();

        if (delayService != null && delayService.getStatus().equals(AsyncTask.Status.RUNNING)) {
            delayService.cancel(true);
            //FIXME: How to resume?
            if (savePos) {
                remWaitTime = delayService.getRemainingTime();
            } else {
                remWaitTime = 0;
            }
            delayService = null;
            contPos = 0;
        } else if (savePos) {
            contPos = ((mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0);
            remWaitTime = 0;
            Log.d("LT", "saving position " + contPos);
        } else {
            contPos = 0;
            remWaitTime = 0;
        }
        if (mediaPlayer != null) {
            //release
            mediaPlayer.release();
            mediaPlayer = null;
        }
        playing = false;
        Intent currTrackIntent = new Intent(LessonPlayer.PLAY_UPDATE_INTENT);
        currTrackIntent.putExtra(AssimilOnClickListener.EXTRA_LESSON_ID, currentLesson.getHeader().getId());
        currTrackIntent.putExtra(AssimilOnClickListener.EXTRA_TRACK_INDEX, getTrackNumber(getApplicationContext()));
        currTrackIntent.putExtra(EXTRA_IS_PLAYING, playing);
        LocalBroadcastManager.getInstance(this).sendBroadcast(currTrackIntent);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.abandonAudioFocus(this);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w("LT", "Releasing audio focus failed! Result: " + result);
        }
    }

    /* (non-Javadoc)
     * @see android.media.MediaPlayer.OnErrorListener#onError(android.media.MediaPlayer, int, int)
     */
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.w("LT", "Media Player has gone to error mode. What: " + what + ", Extra: " + extra + ". Releasing MP.");
        stop(false);
        return true;
    }

    /* (non-Javadoc)
     * @see android.media.MediaPlayer.OnCompletionListener#onCompletion(android.media.MediaPlayer)
     */
    public void onCompletion(MediaPlayer mp) {
        Log.d("LT", "onCompletion");
        //FIXME: Make this configurable
        if (delayPercentage > 0) {
            if (delayService != null) {
                delayService.cancel(true); //FIXME: What does this do
            }
            delayService = new DelayService(this);
            //FIXME: Make this configurable
            long delay = (mp.getDuration() * delayPercentage) / 100;
            delayService.execute(delay);
        } else {
            playNextOrStop(false);
        }
    }

    /* (non-Javadoc)
     * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
     */
    public void onPrepared(MediaPlayer arg0) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Toast.makeText(this, "Could not start playing! Some other media playing?", Toast.LENGTH_SHORT).show();
            return;
        }
        if (doCont) {
            doCont = false;
            contPos -= 100;
            if (contPos < 0) {
                contPos = 0;
            }
            Log.d("LT", "Resuming at position " + contPos);
            mediaPlayer.seekTo((int) contPos);
        }
        mediaPlayer.start();
        playing = true;
        Intent currTrackIntent = new Intent(LessonPlayer.PLAY_UPDATE_INTENT);
        currTrackIntent.putExtra(AssimilOnClickListener.EXTRA_LESSON_ID, currentLesson.getHeader().getId());
        currTrackIntent.putExtra(AssimilOnClickListener.EXTRA_TRACK_INDEX, getTrackNumber(getApplicationContext()));
        currTrackIntent.putExtra(EXTRA_IS_PLAYING, playing);
        LocalBroadcastManager.getInstance(this).sendBroadcast(currTrackIntent);
        Intent resultIntent = new Intent(this, MainActivity.class);//(this, ShowLesson.class);
        resultIntent.putExtra(AssimilOnClickListener.EXTRA_LESSON_ID, currentLesson.getHeader().getId());

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack
        stackBuilder.addParentStack(MainActivity.class);//(ShowLesson.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent pi = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        notifyBuilder
                .setContentText("Playing: " + getLessonTitle(getApplicationContext()) + " " + getTrackNumberText(getApplicationContext()))
                .setContentIntent(pi);

        startForeground(NOTIFICATION_ID, notifyBuilder.getNotification());
        try {
            Editor editor = getSharedPreferences("selma", Context.MODE_PRIVATE).edit();
            editor.putLong(AssimilDatabase.LAST_LESSON_PLAYED, currentLesson.getHeader().getId());
            editor.putInt(AssimilDatabase.LAST_TRACK_PLAYED, currentTrack);
            editor.commit();
        } catch (NullPointerException e) {
            //Might happen when in "starred only" mode but current last played lesson is not starred.
        }
    }

    /* (non-Javadoc)
     * @see android.media.AudioManager.OnAudioFocusChangeListener#onAudioFocusChange(int)
     */
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                play(currentLesson, currentTrack, true, this);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                Log.d("LT", "received AUDIOFOCUS_LOSS");
                stopPlaying(this);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.d("LT", "received AUDIOFOCUS_LOSS_TRANSIENT");
                stopPlaying(this);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                Log.d("LT", "received AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                stopPlaying(this);
                break;
        }
    }
}
