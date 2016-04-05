/**
 *
 */
package com.github.federvieh.selma.assimillib;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.github.federvieh.selma.MainActivity;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;

import java.io.File;

/**
 * @author frank
 */
public class LessonPlayerExo extends LessonPlayer implements ExoPlayer.Listener {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;
    private static final String USER_AGENT = "selma";
    private ExoPlayer player;

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                Log.d(this.getClass().getSimpleName(), "onPlayerStateChanged("+playWhenReady+", STATE_BUFFERING)");
                break;
            case ExoPlayer.STATE_ENDED:
                Log.d(this.getClass().getSimpleName(), "onPlayerStateChanged("+playWhenReady+", STATE_ENDED)");
                long dur = player.getDuration();
                if (delayPercentage > 0) {
                    if (delayService != null) {
                        delayService.cancel(true);
                    }
                    delayService = new DelayService(this);
                    long delay = (dur * delayPercentage) / 100;
                    delayService.execute(delay);
                } else {
                    playNextOrStop(false);
                }
                break;
            case ExoPlayer.STATE_IDLE:
                Log.d(this.getClass().getSimpleName(), "onPlayerStateChanged("+playWhenReady+", STATE_BUFFERING)");
                break;
            case ExoPlayer.STATE_PREPARING:
                Log.d(this.getClass().getSimpleName(), "onPlayerStateChanged("+playWhenReady+", STATE_BUFFERING)");
                break;
            case ExoPlayer.STATE_READY:
                Log.d(this.getClass().getSimpleName(), "onPlayerStateChanged("+playWhenReady+", STATE_BUFFERING)");
                break;
            case ExoPlayer.TRACK_DEFAULT:
                Log.d(this.getClass().getSimpleName(), "onPlayerStateChanged("+playWhenReady+", STATE_BUFFERING)");
                break;
            case ExoPlayer.TRACK_DISABLED:
                Log.d(this.getClass().getSimpleName(), "onPlayerStateChanged("+playWhenReady+", STATE_BUFFERING)");
                break;
            default:
                Log.w(this.getClass().getSimpleName(), "onPlayerStateChanged("+playWhenReady+", Unknown state: "+playbackState+")");
                break;
        }
    }

    @Override
    public void onPlayWhenReadyCommitted() {
        //Not required
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Log.e(this.getClass().getSimpleName(), "received error", error);
        if(player != null) {
            player.release();
            player=null;
        }
    }

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
            delayService.execute(remWaitTime);
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
            if (player == null) {
                synchronized (lock) {
                    if (player == null) {
                        player = ExoPlayer.Factory.newInstance(1);
                        player.addListener(this);
                        player.setPlayWhenReady(true);
                    }
                }
            }
            Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
            DataSource dataSource = /*new FileDataSource();*/new DefaultUriDataSource(this, null, USER_AGENT);
            ExtractorSampleSource sampleSource = new ExtractorSampleSource(
                    contentUri, dataSource, allocator, BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
            MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(
                    sampleSource, MediaCodecSelector.DEFAULT);
            if (doCont) {
                doCont = false;
                contPos -= 100;
                if (contPos < 0) {
                    contPos = 0;
                }
                Log.d("LT", "Resuming at position " + contPos);
                player.seekTo(contPos);
            } else {
                player.seekTo(0);
            }
            player.prepare(audioRenderer);
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
    }

    @Override
    protected void stop(boolean savePos) {
        stopForeground(true);
        stopSelf();

        if (delayService != null && delayService.getStatus().equals(AsyncTask.Status.RUNNING)) {
            delayService.cancel(true);
            if (savePos) {
                remWaitTime = delayService.getRemainingTime();
            } else {
                remWaitTime = 0;
            }
            delayService = null;
            contPos = 0;
        } else if (savePos) {
            contPos = ((player != null) ? player.getCurrentPosition() : 0);
            remWaitTime = 0;
            Log.d("LT", "saving position " + contPos);
        } else {
            contPos = 0;
            remWaitTime = 0;
        }
        if (player != null) {
            //release
            player.release();
            player = null;
        }
        playing = false;
        Intent currTrackIntent = new Intent(LessonPlayer.PLAY_UPDATE_INTENT);
        currTrackIntent.putExtra(AssimilOnClickListener.EXTRA_LESSON_ID, currentLesson.getHeader().getId());
        currTrackIntent.putExtra(AssimilOnClickListener.EXTRA_TRACK_INDEX, getTrackNumber(getApplicationContext()));
        currTrackIntent.putExtra(EXTRA_IS_PLAYING, playing);
        LocalBroadcastManager.getInstance(this).sendBroadcast(currTrackIntent);
    }


}
