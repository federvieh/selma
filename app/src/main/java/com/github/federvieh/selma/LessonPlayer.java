/*
 * Copyright (C) 2016 Frank Oltmanns (frank.oltmanns+selma(at)gmail.com)
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

package com.github.federvieh.selma;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.CursorIndexOutOfBoundsException;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

/**
 * This is a service that handles the complete playback functionality (i.e. interacting with MediaPlayer).
 * <p>
 * It's possible to define an additional waiting time percentage, so that after a track has finished the service waits
 * before continuing to play the next track based on the length of the previous track ({@see setDelay}).
 *
 * @author frank
 */
public class LessonPlayer extends Service implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, OnPreparedListener, OnAudioFocusChangeListener, DelayService.DelayServiceListener {
    private static final String LAST_LESSON_PLAYED = "LAST_LESSON_PLAYED";
    private static final String LAST_TRACK_PLAYED = "LAST_TRACK_PLAYED";
    private static final String DELAY_PERCENTAGE = "DELAY_PERCENTAGE";
    public static final String EXTRA_LESSON_ID = "EXTRA_LESSON_ID";
    private static final String EXTRA_TRACK_INDEX = "EXTRA_TRACK_INDEX";

    private static int delayPercentage = 100;

    /**
     * Set the percentage of track length to wait after playing a track before starting playback of the next track. E.g.
     * if the track that has just finished was 6 seconds and the delayPercentage is 50, the next track will start after
     * waiting 3 seconds.
     */
    public static void setDelay(int delay, Context ctxt) {
        if (ctxt != null) {
            Editor editor = ctxt.getSharedPreferences("selma", Context.MODE_PRIVATE).edit();
            editor
                    .putInt(DELAY_PERCENTAGE, delay)
                    .commit();
        }

        LessonPlayer.delayPercentage = delay;
        Log.d("LT", "Delay: " + delay);
    }

    /**
     * Get the percentage of track length to wait after playing a track before starting playback of the next track. E.g.
     * if the track that has just finished was 6 seconds and the delayPercentage is 50, the next track will start after
     * waiting 3 seconds.
     */
    public static int getDelay(Context ctxt) {
        if (ctxt != null) {
            SharedPreferences settings = ctxt.getSharedPreferences("selma", Context.MODE_PRIVATE);
            delayPercentage = settings.getInt(DELAY_PERCENTAGE, 0);
        }
        return delayPercentage;
    }


    @Override
    public void onWaitingRemainderUpdate(long remainingTime) {
//        Log.d("LT", "remaining: " + remainingTime);
    }

    @Override
    public void onWaitingFinished(boolean result) {
        playNextOrStop(false);
    }

