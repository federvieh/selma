/**
 *
 */
package com.github.federvieh.selma.assimillib;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
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
public class LessonPlayer extends Service implements DelayService.DelayServiceListener, ExoPlayer.Listener {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;
    private static final String USER_AGENT = "selma";
    private static int delayPercentage = 100;
    private ExoPlayer player;

    public static void setDelay(int delay) {
        LessonPlayer.delayPercentage = delay;
        Log.d("LT", "Delay: " + delay);
    }

    @Override
    public void onWaitingRemainderUpdate(long remainingTime) {
        Log.d("LT", "remaining: " + remainingTime);
    }

    @Override
    public void onWaitingFinished(boolean result) {
        playNextOrStop(false);
    }

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

    private static AssimilLesson currentLesson;
    private static int currentTrack = -1;
    private static int previousTrack = -1;

    private static int numberOfInstances = 0; //This should always be one after the first time, right?

    private static Object lock = new Object();
    private static DelayService delayService;
    private static boolean doCont = false;
    private static long contPos = 0;
    private static long remWaitTime = 0;

    private NotificationCompat.Builder notifyBuilder;
    private static boolean playing;
    private static PlayMode playMode = PlayMode.REPEAT_ALL_LESSONS;
    private static ListTypes lt = ListTypes.TRANSLATE;


