/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import java.io.File;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.github.federvieh.selma.MainActivity;

/**
 * @author frank
 *
 */
public class LessonPlayer extends Service implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, OnPreparedListener, OnAudioFocusChangeListener{
	
	public enum PlayMode{
//		SINGLE_TRACK,
//		SINGLE_LESSON,
		ALL_LESSONS,
		REPEAT_TRACK,
		REPEAT_LESSON,
		REPEAT_ALL_LESSONS,
		REPEAT_ALL_STARRED
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
	
	private static int numberOfInstances = 0; //This should always be one after the first time, right?
	
	private static Object lock = new Object();
	private static MediaPlayer mediaPlayer;
	private static boolean doCont = false;
	private static int contPos = 0;
	private NotificationCompat.Builder notifyBuilder;
	private static boolean playing;
	private static PlayMode playMode = PlayMode.REPEAT_ALL_STARRED;
	private static ListTypes lt = ListTypes.LIST_TYPE_ALL_TRANSLATE;
	
	
	public LessonPlayer(){
		numberOfInstances++;
		Log.i("LT","Created a new LessonPlayer for the "+numberOfInstances+"th time.");
		
		notifyBuilder = new NotificationCompat.Builder(this)
		    .setContentTitle("Language Trainer")
		    .setContentText("Paused.")
		    .setSmallIcon(android.R.drawable.btn_radio)
		    .setOngoing(true);
	}
	
	public static void stopPlaying(Context context){
		Intent service = new Intent(context, LessonPlayer.class);
		service.putExtra(STOP, (long)0);
		context.startService(service);
	}
	
	public static void playNextTrack(Context context){
		Intent service = new Intent(context, LessonPlayer.class);
		service.putExtra(NEXT_TRACK, (long)0);
		context.startService(service);
	}
	
	public static void playNextLesson(Context context){
		Intent service = new Intent(context, LessonPlayer.class);
		service.putExtra(NEXT_LESSON, (long)0);
		context.startService(service);
	}
	
	public static void play(AssimilLesson lesson, int trackNo, boolean cont, Context ctxt){
		String id = null;
		try{
			id = lesson.getPathByTrackNo(trackNo);
		}
		catch (Exception e){
			Log.w("LT","Could not find track "+trackNo+" in lesson "+lesson, e);
			return;
		}
		Log.d("LT","doCont="+cont);
		doCont = cont;
		//send intent to service
		currentLesson = lesson;
		currentTrack = trackNo;
		//FIXME: Inform PlaybarFragement to update view content (lesson + track)
		Intent service = new Intent(ctxt, LessonPlayer.class);
		service.putExtra(PLAY, id);
		ctxt.startService(service);
	}
	
	private void play(String path){
		File f = new File(path);
		Uri contentUri = Uri.fromFile(f);
//		Uri contentUri = ContentUris.withAppendedId(
//				android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
		if(mediaPlayer == null){
			synchronized(lock){
				if(mediaPlayer==null){
					mediaPlayer = new MediaPlayer();
					mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
					mediaPlayer.setOnCompletionListener(this);
					mediaPlayer.setOnPreparedListener(this);
					mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
					//TODO: Move to extra function, add error listener
				}
			}
		}
		else{
			mediaPlayer.reset();
		}
		try {
			mediaPlayer.setDataSource(this, contentUri);
			mediaPlayer.prepareAsync();
		} catch (Exception e) {
			Log.w("LT","Could not set data source or prepare media player "+contentUri, e);
			return;
		}
	}
	