    public static void setListType(ListTypes listType, Context ctxt) {
        switch (listType) {
            case ALL: {
                switch (currentLesson.getListType()) {
                    case ALL:
                        //No change
                        break;
                    case NO_TRANSLATE:
                        // NO_TRANSLATE -> ALL
                        //reload the lesson with translations while continuing playback
                        currentLesson = new Lesson(currentLesson.getId(), ctxt, listType);
                        break;
                    case ONLY_TRANSLATE:
                        // ONLY_TRANSLATE -> ALL
                        int translateSize = currentLesson.getSize();
                        Lesson newLesson = new Lesson(currentLesson.getId(), ctxt, listType);
                        int allTextsSize = newLesson.getSize();
                        int numberOfLessonTexts = allTextsSize - translateSize;
                        int newIndex = currentTrack + numberOfLessonTexts;
                        currentTrack = newIndex;
                        currentLesson = newLesson;
//                        //TODO: Is this even necessary? Might be enough to just correct currentTrack!
//                        play(newLesson, newIndex, true, ctxt);
                        break;
                }
                break;
            }
            case NO_TRANSLATE: {
                switch (currentLesson.getListType()) {
                    case ALL:
                        // ALL -> NO_TRANSLATE
                        // Check if we are currently playing translate, if so -> stop. Otherwise
                        // reload the lesson without translations while continuing playback
                        if (currentLesson.getTextType(currentTrack) == SelmaSQLiteHelper2.TextType.TRANSLATE
                                || currentLesson.getTextType(currentTrack) == SelmaSQLiteHelper2.TextType.TRANSLATE_HEADING) {
                            currentLesson = new Lesson(currentLesson.getId(), ctxt, listType);
                            currentTrack = 0;
                            LessonPlayer.stopPlaying(ctxt, false);
                        } else {
                            currentLesson = new Lesson(currentLesson.getId(), ctxt, listType);
                        }
                        break;
                    case NO_TRANSLATE:
                        //No change
                        break;
                    case ONLY_TRANSLATE:
                        //ONLY_TRANSLATE -> NO_TRANSLATE
                        currentLesson = new Lesson(currentLesson.getId(), ctxt, listType);
                        currentTrack = 0;
                        LessonPlayer.stopPlaying(ctxt, false);
                        break;
                }
                break;
            }
            case ONLY_TRANSLATE: {
                switch (currentLesson.getListType()) {
                    case ALL:
                        // ALL->ONLY_TRANSLATE
                        // Check if we are currently playing translate, if so -> Reload and correct
                        // the current track. Otherwise stop.
                        if (currentLesson.getTextType(currentTrack) == SelmaSQLiteHelper2.TextType.TRANSLATE
                                || currentLesson.getTextType(currentTrack) == SelmaSQLiteHelper2.TextType.TRANSLATE_HEADING) {
                            int allTextsSize = currentLesson.getSize();
                            Lesson newLesson = new Lesson(currentLesson.getId(), ctxt, listType);
                            int translateSize = newLesson.getSize();
                            int numberOfLessonTexts = allTextsSize - translateSize;
                            int newIndex = currentTrack - numberOfLessonTexts;
                            currentLesson = newLesson;
                            currentTrack = newIndex;
//                            //TODO: Is this even necessary? Might be enough to just correct currentTrack!
//                            play(newLesson, newIndex, true, ctxt);
                        } else {
                            currentLesson = new Lesson(currentLesson.getId(), ctxt, listType);
                            currentTrack = 0;
                            LessonPlayer.stopPlaying(ctxt, false);
                        }

                        break;
                    case NO_TRANSLATE:
                        // NO_TRANSLATE->ONLY_TRANSLATE
                        currentLesson = new Lesson(currentLesson.getId(), ctxt, listType);
                        currentTrack = 0;
                        LessonPlayer.stopPlaying(ctxt, false);
                        break;
                    case ONLY_TRANSLATE:
                        //No change
                        break;
                }
                break;
            }
        }
    }

    public enum PlayMode {
        //		SINGLE_TRACK,
//		SINGLE_LESSON,
        ALL_LESSONS,
        REPEAT_TRACK,
        REPEAT_LESSON,
        REPEAT_ALL_LESSONS,
//		REPEAT_ALL_STARRED
    }

    private static final String PLAY = "com.gmail.oltmanns.frank.language.trainer.LessonPlayer.PLAY";
    private static final String STOP = "com.gmail.oltmanns.frank.language.trainer.LessonPlayer.STOP";
    private static final String NEXT_TRACK = "com.gmail.oltmanns.frank.language.trainer.LessonPlayer.NEXT_TRACK";
    private static final String NEXT_LESSON = "com.gmail.oltmanns.frank.language.trainer.LessonPlayer.NEXT_LESSON";
    private static final String EXTRA_IS_PLAYING = "com.gmail.oltmanns.frank.language.trainer.LessonPlayer.EXTRA_IS_PLAYING";
    private static final int NOTIFICATION_ID = 0x21349843;
    public static final String PLAY_UPDATE_INTENT = "PLAY_UPDATE_INTENT";

    private static Lesson currentLesson;
    private static int currentTrack = -1;
    private static int previousTrack = -1;

    private static int numberOfInstances = 0; //This should always be one after the first time, right?

    private static Object lock = new Object();
    private static MediaPlayer mediaPlayer;
    private static DelayService delayService;
    private static boolean doCont = false;
    private static int contPos = 0;
    private static long remWaitTime = 0;

    private NotificationCompat.Builder notifyBuilder;
    private static boolean playing;
    private static PlayMode playMode = PlayMode.REPEAT_ALL_LESSONS;
//    private static ListTypes lt = ListTypes.TRANSLATE;

    /**
     * Name of the course that is currently being played back. {@code null} for all.
     */
    private static String courseName = null;

    public static void setCourseName(String courseName) {
        LessonPlayer.courseName = courseName;
    }

    /**
     * Are currently only starred being played back?
     */
    private static boolean starredOnly = false;

    public static void setStarredOnly(boolean starredOnly) {
        LessonPlayer.starredOnly = starredOnly;
    }