    public void onCreate() {
        numberOfInstances++;
        Log.i("LT", "Created a new LessonPlayer for the " + numberOfInstances + "th time.");

        notifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("Selma")
                .setContentText("Paused.")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true);
    }

    public static void stopPlaying(Context context) {
        Intent service = new Intent(context, LessonPlayer.class);
        service.putExtra(STOP, (long) 0);
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

    private static String preparePlayAndGetPath(AssimilLesson lesson, int trackNo){
        String id = null;
        try {
            id = lesson.getPathByTrackNo(trackNo);
        } catch (Exception e) {
            Log.w("LT", "Could not find track " + trackNo + " in lesson " + lesson, e);
            return null;
        }
        currentLesson = lesson;
        previousTrack = currentTrack;
        currentTrack = trackNo;

        return id;
    }

    public static void play(AssimilLesson lesson, int trackNo, boolean cont, Context ctxt) {
        String trackPath = preparePlayAndGetPath(lesson, trackNo);
        if(trackPath!=null){
            Log.d("LT", "doCont=" + cont);
            doCont = cont;
            //send intent to service
            Intent service = new Intent(ctxt, LessonPlayer.class);
            service.putExtra(PLAY, trackPath);
            ctxt.startService(service);
        }
    }

    private void play(String path) {
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
            stop(true);
        } else if (nextTrackId != -1) {
            playNextOrStop(true);
        } else if (nextLessonId != -1) {
            playNextLesson();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        // If the service is stopped for some reason, the user must explicitly
        // start again, e.g. by pressing the play button.
        return START_NOT_STICKY;
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
/*		case SINGLE_TRACK:
            Log.d("LT", "Playing single song finished. Stopping.");
			stop();
			break;*/
            case REPEAT_TRACK: {
                String trackPath = preparePlayAndGetPath(currentLesson, currentTrack);
                if(trackPath!=null) {
                    play(trackPath);
                }
                break;
            }
            case ALL_LESSONS:
            case REPEAT_ALL_LESSONS:
            case REPEAT_LESSON:
                boolean endOfLessonReached = false;
                String trackPath = preparePlayAndGetPath(currentLesson, currentTrack + 1);
                if(trackPath!=null) {
                    play(trackPath);
                } else {
                    endOfLessonReached = true;
                }
                if (endOfLessonReached) {
                    switch (getPlayMode()) {
                        case REPEAT_LESSON:
                            trackPath = preparePlayAndGetPath(currentLesson, 0);
                            if(trackPath!=null) {
                                play(trackPath);
                            }
                            break;
                        case ALL_LESSONS:
                        case REPEAT_ALL_LESSONS: {
                            //find next lesson
                            int lessonIdx = AssimilDatabase.getCurrentLessons().indexOf(currentLesson.getHeader());
                            if (lessonIdx < 0) {
                                Log.w("LT", "Current lesson not found (@ LessonPlayer.playNextOrStop_1). WTF? Stop playing.");
                                stop(false);
                            } else if (lessonIdx + 1 < AssimilDatabase.getCurrentLessons().size()) {
                                AssimilLesson lesson =
                                        AssimilDatabase.getLesson(AssimilDatabase.getCurrentLessons().get(lessonIdx + 1).getId(), this);
                                trackPath = preparePlayAndGetPath(lesson, 0);
                                if(trackPath!=null) {
                                    play(trackPath);
                                }
                            } else {
                                //last lesson reached
                                if (getPlayMode() == PlayMode.REPEAT_ALL_LESSONS) {
                                    //start again at first lesson again
                                    AssimilLesson lesson =
                                            AssimilDatabase.getLesson(AssimilDatabase.getCurrentLessons().get(0).getId(), this);
                                    trackPath = preparePlayAndGetPath(lesson, 0);
                                    if(trackPath!=null) {
                                        play(trackPath);
                                    }
                                } else {
                                    stop(false);
                                }
                            }
                            break;
                        }
                        default:
                            //Not possible
                            break;
                    }
                }
        }
    }

    /**
     * @return
     */
    public static ListTypes getListType() {
        return lt;
    }

    private void playNextLesson() {
        {
            //find next lesson
            int lessonIdx = AssimilDatabase.getCurrentLessons().indexOf(currentLesson.getHeader());
            if (lessonIdx < 0) {
                Log.w("LT", "Current lesson not found (@ LessonPlayer.playNextLesson_2). WTF? Stop playing.");
                stop(false);
            } else if (lessonIdx + 1 < AssimilDatabase.getCurrentLessons().size()) {
                String trackPath = preparePlayAndGetPath(AssimilDatabase.getLesson(AssimilDatabase.getCurrentLessons().get(lessonIdx + 1).getId(), this), 0);
                if(trackPath!=null) {
                    play(trackPath);
                }
//                play(AssimilDatabase.getLesson(AssimilDatabase.getCurrentLessons().get(lessonIdx + 1).getId(), this), 0, false, this);
            } else {
                //last lesson reached
                //start again at first lesson again
                String trackPath = preparePlayAndGetPath(AssimilDatabase.getLesson(AssimilDatabase.getCurrentLessons().get(0).getId(), this), 0);
                if(trackPath!=null) {
                    play(trackPath);
                }
//                play(AssimilDatabase.getLesson(AssimilDatabase.getCurrentLessons().get(0).getId(), this), 0, false, this);
            }
//			break;
        }
//		}
    }

    /* (non-Javadoc)
     * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
     */
    public void onPrepared() {
    }

    /**
     * @return The number (index) of the track that is currently being played.
     */
    public static int getTrackNumber(Context ctxt) {
        if (currentTrack < 0) {
            //On start read current lesson from SharedPreferences
            SharedPreferences settings = ctxt.getSharedPreferences("selma", Context.MODE_PRIVATE);
            currentTrack = settings.getInt(AssimilDatabase.LAST_TRACK_PLAYED, -1);
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
    public static AssimilLesson getLesson(Context ctxt) {
        if (currentLesson == null) {
            //On start read current lesson from SharedPreferences
            SharedPreferences settings = ctxt.getSharedPreferences("selma", Context.MODE_PRIVATE);
            long lessonId = settings.getLong(AssimilDatabase.LAST_LESSON_PLAYED, -1);
            if (lessonId > 0) {
                currentLesson = AssimilDatabase.getLesson(lessonId, ctxt);
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
            case ALL_LESSONS:
                playMode = PlayMode.REPEAT_TRACK;
                break;
            case REPEAT_TRACK:
                playMode = PlayMode.REPEAT_LESSON;
                break;
            case REPEAT_LESSON:
                playMode = PlayMode.REPEAT_ALL_LESSONS;
                break;
            case REPEAT_ALL_LESSONS:
                playMode = PlayMode.ALL_LESSONS;
                break;
            default:
                playMode = PlayMode.REPEAT_LESSON;
                break;
        }
    }

    /**
     * @return the lesson
     */
    public static String getLessonTitle(Context ctxt) {
        String rv = "...";
        AssimilLesson lesson = getLesson(ctxt);
        if (lesson != null) {
            rv = lesson.getNumber();
        }
        return rv;
    }

    /**
     * @return the number
     */
    public static String getTrackNumberText(Context ctxt) {
        String rv = "...";
        int trackNumber = getTrackNumber(ctxt);
        AssimilLesson lesson = getLesson(ctxt);
        if ((trackNumber >= 0) && (lesson != null)) {
            try {
                rv = lesson.getTextNumber(trackNumber);
            } catch (IndexOutOfBoundsException e) {
                Log.i("LT", "Could not find text " + trackNumber);
            }
        }
        return rv;
    }


    /**
     * @return
     */
    public static boolean isPlayingTranslate() {
        return (lt == ListTypes.TRANSLATE);
    }

    /**
     * @param lt
     */
    public static void setListType(ListTypes lt) {
        LessonPlayer.lt = lt;
    }

}