	private void stop(boolean savePos){
		stopForeground(true);
		stopSelf();
		
		if(savePos){
			contPos  = ((mediaPlayer!=null)?mediaPlayer.getCurrentPosition():0);
			Log.d("LT", "saving position "+contPos);
		}
		else{
			contPos = 0;
		}
		if(mediaPlayer!=null){
			//release
			mediaPlayer.release();
			mediaPlayer = null;
		}
		playing = false;
		Intent currTrackIntent = new Intent(LessonPlayer.PLAY_UPDATE_INTENT);
		currTrackIntent.putExtra(AssimilOnClickListener.EXTRA_LESSON_ID, currentLesson.getHeader().getId());
		currTrackIntent.putExtra(AssimilOnClickListener.EXTRA_TRACK_INDEX, getTrackNumber());
		currTrackIntent.putExtra(EXTRA_IS_PLAYING, playing);
		LocalBroadcastManager.getInstance(this).sendBroadcast(currTrackIntent);

		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int result = audioManager.abandonAudioFocus(this);
		if(result!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
			Log.w("LT", "Releasing audio focus failed! Result: "+result);
		}
		try{
			Editor editor = getSharedPreferences("selma", Context.MODE_PRIVATE).edit();
			editor.putLong(AssimilDatabase.LAST_LESSON_PLAYED, currentLesson.getHeader().getId());
			editor.putInt(AssimilDatabase.LAST_TRACK_PLAYED, currentTrack);
			editor.commit();
		}
		catch(NullPointerException e){
			//Might happen when in "starred only" mode but current last played lesson is not starred.
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
	
	private void handleCommand(Intent intent){
		String playPath = intent.getStringExtra(PLAY);
		long stopId = intent.getLongExtra(STOP, -1);
		long nextTrackId = intent.getLongExtra(NEXT_TRACK, -1);
		long nextLessonId = intent.getLongExtra(NEXT_LESSON, -1);
		if(playPath != null){
			play(playPath);
		}
		else if(stopId != -1){
			stop(true);
		}
		else if(nextTrackId != -1){
			playNextOrStop(true);
		}
		else if(nextLessonId != -1){
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

	/* (non-Javadoc)
	 * @see android.media.MediaPlayer.OnErrorListener#onError(android.media.MediaPlayer, int, int)
	 */
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.w("LT", "Media Player has gone to error mode. What: "+what+", Extra: "+extra+". Releasing MP.");
		stop(false);
		return true;
	}

	/* (non-Javadoc)
	 * @see android.media.MediaPlayer.OnCompletionListener#onCompletion(android.media.MediaPlayer)
	 */
	public void onCompletion(MediaPlayer mp) {
		Log.d("LT", "onCompletion");
		playNextOrStop(false);
	}
	
	private void playNextOrStop(boolean force){
		PlayMode pm = getPlayMode();
		if(force){
			if(pm==PlayMode.REPEAT_TRACK){
				ListTypes lt = getListType();
				if((lt==ListTypes.LIST_TYPE_ALL_NO_TRANSLATE)||(lt==ListTypes.LIST_TYPE_ALL_TRANSLATE)){
					pm=PlayMode.REPEAT_ALL_LESSONS;
				}
				else{
					pm=PlayMode.REPEAT_ALL_STARRED;
				}
			}
		}
		switch(pm){
/*		case SINGLE_TRACK:
			Log.d("LT", "Playing single song finished. Stopping.");
			stop();
			break;*/
		case REPEAT_TRACK:
			play(currentLesson, currentTrack, false, this);
			break;
		case ALL_LESSONS:
		case REPEAT_ALL_LESSONS:
		case REPEAT_LESSON:
//		case SINGLE_LESSON:
		case REPEAT_ALL_STARRED:
			boolean endOfLessonReached = false;
			try{
				currentLesson.getPathByTrackNo(currentTrack+1);
				play(currentLesson, currentTrack+1, false, this);
			}
			catch(IllegalArgumentException e){
				endOfLessonReached = true;
			}
			if(endOfLessonReached){
				switch(getPlayMode()){
/*				case SINGLE_LESSON:
					stop();
					break;*/
				case REPEAT_LESSON:
					play(currentLesson, 0, false, this);
					break;
				case ALL_LESSONS:
				case REPEAT_ALL_LESSONS:
				{
					//find next lesson
					AssimilDatabase ad = null;
					switch(getListType()){
					case LIST_TYPE_ALL_NO_TRANSLATE:
					case LIST_TYPE_ALL_TRANSLATE:
						ad = AssimilDatabase.getDatabase(null);
						break;
					case LIST_TYPE_STARRED_NO_TRANSLATE:
					case LIST_TYPE_STARRED_TRANSLATE:
						ad = AssimilDatabase.getStarredOnly(null);
						break;
					}
					int lessonIdx = ad.indexOf(currentLesson.getHeader());
					if(lessonIdx<0){
						Log.w("LT", "Current lesson not found (@ LessonPlayer.playNextOrStop_1). WTF? Stop playing.");
						stop(false);
					}
					else if (lessonIdx+1 < ad.size()){
						AssimilLesson lesson =
								AssimilDatabase.getLesson(ad.get(lessonIdx+1).getId(), this);
						play(lesson,0, false, this);
					}
					else{
						//last lesson reached
						if(getPlayMode() == PlayMode.REPEAT_ALL_LESSONS){
							//start again at first lesson again
							AssimilLesson lesson =
									AssimilDatabase.getLesson(ad.get(0).getId(), this);
							play(lesson,0, false, this);
						}
						else{
							stop(false);
						}
					}
					break;
				}
				case REPEAT_ALL_STARRED:
				{
					//find next lesson
					AssimilDatabase db = AssimilDatabase.getDatabase(null);
					int lessonIdx = db.indexOf(currentLesson.getHeader());
					if(lessonIdx<0){
						Log.w("LT", "Current lesson not found (@ LessonPlayer.playNextOrStop_2). WTF? Stop playing.");
						stop(false);
					}
					else{
						AssimilLesson nextLesson = currentLesson;
						do{
							lessonIdx++;
							if(lessonIdx >= db.size()){
								lessonIdx=0;
							}
							nextLesson = AssimilDatabase.getLesson(db.get(lessonIdx).getId(), this);
						}
						while((!nextLesson.isStarred()) && (!nextLesson.equals(currentLesson)));
						if(nextLesson.isStarred()){
							play(nextLesson, 0, false, this);
						}
						else{
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

	//FIXME: Nowhere the ListType is set!
	/**
	 * @return
	 */
	public static ListTypes getListType() {
		return lt;
	}

	private void playNextLesson(){
		switch(getListType()){
//		case REPEAT_ALL_STARRED:
		case LIST_TYPE_STARRED_NO_TRANSLATE:
		case LIST_TYPE_STARRED_TRANSLATE:
		{
			//find next lesson
			AssimilDatabase db = AssimilDatabase.getDatabase(null);
			int lessonIdx = db.indexOf(currentLesson.getHeader());
			if(lessonIdx<0){
				Log.w("LT", "Current lesson not found (@ LessonPlayer.playNextLesson_1). WTF? Stop playing.");
				stop(false);
			}
			else{
				AssimilLessonHeader nextLessonHeader = currentLesson.getHeader();
				do{
					lessonIdx++;
					if(lessonIdx >= db.size()){
						lessonIdx=0;
					}
					nextLessonHeader = db.get(lessonIdx);
				}
				while((!nextLessonHeader.isStarred()) &&
						(!nextLessonHeader.equals(currentLesson.getHeader())));
				AssimilLesson lesson;
				if(nextLessonHeader.equals(currentLesson.getHeader())){
					lesson = currentLesson;
				}
				else{
					lesson = AssimilDatabase.getLesson(nextLessonHeader.getId(), this);
				}
				play(lesson, 0, false, this);
			}
			break;
		}
//		case REPEAT_LESSON:
//		case ALL_LESSONS:
//		case REPEAT_ALL_LESSONS:
//		case REPEAT_TRACK:
		default:
		{
			//find next lesson
			int lessonIdx = AssimilDatabase.getDatabase(null).indexOf(currentLesson.getHeader());
			if(lessonIdx<0){
				Log.w("LT", "Current lesson not found (@ LessonPlayer.playNextLesson_2). WTF? Stop playing.");
				stop(false);
			}
			else if (lessonIdx+1 < AssimilDatabase.getDatabase(null).size()){

				play(AssimilDatabase.getLesson(AssimilDatabase.getDatabase(null).get(lessonIdx+1).getId(), this), 0, false, this);
			}
			else{
				//last lesson reached
				//start again at first lesson again
				play(AssimilDatabase.getLesson(AssimilDatabase.getDatabase(null).get(0).getId(), this), 0, false, this);
			}
			break;
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
		if(doCont){
			doCont=false;
			contPos -= 100;
			if(contPos<0){
				contPos = 0;
			}
			Log.d("LT", "Resuming at position " + contPos);
			mediaPlayer.seekTo(contPos);
		}
		mediaPlayer.start();
		playing = true;
		Intent currTrackIntent = new Intent(LessonPlayer.PLAY_UPDATE_INTENT);
		currTrackIntent.putExtra(AssimilOnClickListener.EXTRA_LESSON_ID, currentLesson.getHeader().getId());
		currTrackIntent.putExtra(AssimilOnClickListener.EXTRA_TRACK_INDEX, getTrackNumber());
		currTrackIntent.putExtra(EXTRA_IS_PLAYING, playing);
		LocalBroadcastManager.getInstance(this).sendBroadcast(currTrackIntent);
/*		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
		notifyBuilder
		.setContentText("Playing: " + PlaybarManager.getLessonText() + " " + PlaybarManager.getTrackNumberText())
	    .setContentIntent(pi);*/
		//FIXME: This must open the right Fragment once ShowLesson has been converted to Fragment.
		Intent resultIntent = new Intent(this, MainActivity.class);//(this, ShowLesson.class);
		resultIntent.putExtra(AssimilOnClickListener.EXTRA_LESSON_ID, currentLesson.getHeader().getId());

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack
		//FIXME:How is this done with Fragments!?
		stackBuilder.addParentStack(MainActivity.class);//(ShowLesson.class);
		// Adds the Intent to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		// Gets a PendingIntent containing the entire back stack
		PendingIntent pi = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		notifyBuilder
		.setContentText("Playing: " + getLessonTitle() + " " + getTrackNumberText())
	    .setContentIntent(pi);

		startForeground(NOTIFICATION_ID, notifyBuilder.getNotification());
	}

	/* (non-Javadoc)
	 * @see android.media.AudioManager.OnAudioFocusChangeListener#onAudioFocusChange(int)
	 */
	public void onAudioFocusChange(int focusChange) {
		switch (focusChange) {
		case AudioManager.AUDIOFOCUS_GAIN:
			// resume playback
			play(currentLesson,currentTrack,true, this);
			break;

		case AudioManager.AUDIOFOCUS_LOSS:
			Log.d("LT","received AUDIOFOCUS_LOSS");
			stopPlaying(this);
			break;
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			Log.d("LT","received AUDIOFOCUS_LOSS_TRANSIENT");
			stopPlaying(this);
			break;
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
			// Lost focus for an unbounded amount of time: stop playback and release media player
			Log.d("LT","received AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
			stopPlaying(this);
			break;
		}
	}

	/**
	 * @return The number (index) of the track that is currently being played.
	 * 
	 */
	public static int getTrackNumber() {
		return currentTrack;
	}

	/**
	 * @return The lesson that is currently being played.
	 */
	public static AssimilLesson getLesson() {
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
		//FIXME: Update PlaybarFragment: update(); 
	}

	public static void increasePlayMode() {
		switch (playMode){
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
			if((lt == ListTypes.LIST_TYPE_ALL_NO_TRANSLATE)||(lt == ListTypes.LIST_TYPE_ALL_TRANSLATE)){
				playMode = PlayMode.REPEAT_ALL_LESSONS;
			}
			else{
				playMode = PlayMode.REPEAT_ALL_STARRED;
			}
			break;
		case REPEAT_ALL_LESSONS:
			playMode = PlayMode.ALL_LESSONS;
			break;
		case REPEAT_ALL_STARRED:
//			playMode = PlayMode.SINGLE_TRACK;
			playMode = PlayMode.ALL_LESSONS;
			break;
		default:
//			playMode = PlayMode.SINGLE_TRACK;
			playMode = PlayMode.REPEAT_LESSON;
			break;
		}
	}

	/**
	 * @return the lesson
	 */
	public static String getLessonTitle() {
		String rv = "...";
		AssimilLesson lesson = getLesson();
		if(lesson!=null){
			rv = lesson.getNumber();
		}
		return rv;
	}

	/**
	 * @return the number
	 */
	public static String getTrackNumberText() {
		String rv = "...";
		int trackNumber = getTrackNumber();
		AssimilLesson lesson = getLesson();
		if((trackNumber>=0)&&(lesson!=null)){
			rv = lesson.getTextNumber(trackNumber);
		}
		return rv;
	}


	/**
	 * @return
	 */
	public static boolean isPlayingTranslate() {
		return (lt == ListTypes.LIST_TYPE_STARRED_TRANSLATE)||(lt == ListTypes.LIST_TYPE_ALL_TRANSLATE);
	}

	/**
	 * @param lt
	 */
	public static void setListType(ListTypes lt){
		LessonPlayer.lt = lt;
	//FIXME: Do we need this?
//	if((lt == ListTypes.LIST_TYPE_ALL_NO_TRANSLATE)||(lt == ListTypes.LIST_TYPE_STARRED_NO_TRANSLATE)){
//		playTranslate(false);
//	}
//	else{
//		playTranslate(true);
//	}
//	checkPlaymode();
	}

}
