package com.github.federvieh.selma.assimillib;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public abstract class LessonPlayer extends Service implements DelayService.DelayServiceListener {
    public static final String PLAY_UPDATE_INTENT = "PLAY_UPDATE_INTENT";
    protected static final double MAX_LOCK_LENGTH_FACTOR = 3.1; //trackLength + 200% delay is the longest lock we need

    /**
     *
     */
    public static void setPlayMode(PlayMode pm) {
        playMode = pm;
    }

    public static PlayMode getPlayMode() {
        return playMode;
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
     * @return if the LessonPlayer is currently playing
     */
    public static boolean isPlaying() {
        return playing;
    }

    public static void stopPlaying(Context context) {
        Intent service;
        if(android.os.Build.VERSION.SDK_INT>=16){
            service = new Intent(context, LessonPlayerExo.class);
        } else {
            service = new Intent(context, LessonPlayerMP.class);
        }
        service.putExtra(STOP, (long) 0);
        context.startService(service);
    }

    public static void play(AssimilLesson lesson, int trackNo, boolean cont, Context ctxt) {
        String trackPath = preparePlayAndGetPath(lesson, trackNo);
        Log.d("LT", "doCont=" + cont);
        doCont = cont;
        //send intent to service
        if(trackPath!=null) {
            Intent service;
            if (android.os.Build.VERSION.SDK_INT >= 16) {
                service = new Intent(ctxt, LessonPlayerExo.class);
            } else {
                service = new Intent(ctxt, LessonPlayerMP.class);
            }
            service.putExtra(PLAY, trackPath);
            ctxt.startService(service);
        }
    }

    public static void playNextTrack(Context context) {
        Intent service;
        if(android.os.Build.VERSION.SDK_INT>=16){
            service = new Intent(context, LessonPlayerExo.class);
        } else {
            service = new Intent(context, LessonPlayerMP.class);
        }
        service.putExtra(NEXT_TRACK, (long) 0);
        context.startService(service);
    }

    public static void playNextLesson(Context context) {
        Intent service;
        if(android.os.Build.VERSION.SDK_INT>=16){
            service = new Intent(context, LessonPlayerExo.class);
        } else {
            service = new Intent(context, LessonPlayerMP.class);
        }
        service.putExtra(NEXT_LESSON, (long) 0);
        context.startService(service);
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
     * @param lt The list type that shall now be used
     */
    public static void setListType(ListTypes lt) {
        LessonPlayer.lt = lt;
    }

    /**
     * @return The list type that is currently used
     */
    public static ListTypes getListType() {
        return lt;
    }


    /**
     * @return The number (index) of the track that was previously played.
     */
    public static int getPreviousTrack() {
        return previousTrack;
    }

    /**
     * @return if the LessonPlayer is playing translate files (in general, not necessarily at this moment)
     */
    public static boolean isPlayingTranslate() {
        return (lt == ListTypes.TRANSLATE);
    }

    protected static int delayPercentage = 100;

    public static void setDelay(int delay) {
        LessonPlayerExo.delayPercentage = delay;
        Log.d("LT", "Delay: " + delay);
    }

    /*********************************************
     *
     *             abstract methods
     *
     *********************************************/

    protected abstract void stop(boolean savePos);
    protected abstract void play(String path);

    /*********************************************
     *
     *             common methods
     *
     *********************************************/

    @Override
    public void onWaitingRemainderUpdate(long remainingTime) {
//        Log.d("LT", "remaining: " + remainingTime);
    }

    @Override
    public void onWaitingFinished(boolean result) {
        playNextOrStop(false);
    }

    private static final String PLAY = "com.gmail.oltmanns.frank.language.trainer.LessonPlayer.PLAY";
    private static final String STOP = "com.gmail.oltmanns.frank.language.trainer.LessonPlayer.STOP";
    private static final String NEXT_TRACK = "com.gmail.oltmanns.frank.language.trainer.LessonPlayer.NEXT_TRACK";
    private static final String NEXT_LESSON = "com.gmail.oltmanns.frank.language.trainer.LessonPlayer.NEXT_LESSON";
    protected static final String EXTRA_IS_PLAYING = "com.gmail.oltmanns.frank.language.trainer.LessonPlayer.EXTRA_IS_PLAYING";
    protected static final int NOTIFICATION_ID = 0x21349843;

    protected static AssimilLesson currentLesson;
    protected static int currentTrack = -1;
    private static int previousTrack = -1;

    private static int numberOfInstances = 0; //This should always be one after the first time, right?

    protected static PowerManager.WakeLock wakeLock;

    protected final static Object lock = new Object();
    protected static DelayService delayService;
    protected static boolean doCont = false;
    protected static long contPos = 0;
    protected static long remWaitTime = 0;

    protected NotificationCompat.Builder notifyBuilder;
    protected static boolean playing;
    private static PlayMode playMode = PlayMode.REPEAT_ALL_LESSONS;
    private static ListTypes lt = ListTypes.TRANSLATE;


    public void onCreate() {
        numberOfInstances++;
        Log.i("LT", "Created a new LessonPlayerExo for the " + numberOfInstances + "th time.");

        notifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("Selma")
                .setContentText("Paused.")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true);
    }

    private static String preparePlayAndGetPath(AssimilLesson lesson, int trackNo){
        String id;
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

    protected void playNextOrStop(boolean force) {
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
                                Log.w("LT", "Current lesson not found (@ LessonPlayerExo.playNextOrStop_1). WTF? Stop playing.");
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

    private void playNextLesson() {
        {
            //find next lesson
            int lessonIdx = AssimilDatabase.getCurrentLessons().indexOf(currentLesson.getHeader());
            if (lessonIdx < 0) {
                Log.w("LT", "Current lesson not found (@ LessonPlayerExo.playNextLesson_2). WTF? Stop playing.");
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











}
