/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import com.github.federvieh.selma.assimillib.LessonPlayer.PlayMode;

/**
 * @author frank
 *
 */
public class PlaybarManager {
	private static PlayMode playMode = PlayMode.REPEAT_ALL_STARRED;

	/**
	 * @return the playMode
	 */
	public static PlayMode getPlayMode() {
		return playMode;
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
		playmodeUpdated = true;
		update();
	}

	/**
	 * 
	 */
	private static void update() {
		if(pbInstance!=null){
			pbInstance.update();
		}
		if(showLesson!=null){
			showLesson.highlight(trackNumber);
		}
	}

	private static AssimilLesson lesson = null;
	/**
	 * @return the lesson
	 */
	public static String getLessonText() {
		String rv = "...";
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
		if(trackNumber>=0){
//			rv = ""+trackNumber;
			rv = lesson.getTextNumber(trackNumber);
		}
		return rv;
	}

	private static int trackNumber = -1;
	private static Playbar pbInstance = null;
	private static boolean playmodeUpdated = false;
	private static boolean playing = false;
	private static boolean playTranlate;
	private static ListTypes lt = ListTypes.LIST_TYPE_ALL_TRANSLATE;
	private static ShowLesson showLesson;
	
	/**
	 * @param pbInstance the pbInstance to set
	 */
	public static void setPbInstance(Playbar pbInstance) {
		PlaybarManager.pbInstance = pbInstance;
		if(pbInstance!=null){
			pbInstance.update();
		}
	}

	public static void  setCurrent(AssimilLesson currentLesson, int currentTrack){
		lesson = currentLesson;
		trackNumber = currentTrack;
		update();
	}
	
	private PlaybarManager(){
		
	}

	/**
	 * @return
	 */
	public static boolean isPlaymodeUpdated() {
		if(playmodeUpdated ){
			playmodeUpdated = false;
			return true;
		}
		return false;
	}

	/**
	 * @return
	 */
	public static void setPlaying(boolean playingParam) {
		playing = playingParam;
		update();
	}

	/**
	 * @return
	 */
	public static boolean isPlaying() {
		return playing ;
	}

	/**
	 * @return
	 */
	public static AssimilLesson getLessonInstance() {
		return lesson;
	}

	/**
	 * @return
	 */
	public static int getTrackNumber() {
		return trackNumber;
	}

	public static void setListType(ListTypes lt){
		PlaybarManager.lt=lt;
		if((lt == ListTypes.LIST_TYPE_ALL_NO_TRANSLATE)||(lt == ListTypes.LIST_TYPE_STARRED_NO_TRANSLATE)){
			playTranslate(false);
		}
		else{
			playTranslate(true);
		}
		checkPlaymode();
	}
	
	/**
	 * @return the ListType
	 */
	public static ListTypes getListType() {
		return lt;
	}

	/**
	 * 
	 */
	private static void checkPlaymode() {
		if((lt==ListTypes.LIST_TYPE_ALL_NO_TRANSLATE)||(lt==ListTypes.LIST_TYPE_ALL_TRANSLATE)){
			if(playMode==PlayMode.REPEAT_ALL_STARRED){
				playMode = PlayMode.REPEAT_ALL_LESSONS;
			}
		}
		else{
			if(playMode==PlayMode.REPEAT_ALL_LESSONS){
				playMode = PlayMode.REPEAT_ALL_STARRED;
			}
		}
	}

	private static void playTranslate(boolean mode){
		playTranlate = mode;
		update();
	}

	public static boolean isPlayingTranslate(){
		return playTranlate;
	}

	/**
	 * @param showLesson
	 */
	public static void setLessonInstance(ShowLesson showLesson) {
		PlaybarManager.showLesson = showLesson;
		update();
	}
}