    public LessonPlayer() {
        numberOfInstances++;
        Log.i("LT", "Created a new LessonPlayer for the " + numberOfInstances + "th time.");

        notifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("Selma")
                .setContentText("Paused.")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true);
    }

    public static void stopPlaying(Context context, boolean savePos) {
        Intent service = new Intent(context, LessonPlayer.class);
        // 0: save position when stopping (e.g. when pause button was pressed)
        // 1: Don't save position. E.g. when the current track/lesson has become invalid
        service.putExtra(STOP, savePos ? (long)0 : (long)1);
        context.startService(service);
    }

    public static void playNextTrack(Context context) {
        Intent service = new Intent(context, LessonPlayer.class);
        service.putExtra(NEXT_TRACK, (long) 0);
        context.startService(service);
    }

    public static void playNextLesson(Context context) {
        Intent service = new Intent(context, LessonPlayer.class);
        service.putExtra(NEXT_LESSON, (long) 0);
        context.startService(service);
    }

    public static void play(Lesson lesson, int trackNo, boolean cont, Context ctxt) {
        String id = null;
        try {
            id = lesson.getPathByTrackNo(trackNo);
        } catch (Exception e) {
            Log.w("LT", "Could not find track " + trackNo + " in lesson " + lesson, e);
            return;
        }
        Log.d("LT", "doCont=" + cont);
        doCont = cont;
        //send intent to service
        currentLesson = lesson;
        previousTrack = currentTrack;
        currentTrack = trackNo;
        Intent service = new Intent(ctxt, LessonPlayer.class);
        service.putExtra(PLAY, id);
        ctxt.startService(service);
    }

    private void play(String path) {
        Log.d("LT", "play(): doCont=" + doCont + "; remWaitTime=" + remWaitTime);
        File f = new File(path);
        Uri contentUri = Uri.fromFile(f);
//		Uri contentUri = ContentUris.withAppendedId(
//				android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        if (doCont && (remWaitTime > 0)) {
            if (delayService != null) {
                delayService.cancel(true);
            }
            delayService = new DelayService(this);
            delayService.execute(remWaitTime);
            playing = true;
            //Broadcast to all kinds of UI elements
            Intent currTrackIntent = new Intent(LessonPlayer.PLAY_UPDATE_INTENT);
            currTrackIntent.putExtra(EXTRA_LESSON_ID, currentLesson.getId());
            currTrackIntent.putExtra(EXTRA_TRACK_INDEX, getTrackNumber(getApplicationContext()));
            currTrackIntent.putExtra(EXTRA_IS_PLAYING, playing);
            LocalBroadcastManager.getInstance(this).sendBroadcast(currTrackIntent);

            Intent resultIntent = new Intent(this, LessonDetailActivity.class);//(this, ShowLesson.class);
            resultIntent.putExtra(LessonDetailFragment.ARG_ITEM_ID, currentLesson.getId());

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            // Adds the back stack
            stackBuilder.addParentStack(LessonDetailActivity.class);
            // Adds the Intent to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            // Gets a PendingIntent containing the entire back stack
            PendingIntent pi = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            notifyBuilder
                    .setContentText("Playing: " + currentLesson.getId() + " " + getTrackNumberText(getApplicationContext()))
                    .setContentIntent(pi);

            startForeground(NOTIFICATION_ID, notifyBuilder.build());
        } else {
            if (delayService != null) {
                delayService.cancel(true);
            }

            if (mediaPlayer == null) {
                synchronized (lock) {
                    if (mediaPlayer == null) {
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mediaPlayer.setOnCompletionListener(this);
                        mediaPlayer.setOnPreparedListener(this);
                        mediaPlayer.setOnErrorListener(this);
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

    private void stop(boolean savePos) {
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
        //Broadcast to all kinds of UI elements
        Intent currTrackIntent = new Intent(LessonPlayer.PLAY_UPDATE_INTENT);
        currTrackIntent.putExtra(EXTRA_LESSON_ID, currentLesson.getId());
        currTrackIntent.putExtra(EXTRA_TRACK_INDEX, getTrackNumber(getApplicationContext()));
        currTrackIntent.putExtra(EXTRA_IS_PLAYING, playing);
        LocalBroadcastManager.getInstance(this).sendBroadcast(currTrackIntent);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.abandonAudioFocus(this);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w("LT", "Releasing audio focus failed! Result: " + result);
        }
    }

    /* (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent arg0) {
        //Binding is not used in this service
        return null;
    }

    private void handleCommand(Intent intent) {
        String playPath = intent.getStringExtra(PLAY);
        long stopId = intent.getLongExtra(STOP, -1);
        long nextTrackId = intent.getLongExtra(NEXT_TRACK, -1);
        long nextLessonId = intent.getLongExtra(NEXT_LESSON, -1);
        if (playPath != null) {
            play(playPath);
        } else if (stopId != -1) {
            if(stopId == 1) {
                stop(false);
            } else {
                stop(true);
            }
        } else if (nextTrackId != -1) {
            playNextOrStop(true);
        } else if (nextLessonId != -1) {
            playNextLesson();
        }
    }

    /* (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent, int, int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        // If the service is stopped for some reason, the user must explicitly
        // start again, e.g. by pressing the play button.
        return START_NOT_STICKY;
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
        //TODO: A wake lock might be needed here:
        //http://developer.android.com/training/scheduling/wakelock.html#cpu
        if (getDelay(getApplicationContext()) > 0) {
            if (delayService != null) {
                delayService.cancel(true);
            }
            delayService = new DelayService(this);
            long delay = (mp.getDuration() * delayPercentage) / 100;
            delayService.execute(delay);
        } else {
            playNextOrStop(false);
        }
    }

    private void playNextOrStop(boolean force) {
        PlayMode pm = getPlayMode();
        if (force) {
            if (pm == PlayMode.REPEAT_TRACK) {
                //If clicking on next while in repeating track mode, the user still expects to go to the next track
                pm = PlayMode.REPEAT_ALL_LESSONS;
            }
        }
        if (delayService != null && delayService.getStatus().equals(AsyncTask.Status.RUNNING)) {
            delayService.cancel(true);
            delayService = null;
        }
        switch (pm) {
            case REPEAT_TRACK:
                play(currentLesson, currentTrack, false, this);
                break;
            case ALL_LESSONS:
            case REPEAT_ALL_LESSONS:
            case REPEAT_LESSON:
                boolean endOfLessonReached = false;
                try {
                    currentLesson.getPathByTrackNo(currentTrack + 1);
                    play(currentLesson, currentTrack + 1, false, this);
                } catch (CursorIndexOutOfBoundsException e) {
                    endOfLessonReached = true;
                }
                if (endOfLessonReached) {
                    switch (getPlayMode()) {
                        case REPEAT_LESSON:
                            play(currentLesson, 0, false, this);
                            break;
                        case ALL_LESSONS:
                        case REPEAT_ALL_LESSONS: {
                            Lesson nextLesson = currentLesson.getNextLesson(starredOnly, courseName, pm, getApplicationContext());
                            if (nextLesson == null) {
                                stop(false);
                            } else {
                                play(nextLesson, 0, false, this);
                            }
                            break;
                        }
                        default:
                            //Not possible
                            break;
                    }
                }
                break;
            default:
                //Not possible
                break;
        }
    }

    /**
     * FIXME: Is this needed?
     *
     * @return
     */
//    public static ListTypes getListType() {
//        return lt;
//    }
    private void playNextLesson() {
        {
            //find next lesson
            Lesson nextLesson = currentLesson.getNextLesson(starredOnly, courseName, getPlayMode(), getApplicationContext());
            if (nextLesson == null) {
                //TODO: Maybe show a snack bar why playback has been stopped and option to continue at first lesson?
                stop(false);
            } else {
                play(nextLesson, 0, false, this);
            }
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
            mediaPlayer.seekTo(contPos);
        }
        mediaPlayer.start();
        playing = true;
        //Broadcast to all kinds of UI elements
        Intent currTrackIntent = new Intent(LessonPlayer.PLAY_UPDATE_INTENT);
        currTrackIntent.putExtra(EXTRA_LESSON_ID, currentLesson.getId());
        currTrackIntent.putExtra(EXTRA_TRACK_INDEX, getTrackNumber(getApplicationContext()));
        currTrackIntent.putExtra(EXTRA_IS_PLAYING, playing);
        LocalBroadcastManager.getInstance(this).sendBroadcast(currTrackIntent);

        Intent resultIntent = new Intent(this, LessonListActivity.getTwoPane() ? LessonListActivity.class : LessonDetailActivity.class);

        //Create the stack to open the correct lesson
        resultIntent.putExtra(LessonDetailFragment.ARG_ITEM_ID, currentLesson.getId());

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack
        stackBuilder.addParentStack(LessonListActivity.getTwoPane() ? LessonListActivity.class : LessonDetailActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent pi = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        notifyBuilder
                .setContentText("Playing: " + currentLesson.getId() + " " + getTrackNumberText(getApplicationContext()))
                .setContentIntent(pi);

        startForeground(NOTIFICATION_ID, notifyBuilder.build());
        try {
            Editor editor = getSharedPreferences("selma", Context.MODE_PRIVATE).edit();
            editor.putLong(LAST_LESSON_PLAYED, currentLesson.getId());
            editor.putInt(LAST_TRACK_PLAYED, currentTrack);
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
                stopPlaying(this, true);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.d("LT", "received AUDIOFOCUS_LOSS_TRANSIENT");
                stopPlaying(this, true);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                Log.d("LT", "received AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                stopPlaying(this, true);
                break;
        }
    }

    /**
     * @return The number (index) of the track that is currently being played.
     */
    public static int getTrackNumber(Context ctxt) {
        if (currentTrack < 0) {
            //On start read current lesson from SharedPreferences
            SharedPreferences settings = ctxt.getSharedPreferences("selma", Context.MODE_PRIVATE);
            currentTrack = settings.getInt(LAST_TRACK_PLAYED, -1);
        }
        return currentTrack;
    }

    /**
     * @return The number (index) of the track that was previously played.
     */
    public static int getPreviousTrack() {
        return previousTrack;
    }

    /**
     * @return The lesson that is currently being played.
     */
    public static Lesson getLesson(Context ctxt) {
        if (currentLesson == null) {
            //On start read current lesson from SharedPreferences
            SharedPreferences settings = ctxt.getSharedPreferences("selma", Context.MODE_PRIVATE);
            long lessonId = settings.getLong(LAST_LESSON_PLAYED, -1);
            if (lessonId > 0) {
                int lt = settings.getInt(LessonDetailFragment.LAST_LIST_TYPE, ListTypes.ALL.ordinal());
                currentLesson = new Lesson(lessonId, ctxt, ListTypes.values()[lt]);
            }
        }

        return currentLesson;
    }

    /**
     * @return
     */
    public static boolean isPlaying() {
        return playing;
    }

    public static PlayMode getPlayMode() {
        return playMode;
    }

    /**
     *
     */
    public static void setPlayMode(PlayMode pm) {
        playMode = pm;
    }

    public static void increasePlayMode() {
        switch (playMode) {
//		case SINGLE_TRACK:
//			playMode = PlayMode.SINGLE_LESSON;
//			break;
//		case SINGLE_LESSON:
//			playMode = PlayMode.ALL_LESSONS;
//			break;
            case ALL_LESSONS:
                playMode = PlayMode.REPEAT_TRACK;
                break;
            case REPEAT_TRACK:
                playMode = PlayMode.REPEAT_LESSON;
                break;
            case REPEAT_LESSON:
//			if((lt == ListTypes.LIST_TYPE_ALL_NO_TRANSLATE)||(lt == ListTypes.LIST_TYPE_ALL_TRANSLATE)){
                playMode = PlayMode.REPEAT_ALL_LESSONS;
//			}
//			else{
//				playMode = PlayMode.REPEAT_ALL_STARRED;
//			}
                break;
            case REPEAT_ALL_LESSONS:
                playMode = PlayMode.ALL_LESSONS;
                break;
//		case REPEAT_ALL_STARRED:
////			playMode = PlayMode.SINGLE_TRACK;
//			playMode = PlayMode.ALL_LESSONS;
//			break;
            default:
//			playMode = PlayMode.SINGLE_TRACK;
                playMode = PlayMode.REPEAT_LESSON;
                break;
        }
    }

    /**
     * FIXME: Do we still need this?
     *
     * @return the lesson
     */
    public static String getLessonTitle(Context ctxt) {
        String rv = "...";
        Lesson lesson = getLesson(ctxt);
        if (lesson != null) {
            rv = lesson.getLessonName();
        }
        return rv;
    }

    /**
     * @return the number
     */
    public static String getTrackNumberText(Context ctxt) {
        String rv = "...";
        int trackNumber = getTrackNumber(ctxt);
        Lesson lesson = getLesson(ctxt);
        if ((trackNumber >= 0) && (lesson != null)) {
            try {
                rv = lesson.getTextNumber(trackNumber);
            } catch (IndexOutOfBoundsException e) {
                Log.i("LT", "Could not find text " + trackNumber);
            }
        }
        return rv;
    }


    /** FIXME: Is this needed?
     * @return
     */
//    public static boolean isPlayingTranslate() {
//        return (lt == ListTypes.TRANSLATE);
//    }

    /**FIXME: Is this needed?
     * @param lt
     */
//    public static void setListType(ListTypes lt) {
//        LessonPlayer.lt = lt;
//    }

}
